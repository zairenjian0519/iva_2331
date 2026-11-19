"""Single-output RKNNLite classifier with RGA-backed ROI inputs."""

from __future__ import annotations

import logging
import threading
from typing import Any, Mapping

import numpy as np

from app.model_specs import ModelSpec
from app.yolo_lite import Detection, RoiPreprocessError, normalize_rga_roi


LOG = logging.getLogger("duchun.rk3588_python.classifier_lite")


class RknnLiteClassifier:
    """A model-specific RKNNLite classifier context."""

    def __init__(self, spec: ModelSpec) -> None:
        from rknnlite.api import RKNNLite

        if spec.adapter != "classifier":
            raise ValueError(f"{spec.role} adapter must be classifier")
        self.spec = spec
        self.labels = list(spec.class_names) or [
            line.strip()
            for line in spec.labels_path.read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
        if not self.labels:
            raise ValueError(f"{spec.role} has no class names")
        self._runtime_lock = threading.Lock()
        self._runtime = RKNNLite()
        if self._runtime.load_rknn(str(spec.model_path)) != 0:
            raise RuntimeError(f"cannot load RKNN classifier for {spec.role}: {spec.model_path}")
        if self._runtime.init_runtime(core_mask=spec.core_mask) != 0:
            self._runtime.release()
            raise RuntimeError(
                f"cannot initialize RKNN classifier for {spec.role} "
                f"core_mask={spec.core_mask:#x}"
            )
        self._validate_io()
        LOG.warning("loaded RKNNLite classifier role=%s core_mask=%#x", spec.role, spec.core_mask)

    def close(self) -> None:
        with self._runtime_lock:
            if self._runtime is not None:
                runtime = self._runtime
                self._runtime = None
                runtime.release()

    def __enter__(self) -> "RknnLiteClassifier":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()

    def infer_roi(
        self,
        frame: Any,
        roi: Mapping[str, int],
        parent: Mapping[str, int],
    ) -> Detection:
        requested_roi = dict(roi)
        actual_roi: dict[str, int] | None = None
        try:
            actual_roi = normalize_rga_roi(
                requested_roi,
                frame.width,
                frame.height,
                self.spec.width,
                self.spec.height,
                letterbox=False,
            )
            image = frame.rga_rgb(
                self.spec.width,
                self.spec.height,
                actual_roi["x"],
                actual_roi["y"],
                actual_roi["width"],
                actual_roi["height"],
                False,
            )
        except (RuntimeError, ValueError) as exc:
            raise RoiPreprocessError(self.spec.role, requested_roi, actual_roi, str(exc)) from exc
        detection = self.infer_rgb(image.rgb, parent)
        return Detection(
            **{
                **detection.__dict__,
                "roi_bbox": actual_roi,
                "parent_bbox": dict(parent),
            }
        )

    def infer_rgb(
        self,
        image: np.ndarray,
        bbox: Mapping[str, int] | None = None,
    ) -> Detection:
        if image.dtype != np.uint8 or image.shape != (self.spec.height, self.spec.width, 3):
            raise ValueError(
                f"{self.spec.role} requires NHWC uint8 "
                f"{self.spec.width}x{self.spec.height}"
            )
        with self._runtime_lock:
            if self._runtime is None:
                raise RuntimeError("RKNNLite classifier context is closed")
            outputs = self._runtime.inference(inputs=[image[np.newaxis, ...]])
            if outputs is None or len(outputs) != 1:
                raise RuntimeError(f"{self.spec.role} must produce exactly one output")
            logits = np.asarray(outputs[0], dtype=np.float32).reshape(-1)
        if logits.size != len(self.labels) or not np.isfinite(logits).all():
            raise ValueError(
                f"{self.spec.role} output size {logits.size} does not match "
                f"{len(self.labels)} labels"
            )
        probabilities = self.softmax(logits)
        class_id = int(np.argmax(probabilities))
        box = dict(bbox or {"x": 0, "y": 0, "width": self.spec.width, "height": self.spec.height})
        return Detection(
            class_id,
            self.labels[class_id],
            round(float(probabilities[class_id]), 4),
            box,
        )

    @staticmethod
    def softmax(logits: np.ndarray) -> np.ndarray:
        values = np.asarray(logits, dtype=np.float32)
        shifted = values - np.max(values)
        exponentials = np.exp(shifted)
        return exponentials / np.sum(exponentials)

    def _validate_io(self) -> None:
        input_attr = self._runtime.rknn_runtime.get_tensor_attr(0, is_output=False)
        count = int(getattr(input_attr, "n_dims", 0))
        dims = tuple(int(value) for value in getattr(input_attr, "dims", ())[:count])
        expected = (1, self.spec.height, self.spec.width, 3)
        if dims and dims != expected:
            self.close()
            raise ValueError(f"{self.spec.role} expects {dims}, manifest declares {expected}")
        output_attr = self._runtime.rknn_runtime.get_tensor_attr(0, is_output=True)
        output_count = int(getattr(output_attr, "n_dims", 0))
        output_dims = tuple(
            int(value) for value in getattr(output_attr, "dims", ())[:output_count]
        )
        if output_dims and int(np.prod(output_dims)) != len(self.labels):
            self.close()
            raise ValueError(
                f"{self.spec.role} output {output_dims} does not match "
                f"{len(self.labels)} labels"
            )
