"""Single-output RKNN3 classifier with RGA-backed ROI inputs."""

from __future__ import annotations

import threading
import time
from typing import Any, Mapping

import numpy as np

from app.model_specs import ModelSpec
from app.rknn3_yolo import Detection, RoiPreprocessError, normalize_rga_roi


class Rknn3Classifier:
    """A model-specific RKNN3 Lite classifier context."""

    def __init__(self, spec: ModelSpec, *, device_id: str | None = None, target: str = "rk1820") -> None:
        from rknn3lite.api import RKNN3Lite

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
        self._runtime = RKNN3Lite(verbose=False)
        if self._runtime.load_rknn(str(spec.model_path), str(spec.weight_path)) != 0:
            raise RuntimeError(f"cannot load RKNN3 classifier for {spec.role}: {spec.model_path}")
        if self._runtime.init_runtime(
            target=target, core_mask=spec.core_mask,
            device_id=device_id.encode("ascii") if device_id is not None else None
        ) != 0:
            self._runtime.release()
            raise RuntimeError(f"cannot initialize RKNN3 classifier for {spec.role}")
        self._output_attrs = list(self._runtime.get_outputs_tensor_attr() or [])
        self._validate_output()

    def close(self) -> None:
        with self._runtime_lock:
            if self._runtime is not None:
                runtime = self._runtime
                self._runtime = None
                runtime.release()

    def __enter__(self) -> "Rknn3Classifier":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()

    def infer_roi(
        self,
        frame: Any,
        roi: Mapping[str, int],
        parent: Mapping[str, int],
        phase_timings: dict[str, float] | None = None,
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
        detection = self.infer_rgb(image.rgb, parent, phase_timings)
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
        phase_timings: dict[str, float] | None = None,
    ) -> Detection:
        if image.dtype != np.uint8 or image.shape != (self.spec.height, self.spec.width, 3):
            raise ValueError(
                f"{self.spec.role} requires NHWC uint8 "
                f"{self.spec.width}x{self.spec.height}"
            )
        lock_wait_started = time.perf_counter()
        with self._runtime_lock:
            lock_acquired = time.perf_counter()
            if self._runtime is None:
                raise RuntimeError("RKNN3 classifier context is closed")
            npu_started = time.perf_counter()
            runtime_outputs = self._runtime.inference(inputs=[image[np.newaxis, ...]])
            if runtime_outputs is None or len(runtime_outputs) != 1:
                raise RuntimeError(f"{self.spec.role} must produce exactly one output")
            output = np.array(runtime_outputs[0], copy=True)
            npu_finished = time.perf_counter()
        if phase_timings is not None:
            phase_timings["runtime_lock_wait_ms"] = (
                lock_acquired - lock_wait_started
            ) * 1000.0
            phase_timings["npu_execute_ms"] = (
                npu_finished - npu_started
            ) * 1000.0
        postprocess_started = time.perf_counter()
        logits = self._dequantize(output, 0).reshape(-1)
        if logits.size != len(self.labels) or not np.isfinite(logits).all():
            raise ValueError(
                f"{self.spec.role} output size {logits.size} does not match "
                f"{len(self.labels)} labels"
            )
        probabilities = self.softmax(logits)
        class_id = int(np.argmax(probabilities))
        box = dict(bbox or {"x": 0, "y": 0, "width": self.spec.width, "height": self.spec.height})
        result = Detection(
            class_id,
            self.labels[class_id],
            round(float(probabilities[class_id]), 4),
            box,
        )
        if phase_timings is not None:
            phase_timings["postprocess_ms"] = (
                time.perf_counter() - postprocess_started
            ) * 1000.0
        return result

    @staticmethod
    def softmax(logits: np.ndarray) -> np.ndarray:
        values = np.asarray(logits, dtype=np.float32)
        shifted = values - np.max(values)
        exponentials = np.exp(shifted)
        return exponentials / np.sum(exponentials)

    def _dequantize(self, output: np.ndarray, index: int) -> np.ndarray:
        if np.issubdtype(output.dtype, np.floating):
            return output.astype(np.float32, copy=False)
        if index >= len(self._output_attrs):
            raise RuntimeError(f"missing RKNN3 output attributes for {self.spec.role}")
        attr = self._output_attrs[index]
        qnt = getattr(attr, "qnt_info", None)
        scale = getattr(attr, "scale", None)
        zero_point = getattr(attr, "zp", None)
        if qnt is not None:
            scale = scale if scale is not None else getattr(qnt, "scale", None)
            zero_point = zero_point if zero_point is not None else getattr(qnt, "zero_point", None)
        if scale is None or zero_point is None:
            raise RuntimeError(f"missing quantization metadata for {self.spec.role}")
        return (output.astype(np.float32) - float(zero_point)) * float(scale)

    def _validate_output(self) -> None:
        if len(self._output_attrs) > 1:
            self.close()
            raise ValueError(f"{self.spec.role} must expose one output")
