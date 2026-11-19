from __future__ import annotations

from types import SimpleNamespace
import unittest

from app.metrics import CameraMetricsWindow, RuntimeConfig


class MetricsTest(unittest.TestCase):
    def test_runtime_config_camera_override(self) -> None:
        config = RuntimeConfig(10.0, 5.0, {"camera-a": 12.0})
        self.assertEqual(config.max_fps("camera-a"), 12.0)
        self.assertEqual(config.max_fps("camera-b"), 5.0)

    def test_window_uses_counter_deltas(self) -> None:
        window = CameraMetricsWindow("camera-a", 1, 10.0)
        window.started = 0.0
        window.record_inference()
        window.record_model("person_helmet", 20.0)
        window.record_model_phase("person_helmet", "runtime_lock_wait_ms", 1.0)
        window.record_model_phase("person_helmet", "runtime_lock_wait_ms", 3.0)
        window.record_model_phase("person_helmet", "npu_execute_ms", 10.0)
        window.record_model_phase("person_helmet", "npu_execute_ms", 12.0)
        window.record_model_phase("person_helmet", "postprocess_ms", 4.0)
        window.record_model_error("smoking_phone", 2)
        stats = SimpleNamespace(
            packets=250,
            packet_bytes=1_000_000,
            decoded_frames=240,
            delivered_frames=240,
            consumed_frames=100,
            dropped_frames=140,
            errors=0,
            reconnects=0,
            state="running",
            last_error="",
        )
        record = window.maybe_snapshot(stats, {4: {}}, now=10.0)
        self.assertIsNotNone(record)
        self.assertEqual(record["rtsp_packet_fps"], 25.0)
        self.assertEqual(record["decode_fps"], 24.0)
        self.assertEqual(record["consume_fps"], 10.0)
        self.assertEqual(record["drop_fps"], 14.0)
        self.assertEqual(record["inference_fps"], 0.1)
        self.assertEqual(record["per_model_calls"], {"person_helmet": 1})
        self.assertEqual(record["per_model_phase_avg_ms"]["person_helmet"], {
            "runtime_lock_wait_ms": 2.0,
            "npu_execute_ms": 11.0,
            "postprocess_ms": 4.0,
        })
        self.assertEqual(record["per_model_phase_p95_ms"]["person_helmet"], {
            "runtime_lock_wait_ms": 3.0,
            "npu_execute_ms": 12.0,
            "postprocess_ms": 4.0,
        })
        self.assertEqual(record["roi_preprocess_errors"], 2)
        self.assertEqual(record["per_model_errors"], {"smoking_phone": 2})


if __name__ == "__main__":
    unittest.main()
