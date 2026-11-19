from __future__ import annotations

from types import SimpleNamespace
import threading
import unittest
from unittest.mock import patch

from app.inference import (
    FULL_FRAME_ROLE_ALGORITHMS,
    TRTInferCompat,
)


class FakeModel:
    def __init__(self, role: str) -> None:
        self.spec = SimpleNamespace(role=role)
        self.frame_calls = 0

    def infer_frame(self, _frame, confidence=None):
        self.frame_calls += 1
        return []

    def close(self) -> None:
        return None


def config() -> dict[str, float]:
    return {"algorithm_threshold": 0.5}


class PrimaryBypassTest(unittest.TestCase):
    def make_inference(self) -> TRTInferCompat:
        value = TRTInferCompat.__new__(TRTInferCompat)
        roles = {"person_helmet", *FULL_FRAME_ROLE_ALGORITHMS}
        value.specs = {
            role: SimpleNamespace(role=role, confidence=0.25)
            for role in roles
        }
        value.primary = FakeModel("person_helmet")
        value.roi_models = {}
        value.classifier_models = {}
        value.full_frame_models = {
            role: FakeModel(role)
            for role in FULL_FRAME_ROLE_ALGORITHMS
        }
        value._registry_lock = threading.Lock()
        value._roi_error_log_lock = threading.Lock()
        value._roi_error_log_times = {}
        value.result_maps = {}
        return value

    def test_dependency_classifier_is_conservative(self) -> None:
        value = self.make_inference()
        independent = {2, 3, 16, 18, 24, 29, 30, 31}
        self.assertFalse(value._requires_primary({item: config() for item in independent}))
        self.assertFalse(value._requires_primary({}))
        for algorithm in (0, 1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 99):
            self.assertTrue(value._requires_primary({algorithm: config()}), algorithm)

    def test_independent_algorithm_skips_primary_and_roi(self) -> None:
        value = self.make_inference()
        frame = SimpleNamespace(height=480, width=640)
        timings: dict[str, list[float]] = {}
        with patch("app.inference.build_roi_tasks") as build_roi, patch(
            "app.inference.evaluate", return_value=([], [])
        ) as evaluate:
            persons, alarms, leave = value.infer(
                frame,
                {29: config()},
                model_timings=timings,
            )
        self.assertEqual((persons, alarms, leave), ([], [], False))
        self.assertEqual(value.primary.frame_calls, 0)
        self.assertEqual(value.full_frame_models["oil_leak"].frame_calls, 1)
        self.assertNotIn("person_helmet", timings)
        build_roi.assert_not_called()
        results = evaluate.call_args.args[0]
        self.assertNotIn("person_helmet", results)
        self.assertIn("oil_leak", results)

    def test_mixed_algorithms_run_primary_once(self) -> None:
        value = self.make_inference()
        frame = SimpleNamespace(height=480, width=640)
        with patch("app.inference.build_roi_tasks", return_value=[]) as build_roi, patch(
            "app.inference.evaluate", return_value=([], [])
        ) as evaluate:
            value.infer(frame, {4: config(), 29: config()})
        self.assertEqual(value.primary.frame_calls, 1)
        self.assertEqual(value.full_frame_models["oil_leak"].frame_calls, 1)
        build_roi.assert_called_once()
        results = evaluate.call_args.args[0]
        self.assertIn("person_helmet", results)
        self.assertIn("oil_leak", results)

    def test_all_full_frame_roles_run_without_primary(self) -> None:
        value = self.make_inference()
        frame = SimpleNamespace(height=480, width=640)
        algorithms = {item: config() for item in (2, 3, 16, 18, 24, 29, 30, 31)}
        with patch("app.inference.build_roi_tasks") as build_roi, patch(
            "app.inference.evaluate", return_value=([], [])
        ):
            value.infer(frame, algorithms)
        self.assertEqual(value.primary.frame_calls, 0)
        build_roi.assert_not_called()
        for model in value.full_frame_models.values():
            self.assertEqual(model.frame_calls, 1, model.spec.role)

    def test_unknown_algorithm_preserves_primary_behavior(self) -> None:
        value = self.make_inference()
        frame = SimpleNamespace(height=480, width=640)
        with patch("app.inference.build_roi_tasks", return_value=[]), patch(
            "app.inference.evaluate", return_value=([], [])
        ):
            value.infer(frame, {99: config()})
        self.assertEqual(value.primary.frame_calls, 1)

    def test_algorithm_10_keeps_legacy_leave_flag(self) -> None:
        value = self.make_inference()
        frame = SimpleNamespace(height=480, width=640)
        with patch("app.inference.build_roi_tasks", return_value=[]), patch(
            "app.inference.evaluate", return_value=([], [])
        ):
            _, _, leave = value.infer(frame, {10: config()})
        self.assertTrue(leave)
        self.assertEqual(value.primary.frame_calls, 1)


if __name__ == "__main__":
    unittest.main()
