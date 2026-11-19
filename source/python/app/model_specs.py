"""Model registry for the initial RKNNLite migration scope."""

from __future__ import annotations

from dataclasses import dataclass
import json
import os
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class ModelSpec:
    role: str
    model_path: Path
    labels_path: Path
    width: int
    height: int
    anchors: tuple[tuple[tuple[float, float], ...], ...]
    strides: tuple[int, ...]
    confidence: float
    nms_iou: float
    algorithms: tuple[int, ...]
    class_names: tuple[str, ...]
    adapter: str = "yolov5"
    core_mask: int = 0x7


VALID_CORE_MASKS = frozenset({0x0, 0x1, 0x2, 0x3, 0x4, 0x7})


def _pairs(values: list[list[float]]) -> tuple[tuple[float, float], ...]:
    return tuple((float(pair[0]), float(pair[1])) for pair in values)


def load_specs(path: str | Path) -> dict[str, ModelSpec]:
    document = json.loads(Path(path).read_text(encoding="utf-8"))
    model_root = Path(os.environ.get("DUCHUN_RK3588_MODEL_ROOT", document["model_root"]))
    specs: dict[str, ModelSpec] = {}
    for value in document["models"]:
        role = str(value["role"])
        core_mask = int(str(value.get("core_mask", "0x7")), 0)
        if core_mask not in VALID_CORE_MASKS:
            raise ValueError(
                f"invalid RK3588 core_mask for role {role}: {core_mask:#x}"
            )
        specs[role] = ModelSpec(
            role=role,
            model_path=model_root / value["directory"] / value.get("model", "model.rknn"),
            labels_path=model_root / value["directory"] / value.get("labels", "labels.txt"),
            width=int(value["input"]["width"]),
            height=int(value["input"]["height"]),
            anchors=tuple(_pairs(group) for group in value.get("anchors", [])),
            strides=tuple(int(item) for item in value.get("strides", [])),
            confidence=float(value.get("confidence", 0.25)),
            nms_iou=float(value.get("nms_iou", 0.45)),
            algorithms=tuple(int(item) for item in value["algorithms"]),
            class_names=tuple(str(item) for item in value.get("class_names", [])),
            adapter=str(value.get("adapter", "yolov5")),
            core_mask=core_mask,
        )
    return specs


def result_maps(path: str | Path) -> dict[str, dict[str, dict[str, Any]]]:
    document = json.loads(Path(path).read_text(encoding="utf-8"))
    return {
        str(entry["role"]): dict(entry.get("result_map", {}))
        for entry in document["roles"]
    }
