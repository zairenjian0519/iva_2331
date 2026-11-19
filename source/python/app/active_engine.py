"""Production RK3588 engine with one inference worker per camera."""

from __future__ import annotations

import argparse
from datetime import datetime
import json
import logging
import os
from pathlib import Path
from queue import Empty, Queue
import signal
import threading
import time
from typing import Any, Mapping

import redis
import requests

from camera_deployment import load as load_camera_deployment
from app.inference import TRTInferCompat
from app.legacy_config import CameraJob, build_jobs
from app.metrics import CameraMetricsWindow, JsonlMetricsWriter, RuntimeConfig
from app.preview import PreviewRefreshController
from app.snapshot import alarm_snapshot, write_camera_snapshot
from rk_mpp_pipeline import CameraConfig, MppRtspManager


LOG = logging.getLogger("duchun.rk3588_python.engine")
STATISTICS_ALGORITHMS = {10, 11, 12, 13}
WEEKDAY_KEYS = {0: "1", 1: "2", 2: "3", 3: "4", 4: "5", 5: "6", 6: "0"}


class JsonlWriter:
    def __init__(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        self._path = path
        self._lock = threading.Lock()

    def write(self, value: dict[str, Any]) -> None:
        with self._lock, self._path.open("a", encoding="utf-8") as target:
            target.write(json.dumps(value, ensure_ascii=False, sort_keys=True) + "\n")


def _within_valid_time(config: Mapping[str, Any], now: datetime | None = None) -> bool:
    schedule = config.get("valid_time")
    if not schedule:
        return True
    now = now or datetime.now()
    periods = schedule.get(WEEKDAY_KEYS[now.weekday()]) if isinstance(schedule, Mapping) else None
    if not periods:
        return False
    current = now.strftime("%H:%M")
    for period in periods:
        try:
            start, end = str(period).split("-", 1)
        except ValueError:
            LOG.warning("ignoring malformed valid_time interval %r", period)
            continue
        if start <= end and start <= current <= end:
            return True
        if start > end and (current >= start or current <= end):
            return True
    return False


def _enabled_algorithms(ai_dict: Mapping[int, Mapping[str, Any]]) -> dict[int, Mapping[str, Any]]:
    return {int(key): value for key, value in ai_dict.items() if _within_valid_time(value)}


def _line_style(height: int, width: int) -> tuple[int, int, bool]:
    return (8, 3, False) if height > 720 and width > 1080 else (2, 1, True)


class ActiveEngine:
    def __init__(
        self,
        jobs: list[CameraJob],
        inference: TRTInferCompat,
        endpoint: str,
        timeout: float,
        writer: JsonlWriter,
        runtime_config: RuntimeConfig,
        metrics_writer: JsonlMetricsWriter,
    ) -> None:
        self.jobs = {stream_id: job for stream_id, job in enumerate(jobs, start=1)}
        self.inference = inference
        self.endpoint = endpoint
        self.timeout = timeout
        self.writer = writer
        self.runtime_config = runtime_config
        self.metrics_writer = metrics_writer
        self.stop = threading.Event()
        self.worker_errors: Queue[tuple[str, Exception]] = Queue()
        self.threads: list[threading.Thread] = []
        self.preview_root = Path(os.getenv(
            "DUCHUN_CAMERA_IMAGE_ROOT",
            "/userdata/sps_test/iva-kyscene/data/alarm_picture/download/show_image",
        ))
        self.preview = PreviewRefreshController(
            (job.camera_id for job in jobs),
            interval_seconds=float(os.getenv("DUCHUN_CAMERA_REFRESH_SECONDS", "300")),
            mqtt_host=os.getenv("DUCHUN_MQTT_HOST", "127.0.0.1"),
            mqtt_port=int(os.getenv("DUCHUN_MQTT_PORT", "1883")),
            topic=os.getenv("DUCHUN_PREVIEW_TOPIC", "web_picrefresh"),
        )
        configs: list[CameraConfig] = []
        for stream_id, job in self.jobs.items():
            config = CameraConfig()
            config.camera_id, config.rtsp_url = stream_id, job.rtsp_url
            config.codec = job.codec if job.codec in {"h264", "h265", "hevc"} else "auto"
            config.reconnect_delay_ms, config.timeout_ms = 5000, 5000
            configs.append(config)
        self.manager = MppRtspManager(configs)

    def run(self) -> None:
        self.preview.start()
        try:
            self.manager.start()
            for stream_id in self.manager.camera_ids:
                job = self.jobs[stream_id]
                thread = threading.Thread(
                    target=self._run_camera,
                    args=(stream_id,),
                    name=f"rk3588-camera-{job.camera_id}",
                    daemon=True,
                )
                self.threads.append(thread)
                thread.start()
            while not self.stop.wait(0.2):
                self._raise_worker_error()
            self._raise_worker_error()
        finally:
            self.stop.set()
            for thread in self.threads:
                thread.join()
            self.manager.stop()
            self.preview.stop()
            self.metrics_writer.close()

    def _run_camera(self, stream_id: int) -> None:
        job = self.jobs[stream_id]
        session = requests.Session()
        geometry_cache: dict[
            tuple[int, int],
            tuple[dict[int, dict[str, Any]], dict[str, dict[str, Any]]],
        ] = {}
        logged_config = False
        enabled: dict[int, Mapping[str, Any]] = {}
        max_fps = self.runtime_config.max_fps(job.camera_id)
        last_inference = 0.0
        metrics = CameraMetricsWindow(
            job.camera_id,
            stream_id,
            self.runtime_config.metrics_interval_seconds,
        )
        try:
            while not self.stop.is_set():
                frame = self.manager.read_latest(stream_id, timeout_ms=25)
                now = time.monotonic()
                if frame is None:
                    self._emit_metrics(metrics, stream_id, enabled, now)
                    continue
                if self.preview.should_refresh(job.camera_id, now):
                    try:
                        write_camera_snapshot(frame, self.preview_root, job.camera_id)
                        metrics.record_preview_jpeg()
                        self.preview.mark_success(job.camera_id, now)
                    except Exception:
                        self.preview.mark_failure(job.camera_id, now)
                        LOG.exception("failed to refresh camera preview %s", job.camera_id)

                geometry_key = (frame.width, frame.height)
                if geometry_key not in geometry_cache:
                    geometry_cache[geometry_key] = job.geometry(frame.width, frame.height)
                ai_dict, rois_dict = geometry_cache[geometry_key]
                enabled = _enabled_algorithms(ai_dict)
                if not logged_config:
                    logged_config = True
                    LOG.warning(
                        "first frame camera=%s worker=%s local_time=%s configured=%s enabled=%s max_fps=%s",
                        job.camera_id,
                        threading.current_thread().name,
                        datetime.now().isoformat(timespec="seconds"),
                        sorted(ai_dict),
                        sorted(enabled),
                        max_fps,
                    )
                if not enabled:
                    self._emit_metrics(metrics, stream_id, enabled, now)
                    continue
                if max_fps > 0 and now - last_inference < 1.0 / max_fps:
                    self._emit_metrics(metrics, stream_id, enabled, now)
                    continue

                last_inference = now
                model_timings: dict[str, list[float]] = {}
                model_errors: dict[str, int] = {}
                persons, alarms, is_leave = self.inference.infer(
                    frame,
                    enabled,
                    model_timings=model_timings,
                    model_errors=model_errors,
                )
                metrics.record_inference()
                for role, values in model_timings.items():
                    for elapsed_ms in values:
                        metrics.record_model(role, elapsed_ms)
                for role, count in model_errors.items():
                    metrics.record_model_error(role, count)

                statistics = {
                    str(key): value
                    for key, value in enabled.items()
                    if key in STATISTICS_ALGORITHMS
                }
                if not (persons or alarms or is_leave or "10" in statistics):
                    self._emit_metrics(metrics, stream_id, enabled)
                    continue

                line1, line2, is_resize = _line_style(frame.height, frame.width)
                payload = {
                    "result1": persons,
                    "result2": alarms,
                    "detectmode": 0,
                    "img": alarm_snapshot(frame),
                    "camera_id": job.camera_id,
                    "ori_hw": [frame.height, frame.width],
                    "rtsp_ip": job.camera_ip,
                    "rois": rois_dict,
                    "statis": statistics,
                    "line1": line1,
                    "line2": line2,
                    "is_resize": is_resize,
                }
                metrics.record_alarm_jpeg()
                metrics.record_http(self._post(session, stream_id, job, frame, payload))
                self._emit_metrics(metrics, stream_id, enabled)
        except Exception as exc:
            if not self.stop.is_set():
                LOG.exception("camera worker failed camera=%s", job.camera_id)
                self.worker_errors.put((job.camera_id, exc))
                self.stop.set()
        finally:
            session.close()

    def _emit_metrics(
        self,
        metrics: CameraMetricsWindow,
        stream_id: int,
        enabled: Mapping[int, Any],
        now: float | None = None,
    ) -> None:
        record = metrics.maybe_snapshot(self.manager.stream_stats(stream_id), enabled, now)
        if record is not None:
            self.metrics_writer.write(record)

    def _raise_worker_error(self) -> None:
        try:
            camera_id, error = self.worker_errors.get_nowait()
        except Empty:
            return
        raise RuntimeError(f"camera worker failed: {camera_id}") from error

    def _post(
        self,
        session: requests.Session,
        stream_id: int,
        job: CameraJob,
        frame: Any,
        payload: dict[str, Any],
    ) -> bool:
        record = {
            "schema": "duchun.rk3588-python.active.v2",
            "camera_id": job.camera_id,
            "pts_us": frame.pts_us,
            "result1": payload["result1"],
            "result2": payload["result2"],
            "statis_keys": sorted(payload["statis"]),
            "image_bytes": len(payload["img"]),
            "stream_stats": self._stats(stream_id),
        }
        success = False
        try:
            response = session.post(self.endpoint, json=payload, timeout=self.timeout)
            response.raise_for_status()
            record["http_status"] = response.status_code
            success = True
        except requests.RequestException as exc:
            record["error"] = str(exc)
            LOG.warning("delivery failed for camera %s: %s", job.camera_id, exc)
        self.writer.write(record)
        return success

    def _stats(self, camera_id: int) -> dict[str, Any]:
        stats = self.manager.stream_stats(camera_id)
        return {
            name: getattr(stats, name)
            for name in (
                "packets",
                "packet_bytes",
                "decoded_frames",
                "delivered_frames",
                "consumed_frames",
                "dropped_frames",
                "errors",
                "reconnects",
                "state",
                "last_error",
            )
        }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-manifest", default="models/initial_models.json")
    parser.add_argument("--role-manifest", default="models/roles.json")
    parser.add_argument(
        "--runtime-config",
        default=os.getenv("DUCHUN_INFERENCE_RUNTIME_CONFIG", "config/inference_runtime.json"),
    )
    parser.add_argument(
        "--metrics-output",
        default=os.getenv(
            "DUCHUN_INFERENCE_METRICS_PATH",
            "runtime/logs/inference_metrics.jsonl",
        ),
    )
    parser.add_argument(
        "--endpoint",
        default=os.getenv("DUCHUN_RESULT_ENDPOINT", "http://127.0.0.1:8899/analyresult"),
    )
    parser.add_argument(
        "--request-timeout",
        type=float,
        default=float(os.getenv("DUCHUN_HTTP_TIMEOUT", "5")),
    )
    parser.add_argument(
        "--output",
        default=os.getenv("DUCHUN_ACTIVE_OUTPUT", "runtime/active/engine.jsonl"),
    )
    parser.add_argument("--redis-host", default=os.getenv("DUCHUN_REDIS_HOST", "127.0.0.1"))
    parser.add_argument("--redis-port", type=int, default=int(os.getenv("DUCHUN_REDIS_PORT", "6379")))
    parser.add_argument("--redis-db", type=int, default=int(os.getenv("DUCHUN_REDIS_DB", "1")))
    args = parser.parse_args()

    client = redis.Redis(host=args.redis_host, port=args.redis_port, db=args.redis_db)
    try:
        jobs = build_jobs(client.get("camera_list_info"), client.get("algorithm_list_info"))
    finally:
        client.close()
    deployment = load_camera_deployment()
    jobs = deployment.jobs_for(jobs, 0)
    LOG.warning("RK3588 assigned cameras=%s", [job.camera_id for job in jobs])
    if not jobs:
        stop = threading.Event()
        signal.signal(signal.SIGTERM, lambda *_: stop.set())
        signal.signal(signal.SIGINT, lambda *_: stop.set())
        LOG.warning("no RK3588 camera jobs; waiting for configuration restart")
        stop.wait()
        return 0

    enabled_algorithms = {
        int(algorithm)
        for job in jobs
        for algorithm in job.ai_dict
    }
    runtime_config = RuntimeConfig.load(args.runtime_config)
    inference = TRTInferCompat(
        args.model_manifest,
        args.role_manifest,
        enabled_algorithms=enabled_algorithms,
    )
    metrics_writer = JsonlMetricsWriter(args.metrics_output)
    engine = ActiveEngine(
        jobs,
        inference,
        args.endpoint,
        args.request_timeout,
        JsonlWriter(Path(args.output)),
        runtime_config,
        metrics_writer,
    )
    signal.signal(signal.SIGTERM, lambda *_: engine.stop.set())
    signal.signal(signal.SIGINT, lambda *_: engine.stop.set())
    try:
        engine.run()
    finally:
        inference.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
