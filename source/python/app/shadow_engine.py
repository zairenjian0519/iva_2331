"""Read-only Redis shadow engine for the first RKNNLite migration scope."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import signal
import threading
import time
from typing import Any

import redis

from app.inference import TRTInferCompat
from app.legacy_config import CameraJob, build_jobs
from app.snapshot import alarm_snapshot
from rk_mpp_pipeline import CameraConfig, MppRtspManager


class JsonlWriter:
    def __init__(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        self._path = path
        self._lock = threading.Lock()

    def write(self, value: dict[str, Any]) -> None:
        with self._lock, self._path.open("a", encoding="utf-8") as target:
            target.write(json.dumps(value, ensure_ascii=False, sort_keys=True) + "\n")


class ShadowEngine:
    def __init__(self, jobs: list[CameraJob], inference: TRTInferCompat, writer: JsonlWriter) -> None:
        self.jobs = {job.camera_id: job for job in jobs}
        self.inference = inference
        self.writer = writer
        self.stop = threading.Event()
        self.last_run: dict[str, float] = {}
        configs: list[CameraConfig] = []
        for job in jobs:
            config = CameraConfig()
            config.camera_id = int(job.camera_id)
            config.rtsp_url = job.rtsp_url
            config.codec = job.codec if job.codec in {"h264", "h265", "hevc"} else "auto"
            config.reconnect_delay_ms = 5000
            config.timeout_ms = 5000
            configs.append(config)
        self.manager = MppRtspManager(configs)

    def run(self) -> None:
        self.manager.start()
        try:
            while not self.stop.is_set():
                for camera_id in self.manager.camera_ids:
                    job = self.jobs[str(camera_id)]
                    frame = self.manager.read_latest(camera_id, timeout_ms=25)
                    if frame is None:
                        continue
                    now = time.monotonic()
                    # Match engine.py's count < 2 behavior before tuning throughput.
                    if now - self.last_run.get(job.camera_id, 0.0) < 0.5:
                        continue
                    self.last_run[job.camera_id] = now
                    persons, alarms, is_leave = self.inference.infer(frame, job.ai_dict)
                    image_b64 = alarm_snapshot(frame) if persons or alarms or is_leave else None
                    self.writer.write({
                        "schema": "duchun.rk3588-python.shadow.v1",
                        "camera_id": job.camera_id,
                        "rtsp_ip": job.camera_ip,
                        "pts_us": frame.pts_us,
                        "ori_hw": [frame.height, frame.width],
                        "result1": persons,
                        "result2": alarms,
                        "is_leave": is_leave,
                        "img": image_b64,
                        "rois": job.rois_dict,
                        "statis": {},
                        "line1": 2,
                        "line2": 1,
                        "is_resize": not (frame.height > 720 and frame.width > 1080),
                        "stream_stats": self._stats(camera_id),
                    })
        finally:
            self.manager.stop()

    def _stats(self, camera_id: int) -> dict[str, Any]:
        stats = self.manager.stream_stats(camera_id)
        return {name: getattr(stats, name) for name in (
            "packets", "decoded_frames", "delivered_frames", "dropped_frames",
            "errors", "reconnects", "state", "last_error",
        )}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-manifest", default="models/initial_models.json")
    parser.add_argument("--role-manifest", default="models/roles.json")
    parser.add_argument("--output", default=os.getenv("DUCHUN_SHADOW_OUTPUT", "runtime/shadow/events.jsonl"))
    parser.add_argument("--redis-host", default=os.getenv("DUCHUN_REDIS_HOST", "127.0.0.1"))
    parser.add_argument("--redis-port", type=int, default=int(os.getenv("DUCHUN_REDIS_PORT", "6379")))
    parser.add_argument("--redis-db", type=int, default=int(os.getenv("DUCHUN_REDIS_DB", "1")))
    args = parser.parse_args()

    client = redis.Redis(host=args.redis_host, port=args.redis_port, db=args.redis_db)
    jobs = build_jobs(client.get("camera_list_info"), client.get("algorithm_list_info"))
    if not jobs:
        raise RuntimeError("Redis did not yield any enabled camera jobs")
    inference = TRTInferCompat(args.model_manifest, args.role_manifest)
    engine = ShadowEngine(jobs, inference, JsonlWriter(Path(args.output)))
    signal.signal(signal.SIGTERM, lambda *_: engine.stop.set())
    signal.signal(signal.SIGINT, lambda *_: engine.stop.set())
    try:
        engine.run()
    finally:
        inference.close()
        client.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
