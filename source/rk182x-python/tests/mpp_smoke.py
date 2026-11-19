"""Decode a local H.264 file through the same native pipeline as an RTSP stream."""

from __future__ import annotations

import sys
import time

from rk_mpp_pipeline import CameraConfig, MppRtspManager


def main() -> None:
    config = CameraConfig()
    config.camera_id = 1
    config.rtsp_url = sys.argv[1]
    manager = MppRtspManager([config])
    manager.start()
    try:
        deadline = time.monotonic() + 10
        frame = None
        while frame is None and time.monotonic() < deadline:
            frame = manager.read_latest(1, timeout_ms=500)
        print("stats", manager.stream_stats(1).state, manager.stream_stats(1).decoded_frames)
        if frame is None:
            raise RuntimeError(manager.stream_stats(1).last_error or "no decoded frame")
        print(
            "frame",
            frame.pixel_format,
            frame.width,
            frame.height,
            frame.yuv.shape,
            frame.rgb.shape,
            frame.rgb.flags.c_contiguous,
            frame.rgb.flags.writeable,
        )
    finally:
        manager.stop()


if __name__ == "__main__":
    main()
