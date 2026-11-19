"""Shared camera-to-processor routing for the two Python engines."""

from __future__ import annotations

from dataclasses import dataclass
import json
import os
from pathlib import Path
from typing import Any, Iterable, TypeVar


DEFAULT_PATH = Path("/userdata/sps_test/iva/config/camera_deployment.json")
T = TypeVar("T")


@dataclass(frozen=True)
class CardBinding:
    slot: int
    device_id: str
    target: str
    model_manifest: Path


@dataclass(frozen=True)
class CameraDeployment:
    cameras: dict[str, int]
    cards: dict[int, CardBinding]

    def target_for(self, camera_id: str) -> int:
        return self.cameras.get(str(camera_id), 0)

    def jobs_for(self, jobs: Iterable[T], target: int) -> list[T]:
        return [job for job in jobs if self.target_for(getattr(job, "camera_id")) == target]


def _no_duplicate_keys(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate camera deployment key: {key}")
        result[key] = value
    return result


def load(path: str | Path | None = None) -> CameraDeployment:
    source = Path(path or os.environ.get("DUCHUN_CAMERA_DEPLOYMENT_CONFIG", DEFAULT_PATH))
    if not source.exists():
        return CameraDeployment({}, {})
    document = json.loads(source.read_text(encoding="utf-8"), object_pairs_hook=_no_duplicate_keys)
    if not isinstance(document, dict) or type(document.get("default", 0)) is not int or document.get("default", 0) != 0:
        raise ValueError(f"camera deployment default must be 0: {source}")
    raw_cameras = document.get("cameras", {})
    raw_cards = document.get("rk182x_cards", {})
    if not isinstance(raw_cameras, dict) or not isinstance(raw_cards, dict):
        raise ValueError(f"camera deployment cameras/cards must be objects: {source}")

    cameras: dict[str, int] = {}
    for camera_id, target in raw_cameras.items():
        if not isinstance(camera_id, str) or not camera_id or type(target) is not int or target not in (1, 2):
            raise ValueError(f"invalid camera deployment target for {camera_id!r}: {target!r}")
        cameras[camera_id] = target

    cards: dict[int, CardBinding] = {}
    for key, value in raw_cards.items():
        if key not in ("1", "2") or not isinstance(value, dict):
            raise ValueError(f"invalid RK182X card slot: {key!r}")
        device_id = value.get("device_id")
        target = value.get("target")
        manifest = value.get("model_manifest")
        if not isinstance(device_id, str) or not device_id.strip():
            raise ValueError(f"card {key} has no device_id")
        if target not in ("rk1820", "rk1828"):
            raise ValueError(f"card {key} has invalid target: {target!r}")
        if not isinstance(manifest, str) or not Path(manifest).is_absolute():
            raise ValueError(f"card {key} needs an absolute model_manifest path")
        cards[int(key)] = CardBinding(int(key), device_id.strip(), target, Path(manifest))
    return CameraDeployment(cameras, cards)
