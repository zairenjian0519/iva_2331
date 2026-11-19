"""Each RK182X card loads only the roles needed by its assigned cameras."""

from collections import defaultdict
from types import SimpleNamespace
import unittest
from unittest.mock import patch

import app.inference as inference_module


class FakeModel:
    created: dict[str, list[str]] = defaultdict(list)

    def __init__(self, spec, *, device_id=None, target="rk1820"):
        self.spec = spec
        self.closed = False
        self.created[device_id].append(spec.role)

    def infer_frame(self, _frame, confidence=None, phase_timings=None):
        return []

    def close(self):
        self.closed = True


class CardModelIsolationTest(unittest.TestCase):
    def test_independent_card_does_not_create_other_cards_models(self):
        FakeModel.created.clear()
        roles = {
            "person_helmet": (4,),
            "reflective_clothes": (6,),
            "oil_leak": (29,),
            "pipe_oil_leak": (31,),
        }
        specs = {
            role: SimpleNamespace(role=role, algorithms=algorithms, core_mask=1, confidence=0.5)
            for role, algorithms in roles.items()
        }
        with patch.object(inference_module, "load_specs", return_value=specs), \
             patch.object(inference_module, "result_maps", return_value={}), \
             patch.object(inference_module, "Rknn3YoloV5", FakeModel), \
             patch.object(inference_module, "evaluate", return_value=([], [])):
            card1 = inference_module.TRTInferCompat(
                "models.json", "roles.json", enabled_algorithms={4, 6}, device_id="card-1"
            )
            card2 = inference_module.TRTInferCompat(
                "models.json", "roles.json", enabled_algorithms={29, 31}, device_id="card-2"
            )
            self.assertEqual(FakeModel.created["card-1"], ["person_helmet", "reflective_clothes"])
            self.assertEqual(FakeModel.created["card-2"], ["oil_leak", "pipe_oil_leak"])
            self.assertIsNone(card2.primary)
            card2.infer(SimpleNamespace(height=480, width=640), {29: {}, 31: {}})
            with self.assertRaisesRegex(RuntimeError, "restart the engine"):
                card2.infer(SimpleNamespace(height=480, width=640), {4: {}})
            card1.close()
            card2.close()
            self.assertTrue(all(model.closed for model in [
                card1.primary, *card1.roi_models.values(), *card2.full_frame_models.values(),
            ]))


if __name__ == "__main__":
    unittest.main()
