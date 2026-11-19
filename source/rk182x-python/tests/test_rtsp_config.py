from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from rtsp_config import load_camera_configs


class RtspConfigTest(unittest.TestCase):
    def test_credentials_are_percent_encoded(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "rtsp.json"
            path.write_text(
                json.dumps(
                    {
                        "cameras": [
                            {
                                "camera_id": 5,
                                "rtsp_url": "rtsp://192.168.1.10:554/live",
                                "username": "user@example",
                                "password": "a/b:c",
                            }
                        ]
                    }
                ),
                encoding="utf-8",
            )
            [camera] = load_camera_configs(path)
        self.assertEqual(camera.camera_id, 5)
        self.assertEqual(camera.rtsp_url, "rtsp://user%40example:a%2Fb%3Ac@192.168.1.10:554/live")


if __name__ == "__main__":
    unittest.main()
