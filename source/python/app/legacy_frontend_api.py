"""Compatibility API used by the legacy Kyscene monitoring pages.

This module deliberately owns no decoder or inference runtime. Standard camera
streams remain owned by Kyscene; only lightweight ``page<camera_id>`` aliases
are created in ZLMediaKit for the legacy sixteen-screen page.
"""

from __future__ import annotations

import configparser
from dataclasses import dataclass
import logging
import os
from pathlib import Path
import threading
import time
from typing import Any, Callable, Optional
from urllib.parse import quote

import requests
from fastapi import APIRouter, Request
from pydantic import BaseModel


LOG = logging.getLogger("duchun.rk3588_python.legacy_frontend")
router = APIRouter()


class HeartbeatRequest(BaseModel):
    camera_id: str = "page16"
    heart_num: int


class StreamRequest(BaseModel):
    rtsp_url: Optional[str] = None
    camera_id: str
    camera_mfr: str = "dh"
    sub_type: str = "sub"
    algorithm_type: int = 0


class GroupRequest(BaseModel):
    group_id: str


class StreamBackendError(RuntimeError):
    pass


@dataclass(frozen=True)
class StreamSettings:
    kyscene_base_url: str
    zlm_api_url: str
    zlm_secret: str
    zlm_rtsp_port: int
    public_host: str
    request_timeout: float

    @classmethod
    def from_environment(cls) -> "StreamSettings":
        config_path = Path(os.getenv(
            "DUCHUN_ZLM_CONFIG",
            "/userdata/sps_test/iva-kyscene/zlmediakit/config.ini",
        ))
        parser = configparser.ConfigParser(interpolation=None, strict=False)
        if not parser.read(config_path, encoding="utf-8"):
            raise StreamBackendError(f"cannot read ZLMediaKit config: {config_path}")
        try:
            secret = parser.get("api", "secret").strip()
            http_port = parser.getint("http", "port", fallback=8080)
            rtsp_port = parser.getint("rtsp", "port", fallback=554)
        except (configparser.Error, ValueError) as exc:
            raise StreamBackendError("invalid ZLMediaKit configuration") from exc
        if not secret:
            raise StreamBackendError("ZLMediaKit API secret is empty")
        zlm_host = os.getenv("DUCHUN_ZLM_HOST", "127.0.0.1")
        zlm_api_url = os.getenv(
            "DUCHUN_ZLM_API_URL",
            f"http://{zlm_host}:{http_port}",
        )
        return cls(
            kyscene_base_url=os.getenv(
                "DUCHUN_KYSCENE_BASE_URL",
                "http://127.0.0.1:8088/kyscene",
            ).rstrip("/"),
            zlm_api_url=zlm_api_url.rstrip("/"),
            zlm_secret=secret,
            zlm_rtsp_port=rtsp_port,
            public_host=os.getenv("DUCHUN_STREAM_PUBLIC_HOST", "127.0.0.1"),
            request_timeout=max(
                1.0,
                float(os.getenv("DUCHUN_STREAM_REQUEST_TIMEOUT_SECONDS", "12")),
            ),
        )


class StreamBackend:
    """Small client for the already deployed Kyscene and ZLMediaKit services."""

    def __init__(self, settings: StreamSettings, session: requests.Session | None = None) -> None:
        self.settings = settings
        self.session = session or requests.Session()

    def close(self) -> None:
        self.session.close()

    def ensure_standard_stream(self, camera_id: str) -> None:
        url = f"{self.settings.kyscene_base_url}/thirdApi/startCameraStream"
        document = self._post_json(
            url,
            {"deviceIp": self.settings.public_host, "cameraId": camera_id},
            require_zlm_code=False,
        )
        if document.get("success") is not True or int(document.get("code", 500)) != 200:
            raise StreamBackendError("Kyscene could not start the standard camera stream")
        self._wait_for_stream(camera_id)

    def alias_exists(self, alias: str) -> bool:
        return alias in self.streams()

    def page_aliases(self) -> set[str]:
        return {stream for stream in self.streams() if stream.startswith("page")}

    def streams(self) -> set[str]:
        document = self._zlm_request("/index/api/getMediaList", {})
        data = document.get("data") or []
        return {
            str(item.get("stream"))
            for item in data
            if isinstance(item, dict)
            and item.get("app") == "live"
            and item.get("stream") is not None
        }

    def create_page_alias(self, camera_id: str) -> str:
        alias = f"page{camera_id}"
        source_stream = quote(camera_id, safe="")
        source_url = (
            f"rtsp://127.0.0.1:{self.settings.zlm_rtsp_port}/live/{source_stream}"
        )
        self._zlm_request(
            "/index/api/addStreamProxy",
            {
                "vhost": "__defaultVhost__",
                "app": "live",
                "stream": alias,
                "url": source_url,
                "enable_hls": False,
                "enable_mp4": False,
            },
        )
        self._wait_for_stream(alias)
        return alias

    def close_page_alias(self, alias: str) -> bool:
        document = self._zlm_request(
            "/index/api/close_streams",
            {"vhost": "__defaultVhost__", "app": "live", "stream": alias},
        )
        return int(document.get("count_closed", 0)) > 0

    def _wait_for_stream(self, stream: str) -> None:
        deadline = time.monotonic() + self.settings.request_timeout
        while time.monotonic() < deadline:
            if stream in self.streams():
                return
            time.sleep(0.25)
        raise StreamBackendError("stream registration timed out")

    def _zlm_request(self, path: str, payload: dict[str, Any]) -> dict[str, Any]:
        document = self._post_json(
            f"{self.settings.zlm_api_url}{path}",
            {"secret": self.settings.zlm_secret, **payload},
            require_zlm_code=True,
        )
        return document

    def _post_json(
        self,
        url: str,
        payload: dict[str, Any],
        *,
        require_zlm_code: bool,
    ) -> dict[str, Any]:
        try:
            response = self.session.post(
                url,
                json=payload,
                timeout=self.settings.request_timeout,
            )
            response.raise_for_status()
            document = response.json()
        except (requests.RequestException, ValueError) as exc:
            raise StreamBackendError("stream service request failed") from exc
        if not isinstance(document, dict):
            raise StreamBackendError("stream service returned an invalid response")
        if require_zlm_code and int(document.get("code", -1)) != 0:
            raise StreamBackendError("ZLMediaKit rejected the stream operation")
        return document


class ScreenSessionState:
    def __init__(self, timeout_seconds: float, clock: Callable[[], float] = time.monotonic) -> None:
        self.timeout_seconds = max(1.0, float(timeout_seconds))
        self._clock = clock
        self._lock = threading.RLock()
        self._last_seen: float | None = None
        self._heart_num = 0
        self._aliases: set[str] = set()

    def claim(self) -> tuple[bool, set[str]]:
        now = self._clock()
        with self._lock:
            expired = self._expire_locked(now)
            if self._last_seen is not None:
                return False, expired
            self._last_seen = now
            self._heart_num = 0
            return True, expired

    def heartbeat(self, heart_num: int) -> None:
        with self._lock:
            self._last_seen = self._clock()
            self._heart_num = heart_num

    def add_alias(self, alias: str) -> None:
        with self._lock:
            if self._last_seen is None:
                self._last_seen = self._clock()
            self._aliases.add(alias)

    def remove_alias(self, alias: str) -> None:
        with self._lock:
            self._aliases.discard(alias)

    def adopt_aliases(self, aliases: set[str]) -> None:
        if not aliases:
            return
        with self._lock:
            self._aliases.update(aliases)
            if self._last_seen is None:
                self._last_seen = self._clock()

    def collect_expired_aliases(self) -> set[str]:
        with self._lock:
            return self._expire_locked(self._clock())

    def all_aliases(self) -> set[str]:
        with self._lock:
            return set(self._aliases)

    def _expire_locked(self, now: float) -> set[str]:
        if self._last_seen is None or now - self._last_seen <= self.timeout_seconds:
            return set()
        aliases = set(self._aliases)
        self._aliases.clear()
        self._last_seen = None
        self._heart_num = 0
        return aliases


class LegacyFrontendManager:
    def __init__(
        self,
        backend: StreamBackend | Any,
        timeout_seconds: float,
        cleanup_interval: float = 1.0,
    ) -> None:
        self.backend = backend
        self.state = ScreenSessionState(timeout_seconds)
        self.cleanup_interval = max(0.1, cleanup_interval)
        self._stream_lock = threading.RLock()
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None

    @classmethod
    def from_environment(cls) -> "LegacyFrontendManager":
        timeout = float(os.getenv("DUCHUN_SCREEN_HEARTBEAT_TIMEOUT_SECONDS", "5"))
        return cls(StreamBackend(StreamSettings.from_environment()), timeout)

    def start(self) -> None:
        try:
            self.state.adopt_aliases(self.backend.page_aliases())
        except Exception as exc:
            self._log_backend_failure("discover aliases", exc)
        self._thread = threading.Thread(
            target=self._cleanup_loop,
            name="legacy-screen-cleanup",
            daemon=True,
        )
        self._thread.start()

    def stop(self) -> None:
        self._stop.set()
        if self._thread is not None:
            self._thread.join(timeout=max(2.0, self.cleanup_interval * 2))
        self._close_aliases(self.state.all_aliases())
        try:
            self.backend.close()
        except Exception:
            LOG.warning("failed to close legacy stream client", exc_info=True)

    def claim_screen(self) -> dict[str, Any]:
        claimed, expired = self.state.claim()
        self._close_aliases(expired)
        if not claimed:
            return {"code": 2005, "msg": "实时监控页面已存在，请勿重复打开"}
        return {"code": 2000, "msg": ""}

    def heartbeat(self, request: HeartbeatRequest) -> dict[str, Any]:
        if request.heart_num <= 0:
            return {"code": 2001, "msg": "heart_num is illegal"}
        self.state.heartbeat(request.heart_num)
        return {"code": 2000, "msg": "receive success"}

    def start_screen(self, camera_id: str) -> dict[str, Any]:
        alias = f"page{camera_id}"
        try:
            with self._stream_lock:
                if self.backend.alias_exists(alias):
                    self.state.add_alias(alias)
                    return {"code": 2006, "msg": f"{camera_id}分屏视频流已在播放中"}
                self.backend.ensure_standard_stream(camera_id)
                created_alias = self.backend.create_page_alias(camera_id)
                self.state.add_alias(created_alias)
            LOG.info("started legacy screen alias camera=%s alias=%s", camera_id, alias)
            return {
                "code": 2000,
                "msg": f"webrtc://127.0.0.1/live/{alias}",
            }
        except Exception as exc:
            self._log_backend_failure("start screen", exc, camera_id)
            return {"code": 2003, "msg": "设备流媒体服务不可用，无法启动实时监控服务"}

    def stop_screen(self, camera_id: str) -> dict[str, Any]:
        alias = f"page{camera_id}"
        try:
            with self._stream_lock:
                if not self.backend.alias_exists(alias):
                    self.state.remove_alias(alias)
                    return {"code": 2007, "msg": f"{camera_id}分屏视频流不存在"}
                self.backend.close_page_alias(alias)
                self.state.remove_alias(alias)
            LOG.info("stopped legacy screen alias camera=%s alias=%s", camera_id, alias)
            return {"code": 2000, "msg": f"{camera_id}分屏视频流已关闭"}
        except Exception as exc:
            self._log_backend_failure("stop screen", exc, camera_id)
            return {"code": 2003, "msg": "设备流媒体服务不可用，无法关闭实时监控流"}

    def _cleanup_loop(self) -> None:
        while not self._stop.wait(self.cleanup_interval):
            self._close_aliases(self.state.collect_expired_aliases())

    def _close_aliases(self, aliases: set[str]) -> None:
        for alias in aliases:
            try:
                with self._stream_lock:
                    self.backend.close_page_alias(alias)
                LOG.info("closed expired legacy screen alias=%s", alias)
            except Exception as exc:
                self._log_backend_failure("close expired alias", exc)

    @staticmethod
    def _log_backend_failure(operation: str, error: Exception, camera_id: str | None = None) -> None:
        LOG.warning(
            "legacy stream operation failed operation=%s camera=%s error_type=%s",
            operation,
            camera_id or "-",
            type(error).__name__,
        )


def enabled_from_environment() -> bool:
    return os.getenv("DUCHUN_LEGACY_FRONTEND_API_ENABLED", "1").strip().lower() in {
        "1", "true", "yes", "on",
    }


def _manager(request: Request) -> LegacyFrontendManager | None:
    return getattr(request.app.state, "legacy_frontend_manager", None)


@router.post("/heartbeat")
def heartbeat(payload: HeartbeatRequest, request: Request) -> dict[str, Any]:
    manager = _manager(request)
    if manager is None:
        return {"code": 2003, "msg": "实时监控兼容服务未初始化"}
    return manager.heartbeat(payload)


@router.get("/firstscreen")
def firstscreen(request: Request) -> dict[str, Any]:
    manager = _manager(request)
    if manager is None:
        return {"code": 2003, "msg": "实时监控兼容服务未初始化"}
    return manager.claim_screen()


@router.post("/startscreen")
def startscreen(payload: StreamRequest, request: Request) -> dict[str, Any]:
    manager = _manager(request)
    if manager is None:
        return {"code": 2003, "msg": "实时监控兼容服务未初始化"}
    return manager.start_screen(payload.camera_id)


@router.post("/stopscreen")
def stopscreen(payload: StreamRequest, request: Request) -> dict[str, Any]:
    manager = _manager(request)
    if manager is None:
        return {"code": 2003, "msg": "实时监控兼容服务未初始化"}
    return manager.stop_screen(payload.camera_id)


@router.post("/addjob")
def addjob(_: GroupRequest) -> dict[str, Any]:
    return {"code": 2016, "msg": "RK3588 Python轮询推理暂未启用"}


@router.post("/removejob")
def removejob(_: GroupRequest) -> dict[str, Any]:
    return {"code": 2016, "msg": "RK3588 Python轮询推理暂未启用"}
