"""Read the production Redis payloads without changing their wire format."""

from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any, Mapping

from iva_utils import camera2task


@dataclass(frozen=True)
class CameraJob:
    camera_id: str
    camera_ip: str
    rtsp_url: str
    codec: str
    width: int
    height: int
    ai_dict: dict[int, dict[str, Any]]
    rois_dict: dict[str, dict[str, Any]]
    raw_algorithm_configs: tuple[dict[str, Any], ...]

    def geometry(self, width: int, height: int) -> tuple[dict[int, dict[str, Any]], dict[str, dict[str, Any]]]:
        """Materialize normalized Redis ROIs for the decoded frame size."""
        return _build_algorithm_maps(self.raw_algorithm_configs, width, height)


def decode_double_json(value: bytes | str | None) -> Any:
    if value is None:
        return None
    first = json.loads(value.decode("utf-8") if isinstance(value, bytes) else value)
    return json.loads(first) if isinstance(first, str) else first


def build_jobs(camera_payload: bytes | str | None, algorithm_payload: bytes | str | None) -> list[CameraJob]:
    cameras = decode_double_json(camera_payload)
    tasks = decode_double_json(algorithm_payload)
    if not cameras or not tasks:
        return []
    camera_index = camera2task(cameras)
    jobs: list[CameraJob] = []
    for task in tasks:
        camera_id = str(task.get("camera_id"))
        camera = camera_index.get(task.get("camera_id"))
        configs = task.get("algorithm_cfg") or []
        if not camera or not configs:
            continue
        width, height = int(camera.get("width") or 0), int(camera.get("height") or 0)
        geometry_width, geometry_height = max(width, 1), max(height, 1)
        ai_dict, rois_dict = _build_algorithm_maps(configs, geometry_width, geometry_height)
        jobs.append(CameraJob(
            camera_id, str(camera.get("ip", "")), str(camera["rtsp_url"]),
            str(camera.get("codec", "auto")).lower(), width, height, ai_dict,
            rois_dict, tuple(dict(item) for item in configs),
        ))
    return jobs


def _build_algorithm_maps(
    configs: list[dict[str, Any]] | tuple[dict[str, Any], ...], width: int, height: int
) -> tuple[dict[int, dict[str, Any]], dict[str, dict[str, Any]]]:
    if width <= 0 or height <= 0:
        raise ValueError("frame dimensions must be positive")
    ai_dict: dict[int, dict[str, Any]] = {}
    rois_dict: dict[str, dict[str, Any]] = {}
    for item in configs:
        algorithm = int(item["operator"][0])
        invalid = _scale_polygons(item.get("invalid_roi", []), width, height)
        roi = _scale_polygons(item.get("camera_roi", []), width, height)
        ai_dict[algorithm] = {
            "algorithm_threshold": float(item.get("algorithm_threshold", 0)) / 100.0,
            "alarm_grade": item.get("alarm_grade", "0"),
            "alarm_Interval_period": float(item.get("alarm_Interval_period", 0)),
            "target_threshold": int(item.get("target_threshold", 0)),
            "hit_count": int(item.get("hit_count", 0)) * 25,
            "alarm_period": int(item.get("alarm_period", 0)),
            "sigma_iou": float(item.get("iou_threshold", 0)) * 0.01,
            "invalid_rois": invalid,
            "valid_time": item.get("valid_time", {}),
        }
        rois_dict[str(algorithm)] = {"camera_roi": roi, "invalid_roi": invalid}
    return ai_dict, rois_dict


def _scale_polygons(values: list[Any], width: int, height: int) -> list[list[list[int]]]:
    output: list[list[list[int]]] = []
    for polygon in values:
        points = polygon[:-1] if polygon else []
        if points:
            output.append([[int(point[0] * width), int(point[1] * height)] for point in points])
    return output
