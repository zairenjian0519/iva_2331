from __future__ import annotations

import unittest
from types import SimpleNamespace
import threading
import time

import numpy as np

from app.model_specs import ModelSpec
from app.yolo_lite import Detection, RknnLiteYoloV5, RoiPreprocessError, Transform, normalize_rga_roi


class _ConcurrentRuntime:
    def __init__(self) -> None:
        self.active = 0
        self.max_active = 0
        self.guard = threading.Lock()

    def inference(self, inputs):
        with self.guard:
            self.active += 1
            self.max_active = max(self.max_active, self.active)
        time.sleep(0.02)
        with self.guard:
            self.active -= 1
        return [np.zeros((1, 1, 1, 1), dtype=np.float32)]


class YoloLayoutTest(unittest.TestCase):
    def test_full_frame_is_reprocessed_when_model_size_differs(self) -> None:
        class Frame:
            width = 1280
            height = 720
            rgb = np.zeros((640, 640, 3), dtype=np.uint8)
            letterbox = SimpleNamespace(scale=0.5, pad_left=0, pad_top=140)
            called_with: tuple[object, ...] | None = None

            def rga_rgb(self, *args: object) -> SimpleNamespace:
                self.called_with = args
                return SimpleNamespace(
                    rgb=np.zeros((512, 512, 3), dtype=np.uint8),
                    letterbox=SimpleNamespace(scale=0.4, pad_left=0, pad_top=112),
                )

        adapter = object.__new__(RknnLiteYoloV5)
        adapter.spec = SimpleNamespace(role="vehicle", width=512, height=512)
        captured: list[Transform] = []
        adapter._infer_rgb = lambda _image, transform, _confidence: captured.append(transform) or []
        frame = Frame()
        adapter.infer_frame(frame, 0.25)
        self.assertEqual(frame.called_with, (512, 512, 0, 0, 1280, 720, True))
        self.assertEqual(captured[0], Transform(0, 0, 1280, 720, 0.4, 0, 112))

    def test_small_roi_is_expanded_for_rga3(self) -> None:
        actual = normalize_rga_roi(
            {"x": 248, "y": 146, "width": 46, "height": 54},
            704, 576, 640, 640,
        )
        self.assertEqual(actual, {"x": 236, "y": 132, "width": 70, "height": 80})
        self.assertGreaterEqual(actual["width"], 68)
        self.assertLessEqual(min(640 / actual["width"], 640 / actual["height"]), 8.0)

    def test_narrow_roi_keeps_size_when_scale_is_supported(self) -> None:
        actual = normalize_rga_roi(
            {"x": 216, "y": 76, "width": 66, "height": 92},
            704, 576, 640, 640,
        )
        self.assertEqual(actual, {"x": 214, "y": 76, "width": 68, "height": 92})

    def test_small_roi_expansion_stays_inside_all_frame_edges(self) -> None:
        values = (
            {"x": 0, "y": 0, "width": 10, "height": 10},
            {"x": 694, "y": 0, "width": 10, "height": 10},
            {"x": 0, "y": 566, "width": 10, "height": 10},
            {"x": 694, "y": 566, "width": 10, "height": 10},
        )
        for requested in values:
            with self.subTest(requested=requested):
                actual = normalize_rga_roi(requested, 704, 576, 640, 640)
                self.assertTrue(all(value % 2 == 0 for value in actual.values()))
                self.assertGreaterEqual(actual["x"], 0)
                self.assertGreaterEqual(actual["y"], 0)
                self.assertLessEqual(actual["x"] + actual["width"], 704)
                self.assertLessEqual(actual["y"] + actual["height"], 576)
                self.assertGreaterEqual(actual["width"], 68)
                self.assertLessEqual(min(640 / actual["width"], 640 / actual["height"]), 8.0)

    def test_infer_roi_uses_actual_crop_for_transform_and_detection(self) -> None:
        class Frame:
            width = 704
            height = 576
            called_with: tuple[object, ...] | None = None

            def rga_rgb(self, *args: object) -> SimpleNamespace:
                self.called_with = args
                return SimpleNamespace(
                    rgb=np.zeros((640, 640, 3), dtype=np.uint8),
                    letterbox=SimpleNamespace(scale=8.0, pad_left=40, pad_top=0),
                )

        adapter = object.__new__(RknnLiteYoloV5)
        adapter.spec = SimpleNamespace(role="smoking_phone", width=640, height=640)
        captured: list[Transform] = []
        adapter._infer_rgb = lambda _image, transform, _confidence: (
            captured.append(transform)
            or [Detection(0, "hand", 0.8, {"x": 250, "y": 150, "width": 20, "height": 20})]
        )
        frame = Frame()
        parent = {"x": 250, "y": 150, "width": 23, "height": 32}
        result = adapter.infer_roi(
            frame,
            {"x": 248, "y": 146, "width": 46, "height": 54},
            parent,
        )
        actual = {"x": 236, "y": 132, "width": 70, "height": 80}
        self.assertEqual(frame.called_with, (640, 640, 236, 132, 70, 80, True))
        self.assertEqual(captured[0].source_x, 236)
        self.assertEqual(captured[0].source_y, 132)
        self.assertEqual(result[0].roi_bbox, actual)
        self.assertEqual(result[0].parent_bbox, parent)

    def test_rga_failure_is_wrapped_as_roi_preprocess_error(self) -> None:
        frame = SimpleNamespace(width=704, height=576)
        frame.rga_rgb = lambda *_args: (_ for _ in ()).throw(RuntimeError("scheduler failed"))
        adapter = object.__new__(RknnLiteYoloV5)
        adapter.spec = SimpleNamespace(role="smoking_phone", width=640, height=640)
        with self.assertRaises(RoiPreprocessError) as caught:
            adapter.infer_roi(
                frame,
                {"x": 248, "y": 146, "width": 46, "height": 54},
                {"x": 250, "y": 150, "width": 23, "height": 32},
            )
        self.assertEqual(caught.exception.actual_roi, {"x": 236, "y": 132, "width": 70, "height": 80})

    def test_chw_head_layout(self) -> None:
        adapter = object.__new__(RknnLiteYoloV5)
        adapter.labels = ["head", "person", "helmet"]
        result = adapter._to_hwc(np.zeros((24, 80, 80), dtype=np.float32), 3)
        self.assertEqual(result.shape, (80, 80, 3, 8))

    def test_hwc_head_layout(self) -> None:
        adapter = object.__new__(RknnLiteYoloV5)
        adapter.labels = ["head", "person", "helmet"]
        result = adapter._to_hwc(np.zeros((40, 40, 24), dtype=np.float32), 3)
        self.assertEqual(result.shape, (40, 40, 3, 8))

    def test_runtime_call_is_serialized(self) -> None:
        adapter = object.__new__(RknnLiteYoloV5)
        adapter.spec = SimpleNamespace(role="test", width=2, height=2, confidence=0.1)
        adapter._runtime_lock = threading.Lock()
        adapter._runtime = _ConcurrentRuntime()
        adapter._decode = lambda outputs, threshold: []
        adapter._nms = lambda values: values
        adapter._restore = lambda values, transform: []
        image = np.zeros((2, 2, 3), dtype=np.uint8)
        transform = Transform(0, 0, 2, 2, 1.0, 0, 0)
        threads = [
            threading.Thread(target=adapter._infer_rgb, args=(image, transform))
            for _ in range(2)
        ]
        for thread in threads:
            thread.start()
        for thread in threads:
            thread.join()
        self.assertEqual(adapter._runtime.max_active, 1)


if __name__ == "__main__":
    unittest.main()
