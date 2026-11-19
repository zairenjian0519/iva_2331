from __future__ import annotations

import unittest

import numpy as np

from app.model_specs import ModelSpec
from app.yolo_lite import RknnLiteYoloV5


class YoloLayoutTest(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
