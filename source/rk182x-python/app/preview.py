"""Camera preview refresh state and MQTT trigger handling."""

from __future__ import annotations

import json
import logging
import os
import threading
from typing import Any, Callable, Iterable

from paho.mqtt import client as mqtt_client


LOG = logging.getLogger("duchun.rk182x_python.preview")


class PreviewRefreshController:
    def __init__(
        self,
        camera_ids: Iterable[str],
        interval_seconds: float = 300.0,
        retry_seconds: float = 5.0,
        mqtt_host: str = "127.0.0.1",
        mqtt_port: int = 1883,
        topic: str = "web_picrefresh",
        client_factory: Callable[..., Any] = mqtt_client.Client,
    ) -> None:
        self.camera_ids = {str(value) for value in camera_ids}
        self.interval_seconds = max(1.0, float(interval_seconds))
        self.retry_seconds = max(1.0, float(retry_seconds))
        self.mqtt_host = mqtt_host
        self.mqtt_port = int(mqtt_port)
        self.topic = topic
        self.client_factory = client_factory
        self._pending = set(self.camera_ids)
        self._last_success: dict[str, float] = {}
        self._retry_after: dict[str, float] = {}
        self._lock = threading.Lock()
        self._client: Any = None

    def request(self, camera_id: str | None = None) -> None:
        with self._lock:
            if camera_id is not None and str(camera_id) in self.camera_ids:
                self._pending.add(str(camera_id))
            else:
                self._pending.update(self.camera_ids)

    def should_refresh(self, camera_id: str, now: float) -> bool:
        camera_id = str(camera_id)
        with self._lock:
            if now < self._retry_after.get(camera_id, 0.0):
                return False
            if camera_id in self._pending:
                return True
            return now - self._last_success.get(camera_id, 0.0) >= self.interval_seconds

    def mark_success(self, camera_id: str, now: float) -> None:
        camera_id = str(camera_id)
        with self._lock:
            self._pending.discard(camera_id)
            self._retry_after.pop(camera_id, None)
            self._last_success[camera_id] = now

    def mark_failure(self, camera_id: str, now: float) -> None:
        camera_id = str(camera_id)
        with self._lock:
            self._pending.add(camera_id)
            self._retry_after[camera_id] = now + self.retry_seconds

    def start(self) -> bool:
        try:
            self._client = self.client_factory(client_id=f"duchun-preview-{os.getpid()}")
            self._client.on_connect = self._on_connect
            self._client.on_message = self._on_message
            self._client.connect_async(self.mqtt_host, self.mqtt_port, 60)
            self._client.loop_start()
            return True
        except Exception:
            self._client = None
            LOG.exception("preview MQTT unavailable; periodic refresh remains active")
            return False

    def stop(self) -> None:
        client, self._client = self._client, None
        if client is None:
            return
        try:
            client.loop_stop()
            client.disconnect()
        except Exception:
            LOG.exception("failed to stop preview MQTT client")

    def _on_connect(self, client: Any, _userdata: Any, _flags: Any, result_code: int, *_args: Any) -> None:
        if result_code == 0:
            client.subscribe(self.topic, qos=1)
        else:
            LOG.warning("preview MQTT connection failed with code %s", result_code)

    def _on_message(self, _client: Any, _userdata: Any, message: Any) -> None:
        camera_id = None
        try:
            payload = json.loads(message.payload.decode("utf-8"))
            if isinstance(payload, dict) and payload.get("camera_id") is not None:
                camera_id = str(payload["camera_id"])
        except Exception:
            LOG.warning("invalid preview refresh payload on %s", self.topic)
        self.request(camera_id)
