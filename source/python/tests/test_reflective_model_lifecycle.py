from __future__ import annotations

from collections import Counter
from types import SimpleNamespace
import unittest
from unittest.mock import patch

import app.inference as inference_module
from app.yolo_lite import Detection, RoiPreprocessError
from compat.business_rules import RoiTask


class _FakeModel:
    created: Counter[str] = Counter()
    confidences: dict[str, list[float | None]] = {}

    def __init__(self, spec: SimpleNamespace) -> None:
        self.role = spec.role
        self.created[self.role] += 1

    def close(self) -> None:
        return None

    def infer_frame(self, _frame: object, confidence: float | None = None) -> list[Detection]:
        self.confidences.setdefault(self.role, []).append(confidence)
        name = "oil" if self.role == "oil_leak" else "person"
        return [Detection(0, name, 0.9, {"x": 100, "y": 80, "width": 200, "height": 300})]

    def infer_roi(self, _frame: object, roi: dict[str, int], parent: dict[str, int],
                  confidence: float | None = None) -> list[Detection]:
        self.confidences.setdefault(self.role, []).append(confidence)
        name = "no_reflective" if self.role == "reflective_clothes" else "hand"
        return [Detection(0, name, 0.9, {"x": 120, "y": 120, "width": 100, "height": 100}, roi, parent)]


class _RecoverableRoiModel:
    def __init__(self) -> None:
        self.calls = 0

    def infer_roi(self, _frame: object, roi: dict[str, int], parent: dict[str, int],
                  confidence: float | None = None) -> list[Detection]:
        self.calls += 1
        if self.calls == 1:
            raise RoiPreprocessError("smoking_phone", roi, roi, "test scheduler failure")
        return []


class _EmptyModel(_FakeModel):
    def infer_frame(self, _frame: object, confidence: float | None = None) -> list[Detection]:
        self.confidences.setdefault(self.role, []).append(confidence)
        return []


class _FakeClassifier:
    created: Counter[str] = Counter()

    def __init__(self, spec: SimpleNamespace) -> None:
        self.role = spec.role
        self.created[self.role] += 1

    def close(self) -> None:
        return None

    def infer_roi(self, _frame: object, roi: dict[str, int], parent: dict[str, int]) -> Detection:
        return Detection(1, "nomask", 0.9, parent, roi, parent)


class ReflectiveModelLifecycleTest(unittest.TestCase):
    @staticmethod
    def _specs(*roles: str) -> dict[str, SimpleNamespace]:
        return {
            role: SimpleNamespace(role=role, confidence=0.5)
            for role in roles
        }

    def setUp(self) -> None:
        _FakeModel.created.clear()
        _FakeModel.confidences.clear()
        _FakeClassifier.created.clear()

    def test_roi_preprocess_failure_skips_only_the_failed_roi(self) -> None:
        specs = self._specs("person_helmet", "smoking_phone")
        frame = SimpleNamespace(width=704, height=576)
        config = {1: {"algorithm_threshold": 0.5, "invalid_rois": [], "alarm_grade": "1",
                      "alarm_Interval_period": 10, "target_threshold": 0}}
        parent = {"x": 250, "y": 150, "width": 23, "height": 32}
        tasks = [
            RoiTask("smoking_phone", parent, {"x": 248, "y": 146, "width": 46, "height": 54}, (1,)),
            RoiTask("smoking_phone", parent, {"x": 200, "y": 100, "width": 100, "height": 100}, (1,)),
        ]
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value={}), \
             patch.object(inference_module, "RknnLiteYoloV5", _FakeModel), \
             patch.object(inference_module, "build_roi_tasks", return_value=tasks):
            inference = inference_module.TRTInferCompat("unused", "unused", enabled_algorithms={1})
            recoverable = _RecoverableRoiModel()
            inference.roi_models["smoking_phone"] = recoverable
            errors: dict[str, int] = {}
            inference.infer(frame, config, model_errors=errors)
        self.assertEqual(recoverable.calls, 2)
        self.assertEqual(errors, {"smoking_phone": 1})

    def test_model_is_created_once_and_shared_across_frames(self) -> None:
        specs = self._specs("person_helmet", "smoking_phone", "reflective_clothes")
        frame = SimpleNamespace(width=1280, height=720)
        config = {6: {"algorithm_threshold": 0.5, "invalid_rois": [], "alarm_grade": "1",
                      "alarm_Interval_period": 10, "target_threshold": 0}}
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value={}), \
             patch.object(inference_module, "RknnLiteYoloV5", _FakeModel):
            inference = inference_module.TRTInferCompat("unused", "unused")
            inference.infer(frame, config)
            inference.infer(frame, config)
            inference.close()
        self.assertEqual(_FakeModel.created, Counter({
            "person_helmet": 1, "smoking_phone": 1, "reflective_clothes": 1,
        }))

    def test_full_frame_models_are_lazy_and_reused(self) -> None:
        roles = ("person_helmet", "smoking_phone", "reflective_clothes",
                 "oil_leak", "uniform", "pipe_oil_leak")
        specs = self._specs(*roles)
        frame = SimpleNamespace(width=1280, height=720)
        config = {29: {"algorithm_threshold": 0.5, "invalid_rois": [], "alarm_grade": "1",
                       "alarm_Interval_period": 10, "target_threshold": 0}}
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value={}), \
             patch.object(inference_module, "RknnLiteYoloV5", _FakeModel):
            inference = inference_module.TRTInferCompat("unused", "unused")
            self.assertNotIn("oil_leak", _FakeModel.created)
            inference.infer(frame, config)
            inference.infer(frame, config)
            inference.close()
        self.assertEqual(_FakeModel.created["oil_leak"], 1)
        self.assertEqual(_FakeModel.created["uniform"], 0)
        self.assertEqual(_FakeModel.created["pipe_oil_leak"], 0)

    def test_threshold_overrides_are_camera_specific(self) -> None:
        specs = self._specs("person_helmet", "oil_leak")
        frame = SimpleNamespace(width=1280, height=720)
        first = {29: {"algorithm_threshold": 0.25, "invalid_rois": []}}
        second = {29: {"algorithm_threshold": 0.75, "invalid_rois": []}}
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value={}), \
             patch.object(inference_module, "RknnLiteYoloV5", _FakeModel):
            inference = inference_module.TRTInferCompat(
                "unused", "unused", enabled_algorithms={29}
            )
            inference.infer(frame, first)
            inference.infer(frame, second)

        self.assertEqual(_FakeModel.created["oil_leak"], 1)
        self.assertEqual(_FakeModel.confidences["oil_leak"], [0.25, 0.75])
        self.assertNotIn("person_helmet", _FakeModel.confidences)
        self.assertEqual(first[29]["algorithm_threshold"], 0.25)
        self.assertEqual(second[29]["algorithm_threshold"], 0.75)

    def test_shared_roles_use_lowest_threshold_and_primary_dependencies(self) -> None:
        specs = self._specs("person_helmet", "smoking_phone", "reflective_clothes")
        frame = SimpleNamespace(width=1280, height=720)
        config = {
            0: {"algorithm_threshold": 0.30, "invalid_rois": []},
            1: {"algorithm_threshold": 0.70, "invalid_rois": []},
            4: {"algorithm_threshold": 0.60, "invalid_rois": []},
            6: {"algorithm_threshold": 0.35, "invalid_rois": []},
            7: {"algorithm_threshold": 0.65, "invalid_rois": []},
        }
        parent = {"x": 100, "y": 80, "width": 200, "height": 300}
        tasks = [
            RoiTask("smoking_phone", parent, parent, (0, 1)),
            RoiTask("reflective_clothes", parent, parent, (6, 7)),
        ]
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value={}), \
             patch.object(inference_module, "RknnLiteYoloV5", _FakeModel), \
             patch.object(inference_module, "build_roi_tasks", return_value=tasks):
            inference = inference_module.TRTInferCompat(
                "unused", "unused", enabled_algorithms=set(config)
            )
            inference.infer(frame, config)

        self.assertEqual(_FakeModel.confidences["person_helmet"], [0.30])
        self.assertEqual(_FakeModel.confidences["smoking_phone"], [0.30])
        self.assertEqual(_FakeModel.confidences["reflective_clothes"], [0.35])

    def test_full_frame_thresholds_zero_floor_and_percent_normalization(self) -> None:
        roles = ("person_helmet", "oil_leak", "uniform", "pipe_oil_leak")
        specs = self._specs(*roles)
        frame = SimpleNamespace(width=1280, height=720)
        config = {
            29: {"algorithm_threshold": 0.0, "invalid_rois": []},
            30: {"algorithm_threshold": 45, "invalid_rois": []},
            31: {"algorithm_threshold": 100, "invalid_rois": []},
        }
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value={}), \
             patch.object(inference_module, "RknnLiteYoloV5", _FakeModel):
            inference = inference_module.TRTInferCompat(
                "unused", "unused", enabled_algorithms=set(config)
            )
            inference.infer(frame, config)

        self.assertNotIn("person_helmet", _FakeModel.confidences)
        self.assertEqual(_FakeModel.confidences["oil_leak"], [0.01])
        self.assertEqual(_FakeModel.confidences["uniform"], [0.45])
        self.assertEqual(_FakeModel.confidences["pipe_oil_leak"], [1.0])
        self.assertEqual(config[29]["algorithm_threshold"], 0.0)

    def test_mask_classifier_is_loaded_only_when_enabled(self) -> None:
        specs = self._specs("person_helmet", "mask")
        frame = SimpleNamespace(width=1280, height=720)
        config = {8: {"algorithm_threshold": 0.75, "invalid_rois": []}}
        parent = {"x": 100, "y": 80, "width": 80, "height": 80}
        tasks = [RoiTask("mask", parent, parent, (8,))]
        maps = {"mask": {"mask": {"kind": "alarm", "algorithm": 8}}}
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value=maps), \
             patch.object(inference_module, "RknnLiteYoloV5", _FakeModel), \
             patch.object(inference_module, "RknnLiteClassifier", _FakeClassifier), \
             patch.object(inference_module, "build_roi_tasks", return_value=tasks):
            inference = inference_module.TRTInferCompat(
                "unused", "unused", enabled_algorithms={8}
            )
            inference.infer(frame, config)
            inference.close()

        self.assertEqual(_FakeClassifier.created, Counter({"mask": 1}))
        self.assertEqual(_FakeModel.confidences["person_helmet"], [0.5])

    def test_leave_flag_reports_algorithm_10_enablement_with_empty_results(self) -> None:
        specs = self._specs("person_helmet")
        frame = SimpleNamespace(width=1280, height=720)
        config = {10: {"algorithm_threshold": 0.5, "invalid_rois": []}}
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value={}), \
             patch.object(inference_module, "RknnLiteYoloV5", _EmptyModel):
            inference = inference_module.TRTInferCompat(
                "unused", "unused", enabled_algorithms={10}
            )
            persons, alarms, leave_enabled = inference.infer(frame, config)

        self.assertEqual(persons, [])
        self.assertEqual(alarms, [])
        self.assertTrue(leave_enabled)

    def test_leave_flag_is_false_without_algorithm_10(self) -> None:
        specs = self._specs("person_helmet")
        frame = SimpleNamespace(width=1280, height=720)
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value={}), \
             patch.object(inference_module, "RknnLiteYoloV5", _EmptyModel):
            inference = inference_module.TRTInferCompat(
                "unused", "unused", enabled_algorithms={4, 11}
            )
            for config in (
                {},
                {4: {"algorithm_threshold": 0.5, "invalid_rois": []}},
                {11: {"algorithm_threshold": 0.5, "invalid_rois": []}},
            ):
                persons, alarms, leave_enabled = inference.infer(frame, config)
                self.assertEqual(persons, [])
                self.assertEqual(alarms, [])
                self.assertFalse(leave_enabled)


if __name__ == "__main__":
    unittest.main()
