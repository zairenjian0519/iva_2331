from __future__ import annotations

import unittest

import rk_mpp_pipeline


class LatestFrameDoubleBufferTest(unittest.TestCase):
    def test_latest_frame_replaces_pending_without_touching_in_flight(self) -> None:
        result = rk_mpp_pipeline.latest_frame_buffer_probe()
        self.assertEqual(result["capacity"], 2)
        self.assertEqual(result["dropped"], 2)
        self.assertEqual(result["first_consumed_pts_us"], 3)
        self.assertEqual(result["second_consumed_pts_us"], 4)
        self.assertEqual(result["pending_frames"], 0)
        self.assertEqual(result["in_flight_frames"], 1)


if __name__ == "__main__":
    unittest.main()
