"""Reproduce the deployed small-ROI RGA case on a live board frame."""

from __future__ import annotations

import argparse
import json
import time

import redis

from app.legacy_config import build_jobs
from app.rknn3_yolo import normalize_rga_roi
from rk_mpp_pipeline import CameraConfig, MppRtspManager


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--camera-id", default="2000001864483994")
    parser.add_argument("--iterations", type=int, default=1)
    args = parser.parse_args()

    client = redis.Redis(host="127.0.0.1", port=6379, db=1)
    try:
        jobs = build_jobs(client.get("camera_list_info"), client.get("algorithm_list_info"))
    finally:
        client.close()
    job = next((value for value in jobs if value.camera_id == args.camera_id), None)
    if job is None:
        raise RuntimeError(f"camera {args.camera_id} is not enabled")

    config = CameraConfig()
    config.camera_id = 1
    config.rtsp_url = job.rtsp_url
    config.codec = job.codec if job.codec in {"h264", "h265", "hevc"} else "auto"
    config.reconnect_delay_ms = 5000
    config.timeout_ms = 5000
    manager = MppRtspManager([config])
    manager.start()
    try:
        deadline = time.monotonic() + 10.0
        frame = None
        while frame is None and time.monotonic() < deadline:
            frame = manager.read_latest(1, timeout_ms=500)
        if frame is None:
            raise RuntimeError(manager.stream_stats(1).last_error or "no decoded frame")
        requested = {"x": 248, "y": 146, "width": 46, "height": 54}
        actual = normalize_rga_roi(requested, frame.width, frame.height, 640, 640)
        image = None
        started_at = time.monotonic()
        for _ in range(max(1, args.iterations)):
            image = frame.rga_rgb(
                640, 640,
                actual["x"], actual["y"], actual["width"], actual["height"], True,
            )
        elapsed = time.monotonic() - started_at
        assert image is not None
        result = {
            "frame": [frame.width, frame.height, frame.hor_stride, frame.ver_stride],
            "requested_roi": requested,
            "actual_roi": actual,
            "scale": image.letterbox.scale,
            "padding": [image.letterbox.pad_left, image.letterbox.pad_top],
            "rgb_shape": list(image.rgb.shape),
            "iterations": max(1, args.iterations),
            "elapsed_seconds": round(elapsed, 3),
        }
        print(json.dumps(result, sort_keys=True))
        if actual != {"x": 236, "y": 132, "width": 70, "height": 80}:
            raise RuntimeError(f"unexpected normalized ROI: {actual}")
        if image.letterbox.scale > 8.0 or tuple(image.rgb.shape) != (640, 640, 3):
            raise RuntimeError("normalized ROI did not satisfy RGA/model requirements")
    finally:
        manager.stop()


if __name__ == "__main__":
    main()
