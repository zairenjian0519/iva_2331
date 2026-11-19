"""Production RK182X engine with one inference worker per camera."""

from __future__ import annotations

import argparse
from datetime import datetime
import json
import logging
import os
from pathlib import Path
from queue import Empty, Queue
import signal
import subprocess
import threading
import time
from typing import Any, Mapping

import redis
import requests

from camera_deployment import CameraDeployment, load as load_camera_deployment
from app.inference import TRTInferCompat
from app.legacy_config import CameraJob, build_jobs
from app.metrics import CameraMetricsWindow, JsonlMetricsWriter, RuntimeConfig
from app.model_specs import load_specs
from app.preview import PreviewRefreshController
from app.rk182x_devices import discover_device_ids
from app.snapshot import alarm_snapshot, write_camera_snapshot
from rk_mpp_pipeline import CameraConfig, MppRtspManager


LOG = logging.getLogger("duchun.rk182x_python.engine")
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
        camera_inferences: Mapping[str, TRTInferCompat] | None = None,
        deployment: CameraDeployment | None = None,
    ) -> None:
        self.jobs = {stream_id: job for stream_id, job in enumerate(jobs, start=1)}
        self.inference = inference
        self.camera_inferences = camera_inferences
        self.deployment = deployment
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
                    name=f"rk182x-camera-{job.camera_id}",
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
        inference = (
            self.camera_inferences[job.camera_id]
            if self.camera_inferences is not None else self.inference
        )
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
                model_phase_timings: dict[str, dict[str, list[float]]] = {}
                model_errors: dict[str, int] = {}
                persons, alarms, is_leave = inference.infer(
                    frame,
                    enabled,
                    model_timings=model_timings,
                    model_phase_timings=model_phase_timings,
                    model_errors=model_errors,
                )
                metrics.record_inference()
                for role, values in model_timings.items():
                    for elapsed_ms in values:
                        metrics.record_model(role, elapsed_ms)
                for role, phases in model_phase_timings.items():
                    for phase, values in phases.items():
                        for elapsed_ms in values:
                            metrics.record_model_phase(role, phase, elapsed_ms)
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
            if self.deployment is not None:
                slot = self.deployment.target_for(self.jobs[stream_id].camera_id)
                record["deployment_target"] = slot
                record["device_id"] = self.deployment.cards[slot].device_id
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
            "schema": "duchun.rk182x-python.active.v3",
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
    parser.add_argument("--model-manifest", default="models/initial_models.rk182x.json")
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
    try:
        deployment = load_camera_deployment()
    except (ValueError, OSError) as exc:
        LOG.error("invalid camera deployment configuration: %s", exc)
        return 78
    jobs = [job for job in jobs if deployment.target_for(job.camera_id) in (1, 2)]
    if not jobs:
        LOG.info("no RK182X camera jobs; service is not needed")
        return 0

    grouped = {slot: deployment.jobs_for(jobs, slot) for slot in (1, 2)}
    try:
        available = discover_device_ids()
    except (OSError, subprocess.SubprocessError) as exc:
        LOG.error("cannot discover RK182X devices: %s", exc)
        return 78
    models_by_slot: dict[int, TRTInferCompat] = {}
    try:
        selected_ids = [
            deployment.cards[slot].device_id
            for slot, group in grouped.items() if group and slot in deployment.cards
        ]
        if len(selected_ids) != sum(bool(group) for group in grouped.values()) or len(set(selected_ids)) != len(selected_ids):
            raise ValueError("each assigned RK182X slot needs a distinct device_id")
        for slot, group in grouped.items():
            if not group:
                continue
            card = deployment.cards[slot]
            if card.device_id not in available:
                raise ValueError(f"RK182X card {slot} device {card.device_id} is unavailable; found {sorted(available)}")
            if not card.model_manifest.is_file():
                raise ValueError(f"RK182X card {slot} model manifest is missing: {card.model_manifest}")
            specs = load_specs(card.model_manifest)
            supported = {algorithm for spec in specs.values() for algorithm in spec.algorithms} - {14, 15}
            for job in group:
                unsupported = set(job.ai_dict) - supported
                if unsupported:
                    raise ValueError(f"camera {job.camera_id} on card {slot} has unsupported algorithms {sorted(unsupported)}")
            algorithms = {algorithm for job in group for algorithm in job.ai_dict}
            models_by_slot[slot] = TRTInferCompat(
                str(card.model_manifest), args.role_manifest,
                enabled_algorithms=algorithms, device_id=card.device_id, target=card.target,
            )
            LOG.warning("RK182X card=%s device_id=%s cameras=%s", slot, card.device_id, [job.camera_id for job in group])
    except (ValueError, KeyError, RuntimeError, OSError) as exc:
        for inference in models_by_slot.values():
            inference.close()
        LOG.error("RK182X startup configuration failed: %s", exc)
        return 78

    runtime_config = RuntimeConfig.load(args.runtime_config)
    metrics_writer = JsonlMetricsWriter(args.metrics_output)
    camera_inferences = {
        job.camera_id: models_by_slot[deployment.target_for(job.camera_id)]
        for job in jobs
    }
    engine = ActiveEngine(
        jobs,
        next(iter(models_by_slot.values())),
        args.endpoint,
        args.request_timeout,
        JsonlWriter(Path(args.output)),
        runtime_config,
        metrics_writer,
        camera_inferences=camera_inferences,
        deployment=deployment,
    )
    signal.signal(signal.SIGTERM, lambda *_: engine.stop.set())
    signal.signal(signal.SIGINT, lambda *_: engine.stop.set())
    try:
        engine.run()
    finally:
        for inference in models_by_slot.values():
            inference.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
