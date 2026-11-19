"""Thread-safe, per-camera inference metrics for the active engine."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
import json
import logging
from logging.handlers import RotatingFileHandler
import math
from pathlib import Path
import threading
import time
from typing import Any, Mapping


STREAM_COUNTERS = (
    "packets",
    "packet_bytes",
    "decoded_frames",
    "delivered_frames",
    "consumed_frames",
    "dropped_frames",
    "errors",
    "reconnects",
)


@dataclass(frozen=True)
class RuntimeConfig:
    metrics_interval_seconds: float = 10.0
    default_max_inference_fps: float = 0.0
    camera_max_inference_fps: dict[str, float] = field(default_factory=dict)

    @classmethod
    def load(cls, path: str | Path) -> "RuntimeConfig":
        source = Path(path)
        if not source.exists():
            return cls()
        document = json.loads(source.read_text(encoding="utf-8"))
        interval = max(1.0, float(document.get("metrics_interval_seconds", 10.0)))
        default_fps = max(0.0, float(document.get("default_max_inference_fps", 0.0)))
        overrides = {
            str(camera_id): max(0.0, float(value))
            for camera_id, value in document.get("camera_max_inference_fps", {}).items()
        }
        return cls(interval, default_fps, overrides)

    def max_fps(self, camera_id: str) -> float:
        return self.camera_max_inference_fps.get(str(camera_id), self.default_max_inference_fps)


class JsonlMetricsWriter:
    def __init__(self, path: str | Path, max_bytes: int = 20 * 1024 * 1024, backups: int = 5) -> None:
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        self._logger = logging.getLogger(f"duchun.rk3588.metrics.{id(self)}")
        self._logger.setLevel(logging.INFO)
        self._logger.propagate = False
        handler = RotatingFileHandler(
            target,
            maxBytes=max_bytes,
            backupCount=backups,
            encoding="utf-8",
        )
        handler.setFormatter(logging.Formatter("%(message)s"))
        self._logger.addHandler(handler)
        self._handler = handler

    def write(self, value: Mapping[str, Any]) -> None:
        self._logger.info(json.dumps(dict(value), ensure_ascii=False, separators=(",", ":")))

    def close(self) -> None:
        self._logger.removeHandler(self._handler)
        self._handler.close()


def _percentile(values: list[float], percentile: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, math.ceil(percentile * len(ordered)) - 1))
    return ordered[index]


class CameraMetricsWindow:
    def __init__(self, camera_id: str, stream_id: int, interval_seconds: float) -> None:
        self.camera_id = str(camera_id)
        self.stream_id = int(stream_id)
        self.interval_seconds = float(interval_seconds)
        self.started = time.monotonic()
        self.previous_stream: dict[str, int] | None = None
        self.inference_frames = 0
        self.model_latencies: dict[str, list[float]] = {}
        self.model_errors: dict[str, int] = {}
        self.alarm_jpeg_frames = 0
        self.preview_jpeg_frames = 0
        self.http_success = 0
        self.http_errors = 0

    def record_model(self, role: str, elapsed_ms: float) -> None:
        self.model_latencies.setdefault(str(role), []).append(float(elapsed_ms))

    def record_model_error(self, role: str, count: int = 1) -> None:
        name = str(role)
        self.model_errors[name] = self.model_errors.get(name, 0) + max(0, int(count))

    def record_inference(self) -> None:
        self.inference_frames += 1

    def record_alarm_jpeg(self) -> None:
        self.alarm_jpeg_frames += 1

    def record_preview_jpeg(self) -> None:
        self.preview_jpeg_frames += 1

    def record_http(self, success: bool) -> None:
        if success:
            self.http_success += 1
        else:
            self.http_errors += 1

    def maybe_snapshot(
        self,
        stream_stats: Any,
        enabled_algorithms: Mapping[int, Any],
        now: float | None = None,
    ) -> dict[str, Any] | None:
        current_time = time.monotonic() if now is None else float(now)
        elapsed = current_time - self.started
        if elapsed < self.interval_seconds:
            return None

        current_stream = {
            name: int(getattr(stream_stats, name, 0))
            for name in STREAM_COUNTERS
        }
        previous = self.previous_stream or {name: 0 for name in STREAM_COUNTERS}
        delta = {name: max(0, current_stream[name] - previous[name]) for name in STREAM_COUNTERS}
        model_calls = {role: len(values) for role, values in self.model_latencies.items()}
        model_avg = {
            role: round(sum(values) / len(values), 3)
            for role, values in self.model_latencies.items()
            if values
        }
        model_p95 = {
            role: round(_percentile(values, 0.95), 3)
            for role, values in self.model_latencies.items()
            if values
        }
        record = {
            "timestamp": datetime.now().astimezone().isoformat(timespec="seconds"),
            "camera_id": self.camera_id,
            "stream_id": self.stream_id,
            "window_seconds": round(elapsed, 3),
            "rtsp_packets": current_stream["packets"],
            "rtsp_packet_fps": round(delta["packets"] / elapsed, 3),
            "rtsp_mbps": round(delta["packet_bytes"] * 8.0 / elapsed / 1_000_000.0, 3),
            "decoded_frames": current_stream["decoded_frames"],
            "decode_fps": round(delta["decoded_frames"] / elapsed, 3),
            "consumed_frames": current_stream["consumed_frames"],
            "consume_fps": round(delta["consumed_frames"] / elapsed, 3),
            "dropped_frames": current_stream["dropped_frames"],
            "drop_fps": round(delta["dropped_frames"] / elapsed, 3),
            "inference_frames": self.inference_frames,
            "inference_fps": round(self.inference_frames / elapsed, 3),
            "per_model_calls": model_calls,
            "per_model_avg_ms": model_avg,
            "per_model_p95_ms": model_p95,
            "roi_preprocess_errors": sum(self.model_errors.values()),
            "per_model_errors": dict(sorted(self.model_errors.items())),
            "alarm_jpeg_frames": self.alarm_jpeg_frames,
            "alarm_jpeg_fps": round(self.alarm_jpeg_frames / elapsed, 3),
            "preview_jpeg_frames": self.preview_jpeg_frames,
            "http_success": self.http_success,
            "http_errors": self.http_errors,
            "enabled_algorithms": sorted(int(value) for value in enabled_algorithms),
            "stream_state": str(getattr(stream_stats, "state", "")),
            "stream_errors": current_stream["errors"],
            "reconnects": current_stream["reconnects"],
            "last_error": str(getattr(stream_stats, "last_error", "")),
        }
        self.previous_stream = current_stream
        self.started = current_time
        self.inference_frames = 0
        self.model_latencies.clear()
        self.model_errors.clear()
        self.alarm_jpeg_frames = 0
        self.preview_jpeg_frames = 0
        self.http_success = 0
        self.http_errors = 0
        return record
