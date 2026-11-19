from __future__ import annotations

import unittest

import numpy as np

from rknn_yolov5 import YoloV5RknnLite


class YoloV5LayoutTest(unittest.TestCase):
    def test_normalise_chw_head(self) -> None:
        tensor = np.zeros((255, 80, 80), dtype=np.float32)
        result = YoloV5RknnLite._normalise_head(tensor)
        self.assertEqual(result.shape, (80, 80, 3, 85))

    def test_normalise_hwc_head(self) -> None:
        tensor = np.zeros((40, 40, 255), dtype=np.float32)
        result = YoloV5RknnLite._normalise_head(tensor)
        self.assertEqual(result.shape, (40, 40, 3, 85))

    def test_iou(self) -> None:
        overlap = YoloV5RknnLite._iou(
            np.asarray((0, 0, 10, 10), dtype=np.float32),
            np.asarray(((0, 0, 10, 10), (20, 20, 30, 30)), dtype=np.float32),
        )
        np.testing.assert_allclose(overlap, np.asarray((1.0, 0.0), dtype=np.float32))


if __name__ == "__main__":
    unittest.main()
