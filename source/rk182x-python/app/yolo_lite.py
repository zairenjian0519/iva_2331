"""RKNNLite YOLOv5 adapter with RGA-backed full-frame and ROI inputs."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Iterable

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


class RknnLiteYoloV5:
    """A model-specific RKNNLite context. It is deliberately single-threaded."""

    def __init__(self, spec: ModelSpec) -> None:
        from rknnlite.api import RKNNLite

        self.spec = spec
        self.labels = list(spec.class_names) or [
            line.strip()
            for line in spec.labels_path.read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
        if not self.labels:
            raise ValueError(f"{spec.role} has no class names")
        self._runtime = RKNNLite()
        if self._runtime.load_rknn(str(spec.model_path)) != 0:
            raise RuntimeError(f"cannot load RKNN model for {spec.role}: {spec.model_path}")
        if self._runtime.init_runtime(core_mask=RKNNLite.NPU_CORE_0_1_2) != 0:
            self._runtime.release()
            raise RuntimeError(f"cannot initialize RKNNLite for {spec.role}")
        self._output_attrs = [self._runtime.rknn_runtime.get_tensor_attr(index, is_output=True) for index in range(3)]
        self._validate_input()

    def close(self) -> None:
        if self._runtime is not None:
            self._runtime.release()
            self._runtime = None

    def __enter__(self) -> "RknnLiteYoloV5":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()

    def infer_frame(self, frame: Any, confidence: float | None = None) -> list[Detection]:
        image = frame.rgb
        transform = Transform(0, 0, frame.width, frame.height,
                              frame.letterbox.scale, frame.letterbox.pad_left, frame.letterbox.pad_top)
        return self._infer_rgb(image, transform, confidence)

    def infer_roi(self, frame: Any, roi: dict[str, int], parent: dict[str, int],
                  confidence: float | None = None) -> list[Detection]:
        image = frame.rga_rgb(self.spec.width, self.spec.height, roi["x"], roi["y"],
                              roi["width"], roi["height"], True)
        transform = Transform(roi["x"], roi["y"], roi["width"], roi["height"],
                              image.letterbox.scale, image.letterbox.pad_left, image.letterbox.pad_top)
        return [Detection(**{**d.__dict__, "roi_bbox": roi, "parent_bbox": parent})
                for d in self._infer_rgb(image.rgb, transform, confidence)]

    def _validate_input(self) -> None:
        attr = self._runtime.rknn_runtime.get_tensor_attr(0, is_output=False)
        count = int(getattr(attr, "n_dims", 0))
        dims = tuple(int(value) for value in getattr(attr, "dims", ())[:count])
        expected = (1, self.spec.height, self.spec.width, 3)
        if dims and dims != expected:
            self.close()
            raise ValueError(f"{self.spec.role} expects {dims}, manifest declares {expected}")

    def _infer_rgb(self, image: np.ndarray, transform: Transform,
                   confidence: float | None = None) -> list[Detection]:
        if self._runtime is None:
            raise RuntimeError("RKNNLite context is closed")
        if image.dtype != np.uint8 or image.shape != (self.spec.height, self.spec.width, 3):
            raise ValueError(f"{self.spec.role} requires NHWC uint8 {self.spec.width}x{self.spec.height}")
        outputs = self._runtime.inference(inputs=[image[np.newaxis, ...]])
        if outputs is None:
            raise RuntimeError(f"{self.spec.role} produced no outputs")
        threshold = self.spec.confidence if confidence is None else float(confidence)
        if not 0.0 <= threshold <= 1.0:
            raise ValueError(f"{self.spec.role} confidence must be in [0, 1]")
        candidates = self._decode(outputs, threshold)
        return self._restore(self._nms(candidates), transform)

    def _decode(self, outputs: Iterable[np.ndarray], confidence: float) -> list[tuple[int, float, np.ndarray]]:
        values = list(outputs)
        if len(values) != len(self.spec.anchors):
            raise ValueError(f"{self.spec.role} output count {len(values)} does not match manifest")
        candidates: list[tuple[int, float, np.ndarray]] = []
        for index, (raw, anchors, stride) in enumerate(zip(values, self.spec.anchors, self.spec.strides)):
            tensor = self._to_hwc(np.asarray(raw), len(anchors))
            height, width, _, attributes = tensor.shape
            if attributes != len(self.labels) + 5:
                raise ValueError(f"{self.spec.role} head {index} attributes {attributes} != labels + 5")
            y, x = np.meshgrid(np.arange(height), np.arange(width), indexing="ij")
            grid = np.stack((x, y), axis=-1)[..., None, :].astype(np.float32)
            anchor_array = np.asarray(anchors, dtype=np.float32).reshape(1, 1, len(anchors), 2)
            xy = (tensor[..., :2] * 2.0 - 0.5 + grid) * stride
            wh = (tensor[..., 2:4] * 2.0) ** 2 * anchor_array
            objectness = tensor[..., 4]
            classes = tensor[..., 5:]
            ids = np.argmax(classes, axis=-1)
            scores = objectness * np.max(classes, axis=-1)
            for row, column, anchor in zip(*np.where(scores >= confidence)):
                box = np.concatenate((xy[row, column, anchor] - wh[row, column, anchor] / 2.0,
                                      xy[row, column, anchor] + wh[row, column, anchor] / 2.0))
                candidates.append((int(ids[row, column, anchor]), float(scores[row, column, anchor]), box))
        return candidates

    def _to_hwc(self, raw: np.ndarray, anchor_count: int) -> np.ndarray:
        tensor = np.squeeze(raw, axis=0) if raw.ndim == 4 and raw.shape[0] == 1 else raw
        if tensor.ndim != 3:
            raise ValueError(f"unexpected YOLO head shape {raw.shape}")
        channels = anchor_count * (len(self.labels) + 5)
        if tensor.shape[0] == channels:
            tensor = tensor.transpose(1, 2, 0)
        elif tensor.shape[-1] != channels:
            raise ValueError(f"cannot identify output layout {raw.shape}")
        return tensor.reshape(tensor.shape[0], tensor.shape[1], anchor_count, len(self.labels) + 5).astype(np.float32, copy=False)

    def _nms(self, candidates: list[tuple[int, float, np.ndarray]]) -> list[tuple[int, float, np.ndarray]]:
        kept: list[tuple[int, float, np.ndarray]] = []
        for class_id in sorted({item[0] for item in candidates}):
            remaining = sorted((item for item in candidates if item[0] == class_id), key=lambda item: item[1], reverse=True)
            while remaining:
                current = remaining.pop(0)
                kept.append(current)
                remaining = [item for item in remaining if self._iou(current[2], item[2]) <= self.spec.nms_iou]
        return sorted(kept, key=lambda item: item[1], reverse=True)

    @staticmethod
    def _iou(first: np.ndarray, second: np.ndarray) -> float:
        left = max(float(first[0]), float(second[0])); top = max(float(first[1]), float(second[1]))
        right = min(float(first[2]), float(second[2])); bottom = min(float(first[3]), float(second[3]))
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
            name = self.labels[class_id] if class_id < len(self.labels) else f"class_{class_id}"
            detections.append(Detection(class_id, name, round(score, 4),
                                        {"x": x1, "y": y1, "width": x2 - x1, "height": y2 - y1}))
        return detections
