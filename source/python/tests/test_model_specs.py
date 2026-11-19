from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from app.model_specs import load_specs


class ModelSpecsTest(unittest.TestCase):
    def test_inline_class_names_do_not_require_a_labels_file(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "models.json"
            manifest.write_text(json.dumps({
                "model_root": directory,
                "models": [{
                    "role": "oil_leak", "directory": "has_oil", "model": "model.rknn",
                    "class_names": ["oil"], "input": {"width": 640, "height": 640},
                    "strides": [8, 16, 32],
                    "anchors": [[[10, 13]], [[30, 61]], [[116, 90]]],
                    "algorithms": [29],
                }],
            }), encoding="utf-8")
            spec = load_specs(manifest)["oil_leak"]
        self.assertEqual(spec.class_names, ("oil",))
        self.assertEqual(spec.model_path.name, "model.rknn")
        self.assertEqual(spec.adapter, "yolov5")
        self.assertEqual(spec.core_mask, 0x7)

    def test_classifier_does_not_require_yolo_geometry(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "models.json"
            manifest.write_text(json.dumps({
                "model_root": directory,
                "models": [{
                    "role": "mask", "adapter": "classifier", "core_mask": "0x2",
                    "directory": "mask_classifier", "model": "model_fp.rknn",
                    "class_names": ["mask", "nomask", "other"],
                    "input": {"width": 224, "height": 224},
                    "algorithms": [8, 9],
                }],
            }), encoding="utf-8")
            spec = load_specs(manifest)["mask"]
        self.assertEqual(spec.adapter, "classifier")
        self.assertEqual(spec.anchors, ())
        self.assertEqual(spec.strides, ())
        self.assertEqual(spec.class_names, ("mask", "nomask", "other"))
        self.assertEqual(spec.core_mask, 0x2)

    def test_invalid_core_mask_is_rejected_with_role(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "models.json"
            manifest.write_text(json.dumps({
                "model_root": directory,
                "models": [{
                    "role": "oil_leak", "directory": "has_oil",
                    "model": "model.rknn", "class_names": ["oil"],
                    "input": {"width": 640, "height": 640},
                    "algorithms": [29], "core_mask": "0x8",
                }],
            }), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "oil_leak.*0x8"):
                load_specs(manifest)


if __name__ == "__main__":
    unittest.main()
