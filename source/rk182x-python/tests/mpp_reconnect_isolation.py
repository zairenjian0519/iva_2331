"""Board integration check for per-camera RTSP failure isolation."""

from __future__ import annotations

import argparse
import json
import time

import redis

from app.legacy_config import build_jobs
from rk_mpp_pipeline import CameraConfig, MppRtspManager


def _config(camera_id: int, url: str, codec: str = "auto") -> CameraConfig:
    config = CameraConfig()
    config.camera_id = camera_id
    config.rtsp_url = url
    config.codec = codec if codec in {"h264", "h265", "hevc"} else "auto"
    config.reconnect_delay_ms = 5000
    config.timeout_ms = 5000
    return config


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--good-camera-id", required=True)
    parser.add_argument("--duration", type=float, default=12.0)
    parser.add_argument("--bad-url", default="rtsp://127.0.0.1:8554/definitely-missing")
    args = parser.parse_args()

    client = redis.Redis(host="127.0.0.1", port=6379, db=1)
    try:
        jobs = build_jobs(client.get("camera_list_info"), client.get("algorithm_list_info"))
    finally:
        client.close()
    good_job = next((job for job in jobs if job.camera_id == args.good_camera_id), None)
    if good_job is None:
        raise RuntimeError(f"camera {args.good_camera_id} is not enabled in Redis")

    manager = MppRtspManager([
        _config(1, good_job.rtsp_url, good_job.codec),
        _config(2, args.bad_url),
    ])
    started_at = time.monotonic()
    manager.start()
    start_seconds = time.monotonic() - started_at
    good_frames = 0
    try:
        deadline = time.monotonic() + args.duration
        while time.monotonic() < deadline:
            if manager.read_latest(1, timeout_ms=100) is not None:
                good_frames += 1
        good = manager.stream_stats(1)
        bad = manager.stream_stats(2)
        result = {
            "manager_start_seconds": round(start_seconds, 3),
            "good_frames": good_frames,
            "good_state": good.state,
            "good_decoded_frames": good.decoded_frames,
            "bad_state": bad.state,
            "bad_errors": bad.errors,
            "bad_reconnects": bad.reconnects,
            "bad_last_error": bad.last_error,
        }
        print(json.dumps(result, ensure_ascii=True, sort_keys=True))
        if start_seconds >= 1.0:
            raise RuntimeError("manager.start() performed blocking network work")
        if good_frames == 0 or good.decoded_frames == 0:
            raise RuntimeError("healthy stream did not continue while the other stream was offline")
        if bad.errors == 0 or bad.reconnects == 0 or bad.state not in {"connecting", "backoff"}:
            raise RuntimeError("offline stream did not enter its independent reconnect loop")
    finally:
        stopped_at = time.monotonic()
        manager.stop()
        stop_seconds = time.monotonic() - stopped_at
        print(json.dumps({"manager_stop_seconds": round(stop_seconds, 3)}))
        if stop_seconds >= 2.0:
            raise RuntimeError("manager.stop() did not interrupt reconnect backoff promptly")


if __name__ == "__main__":
    main()
