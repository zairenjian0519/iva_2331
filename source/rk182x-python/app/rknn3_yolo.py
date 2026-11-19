"""YOLOv5 raw-head inference through RKNN3 Lite.

The RK182X model is the ordinary three-head YOLOv5 export. Decode and NMS
remain in Python so thresholds are not embedded in the RKNN3 model.
"""

from __future__ import annotations

from dataclasses import dataclass
import math
import threading
import time
from typing import Any, Iterable, Mapping

import numpy as np

from app.model_specs import ModelSpec


@dataclass(frozen=True)
class Detection:
    class_id: int
    class_name: str
    score: float
    bbox: dict[str, int]
    roi_bbox: dict[str, int] | None = None
    parent_bbox: dict[str, int] | None = None


@dataclass(frozen=True)
class Transform:
    source_x: int
    source_y: int
    source_width: int
    source_height: int
    scale: float
    pad_left: int
    pad_top: int


class RoiPreprocessError(RuntimeError):
    def __init__(
        self,
        role: str,
        requested_roi: Mapping[str, int],
        actual_roi: Mapping[str, int] | None,
        reason: str,
    ) -> None:
        self.role = str(role)
        self.requested_roi = dict(requested_roi)
        self.actual_roi = dict(actual_roi) if actual_roi is not None else None
        super().__init__(
            f"{self.role} ROI preprocess failed: requested={self.requested_roi} "
            f"actual={self.actual_roi} reason={reason}"
        )


RGA3_MIN_INPUT_WIDTH = 68
RGA3_MAX_UPSCALE = 8.0


def _ceil_even(value: float) -> int:
    return max(2, int(math.ceil(float(value) / 2.0)) * 2)


def _fit_even_interval(center: float, size: int, limit: int) -> tuple[int, int]:
    even_limit = int(limit) & ~1
    if even_limit < 2:
        raise ValueError(f"frame dimension {limit} is too small for NV12/NV21")
    actual_size = min(_ceil_even(size), even_limit)
    start = int(math.floor(center - actual_size / 2.0)) & ~1
    start = max(0, min(start, even_limit - actual_size))
    return start, actual_size


def normalize_rga_roi(
    roi: Mapping[str, int],
    frame_width: int,
    frame_height: int,
    output_width: int,
    output_height: int,
    letterbox: bool = True,
) -> dict[str, int]:
    if frame_width <= 0 or frame_height <= 0 or output_width <= 0 or output_height <= 0:
        raise ValueError("frame and output dimensions must be positive")
    x = int(roi.get("x", 0))
    y = int(roi.get("y", 0))
    width = int(roi.get("width", 0))
    height = int(roi.get("height", 0))
    if width <= 0 or height <= 0:
        raise ValueError(f"ROI dimensions must be positive: {dict(roi)}")
    frame_even_width = int(frame_width) & ~1
    frame_even_height = int(frame_height) & ~1
    raw_left = max(0, x)
    raw_top = max(0, y)
    raw_right = min(frame_even_width, x + width)
    raw_bottom = min(frame_even_height, y + height)
    if raw_right <= raw_left or raw_bottom <= raw_top:
        raise ValueError(f"ROI does not overlap the frame: {dict(roi)}")
    left = raw_left & ~1
    top = raw_top & ~1
    right = min(frame_even_width, _ceil_even(raw_right))
    bottom = min(frame_even_height, _ceil_even(raw_bottom))
    base_width = right - left
    base_height = bottom - top
    if letterbox:
        requested_scale = min(output_width / base_width, output_height / base_height)
        expansion = max(1.0, requested_scale / RGA3_MAX_UPSCALE)
        desired_width = _ceil_even(base_width * expansion)
        desired_height = _ceil_even(base_height * expansion)
    else:
        desired_width = max(base_width, _ceil_even(output_width / RGA3_MAX_UPSCALE))
        desired_height = max(base_height, _ceil_even(output_height / RGA3_MAX_UPSCALE))
    desired_width = max(desired_width, RGA3_MIN_INPUT_WIDTH)
    center_x = (left + right) / 2.0
    center_y = (top + bottom) / 2.0
    actual_x, actual_width = _fit_even_interval(center_x, desired_width, frame_even_width)
    actual_y, actual_height = _fit_even_interval(center_y, desired_height, frame_even_height)
    actual_scale = (
        min(output_width / actual_width, output_height / actual_height)
        if letterbox
        else max(output_width / actual_width, output_height / actual_height)
    )
    if actual_width < RGA3_MIN_INPUT_WIDTH or actual_scale > RGA3_MAX_UPSCALE + 1e-6:
        raise ValueError(
            f"frame cannot satisfy RGA3 limits: actual={actual_width}x{actual_height} "
            f"scale={actual_scale:.4f}"
        )
    return {"x": actual_x, "y": actual_y, "width": actual_width, "height": actual_height}


class Rknn3YoloV5:
    """Run one RKNN3 Lite model and decode its three raw YOLOv5 heads."""

    def __init__(self, spec: ModelSpec, *, device_id: str | None = None, target: str = "rk1820") -> None:
        from rknn3lite.api import RKNN3Lite

        self.spec = spec
        self.labels = list(spec.class_names)
        if not self.labels:
            self.labels = [
                line.strip()
                for line in spec.labels_path.read_text(encoding="utf-8").splitlines()
                if line.strip()
            ]
        if not self.labels:
            raise ValueError(f"{spec.role} has no class names")

        self._runtime = RKNN3Lite(verbose=False)
        if self._runtime.load_rknn(str(spec.model_path), str(spec.weight_path)) != 0:
            raise RuntimeError(f"cannot load RKNN3 model for {spec.role}: {spec.model_path}")
        if self._runtime.init_runtime(
            target=target,
            core_mask=spec.core_mask,
            device_id=device_id.encode("ascii") if device_id is not None else None,
        ) != 0:
            self._runtime.release()
            raise RuntimeError(f"cannot initialize RKNN3 runtime for {spec.role}")
        self._output_attrs = list(self._runtime.get_outputs_tensor_attr() or [])
        self._runtime_lock = threading.Lock()

    def close(self) -> None:
        with self._runtime_lock:
            if self._runtime is not None:
                runtime = self._runtime
                self._runtime = None
                runtime.release()

    def infer_frame(
        self,
        frame: Any,
        confidence: float | None = None,
        phase_timings: dict[str, float] | None = None,
    ) -> list[Detection]:
        image = frame.rgb
        letterbox = frame.letterbox
        if image.shape != (self.spec.height, self.spec.width, 3):
            prepared = frame.rga_rgb(
                self.spec.width, self.spec.height,
                0, 0, frame.width, frame.height, True,
            )
            image = prepared.rgb
            letterbox = prepared.letterbox
        transform = Transform(
            0,
            0,
            frame.width,
            frame.height,
            letterbox.scale,
            letterbox.pad_left,
            letterbox.pad_top,
        )
        return self._infer_rgb(image, transform, confidence, phase_timings)

    def infer_roi(
        self,
        frame: Any,
        roi: dict[str, int],
        parent: dict[str, int],
        confidence: float | None = None,
        phase_timings: dict[str, float] | None = None,
    ) -> list[Detection]:
        requested_roi = dict(roi)
        actual_roi: dict[str, int] | None = None
        try:
            actual_roi = normalize_rga_roi(
                requested_roi, frame.width, frame.height,
                self.spec.width, self.spec.height,
            )
            image = frame.rga_rgb(
                self.spec.width, self.spec.height,
                actual_roi["x"], actual_roi["y"],
                actual_roi["width"], actual_roi["height"], True,
            )
        except (RuntimeError, ValueError) as exc:
            raise RoiPreprocessError(
                self.spec.role, requested_roi, actual_roi, str(exc)
            ) from exc
        transform = Transform(
            actual_roi["x"], actual_roi["y"],
            actual_roi["width"], actual_roi["height"],
            image.letterbox.scale,
            image.letterbox.pad_left,
            image.letterbox.pad_top,
        )
        detections = self._infer_rgb(
            image.rgb, transform, confidence, phase_timings
        )
        return [
            Detection(**{**item.__dict__, "roi_bbox": actual_roi, "parent_bbox": parent})
            for item in detections
        ]

    def _infer_rgb(
        self,
        image: np.ndarray,
        transform: Transform,
        confidence: float | None = None,
        phase_timings: dict[str, float] | None = None,
    ) -> list[Detection]:
        expected = (self.spec.height, self.spec.width, 3)
        if image.dtype != np.uint8 or image.shape != expected:
            raise ValueError(f"{self.spec.role} requires NHWC uint8 {self.spec.width}x{self.spec.height}")

        # RKNN3 Lite 1.0 expects the NHWC input without an explicit data_format
        # conversion. Passing data_format=['nhwc'] changes the input layout on
        # some 1.0 runtimes.
        lock_wait_started = time.perf_counter()
        with self._runtime_lock:
            lock_acquired = time.perf_counter()
            if self._runtime is None:
                raise RuntimeError("RKNN3 runtime is closed")
            npu_started = time.perf_counter()
            runtime_outputs = self._runtime.inference(inputs=[image[np.newaxis, ...]])
            if runtime_outputs is None:
                raise RuntimeError(f"{self.spec.role} produced no RKNN3 outputs")
            # RKNN3 may reuse runtime-owned output buffers on the next call.
            # Copy while holding the per-model lock, then decode outside it.
            outputs = [np.array(output, copy=True) for output in runtime_outputs]
            npu_finished = time.perf_counter()
        if phase_timings is not None:
            phase_timings["runtime_lock_wait_ms"] = (
                lock_acquired - lock_wait_started
            ) * 1000.0
            phase_timings["npu_execute_ms"] = (
                npu_finished - npu_started
            ) * 1000.0
        postprocess_started = time.perf_counter()
        threshold = self.spec.confidence if confidence is None else float(confidence)
        if not 0.0 <= threshold <= 1.0:
            raise ValueError(f"{self.spec.role} confidence must be in [0, 1]")
        candidates = self._decode(outputs, threshold)
        result = self._restore(self._nms(candidates), transform)
        if phase_timings is not None:
            phase_timings["postprocess_ms"] = (
                time.perf_counter() - postprocess_started
            ) * 1000.0
        return result

    def _decode(
        self,
        outputs: Iterable[np.ndarray],
        confidence: float,
    ) -> list[tuple[int, float, np.ndarray]]:
        values = list(outputs)
        if len(values) != len(self.spec.anchors):
            raise ValueError(f"{self.spec.role} output count {len(values)} does not match manifest")

        candidates: list[tuple[int, float, np.ndarray]] = []
        for index, (raw, anchors, stride) in enumerate(
            zip(values, self.spec.anchors, self.spec.strides)
        ):
            tensor = self._to_hwc(self._dequantize(np.asarray(raw), index), len(anchors))
            height, width, _, attributes = tensor.shape
            if attributes != len(self.labels) + 5:
                raise ValueError(
                    f"{self.spec.role} head {index} attributes {attributes} != labels + 5"
                )
            grid_y, grid_x = np.meshgrid(
                np.arange(height), np.arange(width), indexing="ij"
            )
            grid = np.stack((grid_x, grid_y), axis=-1)[..., None, :].astype(np.float32)
            anchor_array = np.asarray(anchors, dtype=np.float32).reshape(1, 1, len(anchors), 2)
            xy = (tensor[..., :2] * 2.0 - 0.5 + grid) * stride
            wh = (tensor[..., 2:4] * 2.0) ** 2 * anchor_array
            objectness = tensor[..., 4]
            classes = tensor[..., 5:]
            class_ids = np.argmax(classes, axis=-1)
            scores = objectness * np.max(classes, axis=-1)
            for row, column, anchor in zip(*np.where(scores >= confidence)):
                box = np.concatenate(
                    (
                        xy[row, column, anchor] - wh[row, column, anchor] / 2.0,
                        xy[row, column, anchor] + wh[row, column, anchor] / 2.0,
                    )
                )
                candidates.append(
                    (int(class_ids[row, column, anchor]), float(scores[row, column, anchor]), box)
                )
        return candidates

    def _dequantize(self, output: np.ndarray, index: int) -> np.ndarray:
        if np.issubdtype(output.dtype, np.floating):
            return output.astype(np.float32, copy=False)
        if index >= len(self._output_attrs):
            raise RuntimeError(f"missing RKNN3 output attributes for head {index}")
        attr = self._output_attrs[index]
        qnt = getattr(attr, "qnt_info", None)
        scale = getattr(attr, "scale", None)
        zero_point = getattr(attr, "zp", None)
        if qnt is not None:
            scale = scale if scale is not None else getattr(qnt, "scale", None)
            zero_point = zero_point if zero_point is not None else getattr(qnt, "zero_point", None)
        if scale is None or zero_point is None:
            raise RuntimeError(f"missing quantization metadata for RKNN3 output {index}")
        return (output.astype(np.float32) - float(zero_point)) * float(scale)

    def _to_hwc(self, raw: np.ndarray, anchor_count: int) -> np.ndarray:
        tensor = np.squeeze(raw, axis=0) if raw.ndim == 4 and raw.shape[0] == 1 else raw
        if tensor.ndim != 3:
            raise ValueError(f"unexpected YOLOv5 head shape {raw.shape}")
        channels = anchor_count * (len(self.labels) + 5)
        if tensor.shape[0] == channels:
            tensor = tensor.transpose(1, 2, 0)
        elif tensor.shape[-1] != channels:
            raise ValueError(f"cannot identify RKNN3 output layout {raw.shape}")
        return tensor.reshape(tensor.shape[0], tensor.shape[1], anchor_count, len(self.labels) + 5)

    def _nms(self, candidates: list[tuple[int, float, np.ndarray]]) -> list[tuple[int, float, np.ndarray]]:
        kept: list[tuple[int, float, np.ndarray]] = []
        for class_id in sorted({item[0] for item in candidates}):
            remaining = sorted(
                (item for item in candidates if item[0] == class_id),
                key=lambda item: item[1],
                reverse=True,
            )
            while remaining:
                current = remaining.pop(0)
                kept.append(current)
                remaining = [
                    item for item in remaining
                    if self._iou(current[2], item[2]) <= self.spec.nms_iou
                ]
        return sorted(kept, key=lambda item: item[1], reverse=True)

    @staticmethod
    def _iou(first: np.ndarray, second: np.ndarray) -> float:
        left = max(float(first[0]), float(second[0]))
        top = max(float(first[1]), float(second[1]))
        right = min(float(first[2]), float(second[2]))
        bottom = min(float(first[3]), float(second[3]))
        intersection = max(0.0, right - left) * max(0.0, bottom - top)
        area_a = max(0.0, float(first[2] - first[0])) * max(0.0, float(first[3] - first[1]))
        area_b = max(0.0, float(second[2] - second[0])) * max(0.0, float(second[3] - second[1]))
        return intersection / max(area_a + area_b - intersection, 1e-6)

    def _restore(self, candidates: list[tuple[int, float, np.ndarray]], transform: Transform) -> list[Detection]:
        detections: list[Detection] = []
        for class_id, score, box in candidates:
            x1 = int(np.clip((box[0] - transform.pad_left) / transform.scale + transform.source_x, transform.source_x, transform.source_x + transform.source_width))
            y1 = int(np.clip((box[1] - transform.pad_top) / transform.scale + transform.source_y, transform.source_y, transform.source_y + transform.source_height))
            x2 = int(np.clip((box[2] - transform.pad_left) / transform.scale + transform.source_x, transform.source_x, transform.source_x + transform.source_width))
            y2 = int(np.clip((box[3] - transform.pad_top) / transform.scale + transform.source_y, transform.source_y, transform.source_y + transform.source_height))
            if x2 <= x1 or y2 <= y1:
                continue
            label = self.labels[class_id] if class_id < len(self.labels) else f"class_{class_id}"
            detections.append(
                Detection(
                    class_id,
                    label,
                    round(score, 4),
                    {"x": x1, "y": y1, "width": x2 - x1, "height": y2 - y1},
                )
            )
        return detections
