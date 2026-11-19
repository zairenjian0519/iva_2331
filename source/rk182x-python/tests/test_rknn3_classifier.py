from __future__ import annotations

from types import SimpleNamespace
import threading
import time
import unittest

import numpy as np

from app.rknn3_classifier import Rknn3Classifier


class _Runtime:
    def __init__(self, logits: list[float]) -> None:
        self.logits = np.asarray([logits], dtype=np.float32)
        self.calls = 0

    def inference(self, inputs: list[np.ndarray]) -> list[np.ndarray]:
        self.calls += 1
        if inputs[0].shape != (1, 224, 224, 3):
            raise AssertionError(inputs[0].shape)
        return [self.logits]


class Rknn3ClassifierTest(unittest.TestCase):
    @staticmethod
    def _adapter(logits: list[float]) -> Rknn3Classifier:
        adapter = object.__new__(Rknn3Classifier)
        adapter.spec = SimpleNamespace(role="mask", width=224, height=224)
        adapter.labels = ["mask", "nomask", "other"]
        adapter._runtime_lock = threading.Lock()
        adapter._runtime = _Runtime(logits)
        adapter._output_attrs = []
        return adapter

    def test_logits_are_converted_to_stable_probabilities(self) -> None:
        probabilities = Rknn3Classifier.softmax(np.asarray([1001.0, 1000.0, 999.0]))
        self.assertAlmostEqual(float(probabilities.sum()), 1.0, places=6)
        self.assertEqual(int(np.argmax(probabilities)), 0)

    def test_infer_rgb_returns_top_class_and_bbox(self) -> None:
        adapter = self._adapter([0.1, 3.0, -2.0])
        bbox = {"x": 10, "y": 20, "width": 30, "height": 40}
        result = adapter.infer_rgb(np.zeros((224, 224, 3), dtype=np.uint8), bbox)
        self.assertEqual((result.class_id, result.class_name), (1, "nomask"))
        self.assertGreater(result.score, 0.9)
        self.assertEqual(result.bbox, bbox)

    def test_roi_uses_stretched_rga_crop_and_parent_box(self) -> None:
        adapter = self._adapter([4.0, 0.0, -1.0])

        class Frame:
            width = 640
            height = 480
            called_with: tuple[object, ...] | None = None

            def rga_rgb(self, *args: object) -> SimpleNamespace:
                self.called_with = args
                return SimpleNamespace(rgb=np.zeros((224, 224, 3), dtype=np.uint8))

        frame = Frame()
        parent = {"x": 100, "y": 80, "width": 100, "height": 100}
        result = adapter.infer_roi(frame, parent, parent)
        self.assertIsNotNone(frame.called_with)
        self.assertEqual(frame.called_with[:2], (224, 224))
        self.assertEqual(frame.called_with[-1], False)
        self.assertEqual(result.bbox, parent)
        self.assertEqual(result.parent_bbox, parent)
        self.assertEqual(result.roi_bbox, parent)

    def test_phase_timings_split_lock_npu_and_postprocess(self) -> None:
        adapter = self._adapter([0.1, 3.0, -2.0])
        original = adapter._runtime.inference

        def delayed(inputs):
            time.sleep(0.01)
            return original(inputs)

        adapter._runtime.inference = delayed
        phases: dict[str, float] = {}
        adapter.infer_rgb(
            np.zeros((224, 224, 3), dtype=np.uint8),
            phase_timings=phases,
        )
        self.assertEqual(
            set(phases),
            {"runtime_lock_wait_ms", "npu_execute_ms", "postprocess_ms"},
        )
        self.assertGreaterEqual(phases["runtime_lock_wait_ms"], 0.0)
        self.assertGreaterEqual(phases["npu_execute_ms"], 8.0)
        self.assertGreaterEqual(phases["postprocess_ms"], 0.0)


if __name__ == "__main__":
    unittest.main()
