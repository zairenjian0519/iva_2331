"""RK182X facade preserving the RK3588 Python business contract."""

from __future__ import annotations

from dataclasses import dataclass
import logging
import threading
import time
from typing import Any, Callable, Iterable, Mapping, TypeVar

from app.rknn3_classifier import Rknn3Classifier
from app.model_specs import load_specs, result_maps
from app.rknn3_yolo import Detection, Rknn3YoloV5, RoiPreprocessError
from compat.business_rules import build_roi_tasks, evaluate


LOG = logging.getLogger("duchun.rk182x_python.inference")

MIN_CANDIDATE_CONFIDENCE = 0.01
PRIMARY_CANDIDATE_ALGORITHMS = {
    0, 1,
    4, 5,
    6, 7,
    8, 9,
    10, 11, 12, 13,
}
ROLE_CANDIDATE_ALGORITHMS = {
    "smoking_phone": {0, 1},
    "fire_smoke": {2},
    "safety_belt": {3, 16},
    "reflective_clothes": {6, 7},
    "mask": {8, 9},
    "vehicle": {18},
    "bolt": {24},
    "oil_leak": {29},
    "uniform": {30},
    "pipe_oil_leak": {31},
}
FULL_FRAME_ROLE_ALGORITHMS = {
    role: ROLE_CANDIDATE_ALGORITHMS[role]
    for role in (
        "fire_smoke", "safety_belt", "vehicle", "bolt",
        "oil_leak", "uniform", "pipe_oil_leak",
    )
}
INDEPENDENT_FULL_FRAME_ALGORITHMS = frozenset().union(
    *FULL_FRAME_ROLE_ALGORITHMS.values()
)
ROI_ROLE_ALGORITHMS = {
    role: ROLE_CANDIDATE_ALGORITHMS[role]
    for role in ("smoking_phone", "reflective_clothes")
}
CLASSIFIER_ROI_ROLE_ALGORITHMS = {
    "mask": ROLE_CANDIDATE_ALGORITHMS["mask"],
}
T = TypeVar("T")


@dataclass(frozen=True)
class ModelEvent:
    detections: list[dict[str, Any]]


class TRTInferCompat:
    """RKNN3 replacement for the platform-specific portions of TRTInfer."""

    def __init__(
        self,
        model_manifest: str,
        role_manifest: str,
        enabled_algorithms: Iterable[int] | None = None,
        device_id: str | None = None,
        target: str = "rk1820",
    ) -> None:
        self.specs = load_specs(model_manifest)
        algorithms = None if enabled_algorithms is None else {int(value) for value in enabled_algorithms}
        self.supported_algorithms = {
            int(algorithm)
            for spec in self.specs.values()
            for algorithm in spec.algorithms
        } - {14, 15}
        unsupported = set() if algorithms is None else algorithms - self.supported_algorithms
        if unsupported:
            LOG.warning("unsupported RK182X algorithms will be skipped: %s", sorted(unsupported))
            algorithms &= self.supported_algorithms
        runtime_options = (
            {"device_id": device_id, "target": target}
            if device_id is not None or target != "rk1820" else {}
        )
        needs_primary = algorithms is None or bool(algorithms - INDEPENDENT_FULL_FRAME_ALGORITHMS)
        self.primary = (
            Rknn3YoloV5(self.specs["person_helmet"], **runtime_options)
            if needs_primary else None
        )
        self.roi_models = {
            role: Rknn3YoloV5(self.specs[role], **runtime_options)
            for role in ("smoking_phone", "reflective_clothes")
            if role in self.specs
            and (algorithms is None or ROI_ROLE_ALGORITHMS[role].intersection(algorithms))
        }
        self.classifier_models = {
            role: Rknn3Classifier(self.specs[role], **runtime_options)
            for role, required in CLASSIFIER_ROI_ROLE_ALGORITHMS.items()
            if role in self.specs
            and (algorithms is None or required.intersection(algorithms))
        }
        self.full_frame_models: dict[str, Rknn3YoloV5] = {
            role: Rknn3YoloV5(self.specs[role], **runtime_options)
            for role, required in FULL_FRAME_ROLE_ALGORITHMS.items()
            if role in self.specs
            and (algorithms is None or required.intersection(algorithms))
        }
        self._roi_error_log_lock = threading.Lock()
        self._roi_error_log_times: dict[str, float] = {}
        LOG.warning(
            "loaded RKNN3 models primary=%s roi=%s full=%s algorithms=%s core_masks=%s",
            "person_helmet" if self.primary is not None else "none",
            sorted([*self.roi_models, *self.classifier_models]),
            sorted(self.full_frame_models),
            sorted(algorithms) if algorithms is not None else "legacy-default",
            {
                role: f"0x{self.specs[role].core_mask:x}"
                for role in sorted({
                    *({"person_helmet"} if self.primary is not None else set()), *self.roi_models,
                    *self.classifier_models, *self.full_frame_models,
                })
            },
        )
        self.result_maps = result_maps(role_manifest)
        # The deployed primary model uses head rather than no_helmet.
        self.result_maps["person_helmet"] = {
            "head": {"kind": "alarm", "algorithm": 4},
            "person": {"kind": "person"},
            "helmet": {"kind": "alarm", "algorithm": 5},
        }

    def close(self) -> None:
        for model in [
            *([self.primary] if self.primary is not None else []),
            *self.roi_models.values(),
            *self.classifier_models.values(),
            *self.full_frame_models.values(),
        ]:
            try:
                model.close()
            except Exception:
                LOG.exception("failed to release RKNN3 model role=%s", model.spec.role)

    def infer(
        self,
        frame: Any,
        ai_dict: Mapping[int, Mapping[str, Any]],
        model_timings: dict[str, list[float]] | None = None,
        model_phase_timings: dict[str, dict[str, list[float]]] | None = None,
        model_errors: dict[str, int] | None = None,
    ) -> tuple[list[list[Any]], list[list[list[Any]]], bool]:
        configs = {
            int(algorithm): config
            for algorithm, config in ai_dict.items()
            if int(algorithm) in self.supported_algorithms
        }
        # Legacy is_leave means that algorithm 10 is enabled, not that leave was detected.
        leave_enabled = 10 in configs
        primary_required = self._requires_primary(configs)
        primary: list[dict[str, Any]] = []
        results: dict[str, ModelEvent] = {}
        if primary_required:
            if self.primary is None:
                raise RuntimeError(
                    "RKNN3 person_helmet was not loaded at engine startup; "
                    "restart the engine after changing camera algorithms"
                )
            primary_confidence = self._candidate_threshold(
                "person_helmet", configs, PRIMARY_CANDIDATE_ALGORITHMS
            )
            if CLASSIFIER_ROI_ROLE_ALGORITHMS["mask"].intersection(configs):
                primary_confidence = min(
                    primary_confidence,
                    self.specs["person_helmet"].confidence,
                )
            primary_values = self._timed(
                "person_helmet",
                lambda phases: self.primary.infer_frame(
                    frame, confidence=primary_confidence, phase_timings=phases
                ),
                model_timings,
                model_phase_timings,
            )
            primary = [self._as_dict(item) for item in primary_values]
            results["person_helmet"] = ModelEvent(primary)
        roi_results: dict[str, list[dict[str, Any]]] = {}
        roi_tasks = (
            build_roi_tasks(primary, configs, frame.height, frame.width)
            if primary_required
            else ()
        )
        for task in roi_tasks:
            detections = roi_results.setdefault(task.role, [])
            try:
                classifier = self.classifier_models.get(task.role)
                if classifier is not None:
                    values = [
                        self._timed(
                            task.role,
                            lambda phases, classifier=classifier, task=task: classifier.infer_roi(
                                frame, task.roi_bbox, task.parent_bbox,
                                phase_timings=phases,
                            ),
                            model_timings,
                            model_phase_timings,
                        )
                    ]
                else:
                    model = self.roi_models.get(task.role)
                    if model is None:
                        continue
                    candidate_confidence = self._candidate_threshold(
                        task.role, configs, task.algorithms
                    )
                    values = self._timed(
                        task.role,
                        lambda phases, model=model, task=task, confidence=candidate_confidence: model.infer_roi(
                            frame, task.roi_bbox, task.parent_bbox,
                            confidence=confidence, phase_timings=phases,
                        ),
                        model_timings,
                        model_phase_timings,
                    )
            except RoiPreprocessError as error:
                self._record_roi_error(task.role, error, model_errors)
                continue
            for item in values:
                detections.append(self._as_dict(item))
        for role, detections in roi_results.items():
            if detections:
                results[role] = ModelEvent(detections)
        for role, algorithms in FULL_FRAME_ROLE_ALGORITHMS.items():
            enabled = algorithms.intersection(configs)
            if not enabled:
                continue
            model = self._full_frame_model(role)
            candidate_confidence = self._candidate_threshold(role, configs, enabled)
            values = self._timed(
                role,
                lambda phases, model=model, confidence=candidate_confidence: model.infer_frame(
                    frame, confidence=confidence, phase_timings=phases
                ),
                model_timings,
                model_phase_timings,
            )
            detections = [self._as_dict(item) for item in values]
            results[role] = ModelEvent(detections)
        persons, alarms = evaluate(results, configs, self.result_maps, frame.height, frame.width)
        return persons, alarms, leave_enabled

    def _record_roi_error(
        self,
        role: str,
        error: RoiPreprocessError,
        model_errors: dict[str, int] | None,
    ) -> None:
        if model_errors is not None:
            model_errors[role] = model_errors.get(role, 0) + 1
        now = time.monotonic()
        with self._roi_error_log_lock:
            previous = self._roi_error_log_times.get(role, 0.0)
            if now - previous < 10.0:
                return
            self._roi_error_log_times[role] = now
        LOG.warning("skipping unsupported RGA ROI role=%s error=%s", role, error)

    def _full_frame_model(self, role: str) -> Rknn3YoloV5:
        model = self.full_frame_models.get(role)
        if model is None:
            raise RuntimeError(
                f"RKNN3 role {role} was not loaded at engine startup; "
                "restart the engine after changing camera algorithms"
            )
        return model

    @staticmethod
    def _timed(
        role: str,
        operation: Callable[[dict[str, float]], T],
        timings: dict[str, list[float]] | None,
        phase_timings: dict[str, dict[str, list[float]]] | None,
    ) -> T:
        phases: dict[str, float] = {}
        started = time.perf_counter()
        value = operation(phases)
        if timings is not None:
            timings.setdefault(role, []).append((time.perf_counter() - started) * 1000.0)
        if phase_timings is not None:
            target = phase_timings.setdefault(role, {})
            for name, elapsed_ms in phases.items():
                target.setdefault(name, []).append(float(elapsed_ms))
        return value

    @staticmethod
    def _requires_primary(configs: Mapping[int, Mapping[str, Any]]) -> bool:
        enabled = {int(algorithm) for algorithm in configs}
        return bool(enabled - INDEPENDENT_FULL_FRAME_ALGORITHMS)

    @staticmethod
    def _threshold(config: Mapping[str, Any]) -> float:
        value = float(config.get("algorithm_threshold", 0))
        if value > 1:
            value /= 100.0
        return min(max(value, 0.0), 1.0)

    def _candidate_threshold(
        self,
        role: str,
        configs: Mapping[int, Mapping[str, Any]],
        algorithms: Iterable[int],
    ) -> float:
        values = [
            self._threshold(configs[algorithm])
            for algorithm in algorithms
            if algorithm in configs
        ]
        if not values:
            return self.specs[role].confidence
        return max(MIN_CANDIDATE_CONFIDENCE, min(values))

    @staticmethod
    def _as_dict(value: Detection) -> dict[str, Any]:
        result: dict[str, Any] = {
            "class_id": value.class_id,
            "class_name": value.class_name,
            "score": value.score,
            "bbox": value.bbox,
        }
        if value.roi_bbox is not None:
            result["roi_bbox"] = value.roi_bbox
        if value.parent_bbox is not None:
            result["parent_bbox"] = value.parent_bbox
        return result
