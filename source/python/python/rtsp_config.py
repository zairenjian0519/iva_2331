"""JSON configuration loader for RTSP credentials and MPP pipeline settings."""

from __future__ import annotations

import json
from pathlib import Path
from urllib.parse import quote, urlsplit, urlunsplit

from rk_mpp_pipeline import CameraConfig


def _url_with_credentials(url: str, username: str | None, password: str | None) -> str:
    if not username:
        return url
    parsed = urlsplit(url)
    if parsed.scheme != "rtsp" or not parsed.hostname:
        raise ValueError("rtsp_url must be an rtsp:// URL when username/password are configured separately")
    if parsed.username is not None:
        return url
    credentials = quote(username, safe="")
    if password is not None:
        credentials += ":" + quote(password, safe="")
    host = parsed.hostname
    if ":" in host and not host.startswith("["):
        host = f"[{host}]"
    if parsed.port is not None:
        host += f":{parsed.port}"
    return urlunsplit((parsed.scheme, f"{credentials}@{host}", parsed.path, parsed.query, parsed.fragment))


def load_camera_configs(path: str | Path) -> list[CameraConfig]:
    source = Path(path)
    document = json.loads(source.read_text(encoding="utf-8"))
    cameras = document.get("cameras")
    if not isinstance(cameras, list) or not cameras:
        raise ValueError("configuration must contain a non-empty cameras array")
    configs: list[CameraConfig] = []
    camera_ids: set[int] = set()
    for item in cameras:
        if not isinstance(item, dict):
            raise ValueError("each camera configuration must be an object")
        camera_id = int(item["camera_id"])
        if camera_id in camera_ids:
            raise ValueError(f"duplicate camera_id {camera_id}")
        camera_ids.add(camera_id)
        config = CameraConfig()
        config.camera_id = camera_id
        config.rtsp_url = _url_with_credentials(
            str(item["rtsp_url"]), item.get("username"), item.get("password")
        )
        config.codec = str(item.get("codec", "auto")).lower()
        if config.codec not in {"auto", "h264", "h265", "hevc"}:
            raise ValueError(f"camera {camera_id} has unsupported codec {config.codec}")
        config.reconnect_delay_ms = int(item.get("reconnect_delay_ms", 5000))
        config.timeout_ms = int(item.get("timeout_ms", 5000))
        configs.append(config)
    return configs
