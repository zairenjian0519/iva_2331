"""Model registry for the RK182X Python engine."""

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
    weight_path: Path
    labels_path: Path
    width: int
    height: int
    anchors: tuple[tuple[tuple[float, float], ...], ...]
    strides: tuple[int, ...]
    confidence: float
    nms_iou: float
    algorithms: tuple[int, ...]
    class_names: tuple[str, ...]
    core_mask: int
    adapter: str = "yolov5"


VALID_SINGLE_CORE_MASKS = frozenset(1 << index for index in range(8))


def _pairs(values: list[list[float]]) -> tuple[tuple[float, float], ...]:
    return tuple((float(pair[0]), float(pair[1])) for pair in values)


def load_specs(path: str | Path) -> dict[str, ModelSpec]:
    document = json.loads(Path(path).read_text(encoding="utf-8"))
    model_root = Path(os.environ.get("DUCHUN_RK182X_MODEL_ROOT", document["model_root"]))
    specs: dict[str, ModelSpec] = {}
    for value in document["models"]:
        role = str(value["role"])
        raw_core_mask = value.get("core_mask", "0x1")
        try:
            core_mask = int(str(raw_core_mask), 0)
        except (TypeError, ValueError) as exc:
            raise ValueError(
                f"invalid RK182X single-core mask for role {role}: {raw_core_mask!r}"
            ) from exc
        if core_mask not in VALID_SINGLE_CORE_MASKS:
            raise ValueError(
                f"invalid RK182X single-core mask for role {role}: {core_mask:#x}"
            )
        directory = model_root / value["directory"]
        specs[role] = ModelSpec(
            role=role,
            model_path=directory / value.get("model", "model.rknn"),
            weight_path=directory / value.get("weight", "model.weight"),
            labels_path=directory / value.get("labels", "labels.txt"),
            width=int(value["input"]["width"]),
            height=int(value["input"]["height"]),
            anchors=tuple(_pairs(group) for group in value.get("anchors", [])),
            strides=tuple(int(item) for item in value.get("strides", [])),
            confidence=float(value.get("confidence", 0.05)),
            nms_iou=float(value.get("nms_iou", 0.45)),
            algorithms=tuple(int(item) for item in value["algorithms"]),
            class_names=tuple(str(item) for item in value.get("class_names", [])),
            core_mask=core_mask,
            adapter=str(value.get("adapter", "yolov5")),
        )
    return specs


def result_maps(path: str | Path) -> dict[str, dict[str, dict[str, Any]]]:
    document = json.loads(Path(path).read_text(encoding="utf-8"))
    return {
        str(entry["role"]): dict(entry.get("result_map", {}))
        for entry in document["roles"]
    }
