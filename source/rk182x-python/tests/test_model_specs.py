from __future__ import annotations

import json
import tempfile
import unittest
from collections import Counter
from pathlib import Path

from app.model_specs import load_specs


class ModelSpecsTest(unittest.TestCase):
    def test_production_roles_use_distributed_single_core_masks(self) -> None:
        manifest = Path(__file__).resolve().parents[1] / "models" / "initial_models.rk182x.json"
        specs = load_specs(manifest)
        expected = {
            "person_helmet": 0x1, "smoking_phone": 0x2,
            "reflective_clothes": 0x4, "mask": 0x8,
            "fire_smoke": 0x10, "safety_belt": 0x20,
            "vehicle": 0x40, "oil_leak": 0x80,
            "uniform": 0x20, "pipe_oil_leak": 0x2,
            "bolt": 0x4,
        }
        self.assertEqual({role: spec.core_mask for role, spec in specs.items()}, expected)
        self.assertTrue(all(mask & (mask - 1) == 0 for mask in expected.values()))
        self.assertEqual(expected["person_helmet"], 0x1)
        self.assertTrue(
            all(mask != 0x1 for role, mask in expected.items() if role != "person_helmet")
        )
        secondary_counts = Counter(
            mask for role, mask in expected.items() if role != "person_helmet"
        )
        self.assertEqual(set(secondary_counts), {0x2, 0x4, 0x8, 0x10, 0x20, 0x40, 0x80})
        self.assertTrue(all(1 <= count <= 2 for count in secondary_counts.values()))

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
        self.assertEqual(spec.core_mask, 0x1)

    def test_classifier_does_not_require_yolo_geometry(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "models.json"
            manifest.write_text(json.dumps({
                "model_root": directory,
                "models": [{
                    "role": "mask", "adapter": "classifier",
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
        self.assertEqual(spec.core_mask, 0x1)

    def test_invalid_single_core_masks_are_rejected(self) -> None:
        for invalid in ("0x0", "0x3", "0x100", "not-a-mask"):
            with self.subTest(core_mask=invalid), tempfile.TemporaryDirectory() as directory:
                manifest = Path(directory) / "models.json"
                manifest.write_text(json.dumps({
                    "model_root": directory,
                    "models": [{
                        "role": "oil_leak", "directory": "oil_leak",
                        "model": "model.rknn", "weight": "model.weight",
                        "class_names": ["oil"],
                        "input": {"width": 640, "height": 640},
                        "algorithms": [29], "core_mask": invalid,
                    }],
                }), encoding="utf-8")
                with self.assertRaisesRegex(ValueError, "oil_leak"):
                    load_specs(manifest)


if __name__ == "__main__":
    unittest.main()
