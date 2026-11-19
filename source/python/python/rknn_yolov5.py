"""RKNNLite YOLOv5 inference for RGB views produced by rk_mpp_pipeline."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable, Sequence

import numpy as np


_ANCHORS = np.asarray(
    (
        ((10, 13), (16, 30), (33, 23)),
        ((30, 61), (62, 45), (59, 119)),
        ((116, 90), (156, 198), (373, 326)),
    ),
    dtype=np.float32,
)
_STRIDES = (8, 16, 32)


@dataclass(frozen=True)
class Detection:
    class_id: int
    label: str
    confidence: float
    xyxy: tuple[float, float, float, float]
    camera_id: int
    pts_us: int


class YoloV5RknnLite:
    """Run the SDK's activated, three-head YOLOv5 RKNN export on a Frame."""

    def __init__(
        self,
        model_path: str | Path,
        labels_path: str | Path | None = None,
        confidence: float = 0.25,
        nms_iou: float = 0.45,
        output_quantization: Sequence[dict[str, float | int]] | None = None,
    ) -> None:
        from rknnlite.api import RKNNLite

        self.confidence = float(confidence)
        self.nms_iou = float(nms_iou)
        self._quantization = list(output_quantization or [])
        self.labels = self._load_labels(labels_path)
        self._rknn = RKNNLite()
        if self._rknn.load_rknn(str(model_path)) != 0:
            raise RuntimeError(f"unable to load RKNN model: {model_path}")
        result = self._rknn.init_runtime(core_mask=RKNNLite.NPU_CORE_0_1_2)
        if result != 0:
            self._rknn.release()
            raise RuntimeError(
                "RKNN runtime initialization failed. Ensure LD_LIBRARY_PATH selects the "
                "project's vendor/lib/librknnrt.so and the model matches the SDK version."
            )
        self.sdk_version = self._rknn.get_sdk_version()
        self._output_attrs = [self._rknn.rknn_runtime.get_tensor_attr(i, is_output=True) for i in range(3)]
        self._verify_input_contract()

    def close(self) -> None:
        if self._rknn is not None:
            self._rknn.release()
            self._rknn = None

    def __enter__(self) -> "YoloV5RknnLite":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()

    def infer(self, frame: Any) -> list[Detection]:
        if self._rknn is None:
            raise RuntimeError("runner has been closed")
        image = frame.rgb
        if image.dtype != np.uint8 or image.shape != (640, 640, 3) or not image.flags.c_contiguous:
            raise ValueError("Frame.rgb must be a contiguous uint8 RGB image with shape (640, 640, 3)")

        # Lite2 1.6 requires a rank-4 NHWC tensor. newaxis creates a zero-copy
        # NumPy view over the RGA-backed RGB array rather than a pixel copy.
        outputs = self._rknn.inference(inputs=[image[np.newaxis, ...]])
        if outputs is None:
            raise RuntimeError("RKNNLite inference returned no output tensors")
        boxes, scores, class_ids = self._decode(outputs)
        keep = self._nms(boxes, scores, class_ids)
        return [self._make_detection(frame, boxes[index], scores[index], class_ids[index]) for index in keep]

    def _verify_input_contract(self) -> None:
        attr = self._rknn.rknn_runtime.get_tensor_attr(0, is_output=False)
        raw_dims = tuple(int(value) for value in self._attr_value(attr, "dims", ()))
        n_dims = int(self._attr_value(attr, "n_dims", len(raw_dims)))
        dims = raw_dims[:n_dims]
        if dims and dims not in {(1, 640, 640, 3), (640, 640, 3)}:
            self.close()
            raise ValueError(f"model input must be NHWC 640x640 RGB uint8, got dimensions {dims}")

    def _decode(self, outputs: Sequence[np.ndarray]) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
        if len(outputs) != 3:
            raise ValueError(f"standard YOLOv5 expects three output tensors, got {len(outputs)}")
        all_boxes: list[np.ndarray] = []
        all_scores: list[np.ndarray] = []
        all_classes: list[np.ndarray] = []
        for index, (output, anchors, stride) in enumerate(zip(outputs, _ANCHORS, _STRIDES)):
            tensor = self._dequantize(np.asarray(output), index)
            tensor = self._normalise_head(tensor)
            grid_h, grid_w = tensor.shape[:2]
            grid_y, grid_x = np.meshgrid(np.arange(grid_h), np.arange(grid_w), indexing="ij")
            grid = np.stack((grid_x, grid_y), axis=-1)[..., None, :].astype(np.float32)
            xy = (tensor[..., 0:2] * 2.0 - 0.5 + grid) * stride
            wh = (tensor[..., 2:4] * 2.0) ** 2 * anchors.reshape(1, 1, 3, 2)
            objectness = tensor[..., 4]
            class_scores = tensor[..., 5:]
            class_ids = np.argmax(class_scores, axis=-1)
            scores = objectness * np.max(class_scores, axis=-1)
            selected = scores >= self.confidence
            if not np.any(selected):
                continue
            xy = xy[selected]
            wh = wh[selected]
            all_boxes.append(np.concatenate((xy - wh / 2.0, xy + wh / 2.0), axis=1))
            all_scores.append(scores[selected].astype(np.float32))
            all_classes.append(class_ids[selected].astype(np.int32))
        if not all_boxes:
            return (
                np.empty((0, 4), dtype=np.float32),
                np.empty((0,), dtype=np.float32),
                np.empty((0,), dtype=np.int32),
            )
        return np.concatenate(all_boxes), np.concatenate(all_scores), np.concatenate(all_classes)

    def _dequantize(self, output: np.ndarray, index: int) -> np.ndarray:
        if np.issubdtype(output.dtype, np.floating):
            return output.astype(np.float32, copy=False)
        attr = self._output_attrs[index]
        scale = self._attr_value(attr, "scale", None)
        zp = self._attr_value(attr, "zp", None)
        if (scale is None or zp is None) and index < len(self._quantization):
            scale = self._quantization[index].get("scale")
            zp = self._quantization[index].get("zp")
        if scale is None or zp is None:
            raise RuntimeError(
                "RKNNLite did not expose output quantization metadata. Supply "
                "output_quantization=[{'scale': ..., 'zp': ...}, ...] for this model."
            )
        return (output.astype(np.float32) - float(zp)) * float(scale)

    @staticmethod
    def _normalise_head(tensor: np.ndarray) -> np.ndarray:
        tensor = np.squeeze(tensor, axis=0) if tensor.ndim == 4 and tensor.shape[0] == 1 else tensor
        if tensor.ndim != 3:
            raise ValueError(f"YOLOv5 output must have three non-batch dimensions, got {tensor.shape}")
        if tensor.shape[0] % 3 == 0:
            channels, height, width = tensor.shape
            tensor = tensor.transpose(1, 2, 0)
        elif tensor.shape[-1] % 3 == 0:
            height, width, channels = tensor.shape
        else:
            raise ValueError(f"cannot identify YOLOv5 output layout: {tensor.shape}")
        attributes = channels // 3
        if attributes <= 5:
            raise ValueError(f"invalid YOLOv5 channel count: {channels}")
        return tensor.reshape(height, width, 3, attributes)

    def _make_detection(self, frame: Any, box: np.ndarray, score: float, class_id: int) -> Detection:
        letterbox = frame.letterbox
        x1 = np.clip((box[0] - letterbox.pad_left) / letterbox.scale, 0, frame.width)
        y1 = np.clip((box[1] - letterbox.pad_top) / letterbox.scale, 0, frame.height)
        x2 = np.clip((box[2] - letterbox.pad_left) / letterbox.scale, 0, frame.width)
        y2 = np.clip((box[3] - letterbox.pad_top) / letterbox.scale, 0, frame.height)
        label = self.labels[class_id] if class_id < len(self.labels) else f"class_{class_id}"
        return Detection(
            class_id=int(class_id),
            label=label,
            confidence=float(score),
            xyxy=(float(x1), float(y1), float(x2), float(y2)),
            camera_id=frame.camera_id,
            pts_us=frame.pts_us,
        )

    def _nms(self, boxes: np.ndarray, scores: np.ndarray, class_ids: np.ndarray) -> list[int]:
        kept: list[int] = []
        for class_id in np.unique(class_ids):
            indices = np.where(class_ids == class_id)[0]
            order = indices[np.argsort(scores[indices])[::-1]]
            while order.size:
                current = int(order[0])
                kept.append(current)
                if order.size == 1:
                    break
                remaining = order[1:]
                overlap = self._iou(boxes[current], boxes[remaining])
                order = remaining[overlap <= self.nms_iou]
        return sorted(kept, key=lambda index: float(scores[index]), reverse=True)

    @staticmethod
    def _iou(box: np.ndarray, others: np.ndarray) -> np.ndarray:
        left_top = np.maximum(box[:2], others[:, :2])
        right_bottom = np.minimum(box[2:], others[:, 2:])
        overlap = np.clip(right_bottom - left_top, 0, None)
        intersection = overlap[:, 0] * overlap[:, 1]
        area = max(0.0, (box[2] - box[0]) * (box[3] - box[1]))
        other_area = np.maximum(0.0, others[:, 2] - others[:, 0]) * np.maximum(0.0, others[:, 3] - others[:, 1])
        return intersection / np.maximum(area + other_area - intersection, 1e-6)

    @staticmethod
    def _attr_value(attr: Any, name: str, default: Any) -> Any:
        if isinstance(attr, dict):
            return attr.get(name, default)
        return getattr(attr, name, default)

    @staticmethod
    def _load_labels(labels_path: str | Path | None) -> list[str]:
        if labels_path is None:
            return []
        return [line.strip() for line in Path(labels_path).read_text(encoding="utf-8").splitlines() if line.strip()]
