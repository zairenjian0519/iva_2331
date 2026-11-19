from __future__ import annotations

from pathlib import Path
from types import ModuleType, SimpleNamespace
import sys
import unittest
from unittest.mock import patch

from app.classifier_lite import RknnLiteClassifier
from app.yolo_lite import RknnLiteYoloV5


class FakeRKNNLite:
    core_masks: list[int] = []

    def __init__(self) -> None:
        self.rknn_runtime = self

    def load_rknn(self, _path: str) -> int:
        return 0

    def init_runtime(self, core_mask: int) -> int:
        self.core_masks.append(core_mask)
        return 0

    def get_tensor_attr(self, _index: int, is_output: bool):
        return SimpleNamespace(n_dims=0, dims=())

    def release(self) -> None:
        return None


def fake_api_module() -> ModuleType:
    module = ModuleType("rknnlite.api")
    module.RKNNLite = FakeRKNNLite
    return module


def spec(role: str, adapter: str, core_mask: int) -> SimpleNamespace:
    return SimpleNamespace(
        role=role,
        adapter=adapter,
        core_mask=core_mask,
        class_names=("class0",),
        labels_path=Path("unused-labels.txt"),
        model_path=Path("unused-model.rknn"),
        width=640 if adapter == "yolov5" else 224,
        height=640 if adapter == "yolov5" else 224,
    )


class CoreMaskRuntimeTest(unittest.TestCase):
    def setUp(self) -> None:
        FakeRKNNLite.core_masks = []

    def test_yolo_uses_model_core_mask(self) -> None:
        with patch.dict(sys.modules, {"rknnlite.api": fake_api_module()}):
            model = RknnLiteYoloV5(spec("person_helmet", "yolov5", 0x1))
            model.close()
        self.assertEqual(FakeRKNNLite.core_masks, [0x1])

    def test_classifier_uses_model_core_mask(self) -> None:
        with patch.dict(sys.modules, {"rknnlite.api": fake_api_module()}):
            model = RknnLiteClassifier(spec("mask", "classifier", 0x2))
            model.close()
        self.assertEqual(FakeRKNNLite.core_masks, [0x2])


if __name__ == "__main__":
    unittest.main()
