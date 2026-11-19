"""Single-camera rules aligned with main_lynxi.TRTInfer.infer.

Native code owns model execution. This module defines ROI geometry and converts
semantic RKNN detections (all boxes in original-frame coordinates) to bg_task.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping

from iva_utils import invalid_region, split_result


ROI_ROLE_ALGORITHMS = {
    "smoking_phone": {0, 1}, "sleep": {15},
    "reflective_clothes": {6, 7}, "blue_uniform": {27, 28},
    "shoes_gloves_glasses": {19, 20, 21},
}
CLASSIFIER_ROI_ROLE_ALGORITHMS = {"mask": {8, 9}}
FULL_FRAME_SPECIAL_ROLES = {"oil_leak", "uniform", "pipe_oil_leak"}
SPECIAL_ROLES = set(ROI_ROLE_ALGORITHMS) | FULL_FRAME_SPECIAL_ROLES | {"person_helmet", "safety_belt"}

# TRTInfer.total_min_hw: head and helmet use the same primary-model filter.
PRIMARY_MIN_HW = {4: (0.055, 0.031), 5: (0.055, 0.031)}
REFLECTIVE_MIN_HW = (0.175, 0.104)
REFLECTIVE_POSITIVE = {"reflective", "refleactive"}
REFLECTIVE_NEGATIVE = {"no_reflective", "no_refleactive"}
REFLECTIVE_CLASSES = REFLECTIVE_POSITIVE | REFLECTIVE_NEGATIVE | {"uncertain"}
SAFETY_CLASSES = {"safety", "uncertain_safety", "no_safety"}
UNIFORM_PERSON_NAMES = {"person", "worker", "people", "no_uniform", "no_workwear", "unclothed", "0"}
UNIFORM_CLOTHES_NAMES = {"uniform", "workwear", "workclothes", "work_clothes", "clothes", "1"}


@dataclass(frozen=True)
class RoiTask:
    role: str
    parent_bbox: dict[str, int]
    roi_bbox: dict[str, int]
    algorithms: tuple[int, ...]


def _box(det: Mapping[str, Any]) -> tuple[int, int, int, int]:
    value = det.get("bbox") or det.get("parent_bbox") or {}
    return int(value.get("x", 0)), int(value.get("y", 0)), int(value.get("width", 0)), int(value.get("height", 0))


def _bbox(x: int, y: int, width: int, height: int) -> dict[str, int]:
    return {"x": x, "y": y, "width": width, "height": height}


def _even(value: int) -> int:
    return value if value % 2 == 0 else value + 1


def _clip_roi(x1: int, y1: int, x2: int, y2: int, frame_width: int, frame_height: int) -> dict[str, int] | None:
    left, top = _even(max(0, min(x1, frame_width))), _even(max(0, min(y1, frame_height)))
    right, bottom = _even(max(left, min(x2, frame_width))), _even(max(top, min(y2, frame_height)))
    return _bbox(left, top, right - left, bottom - top) if right - left > 7 and bottom - top > 7 else None


def _contains(parent: Mapping[str, Any], child: Mapping[str, Any]) -> bool:
    x, y, width, height = _box(parent)
    cx, cy, cw, ch = _box(child)
    return x <= cx + cw / 2 <= x + width and y <= cy + ch / 2 <= y + height


def _associated(person: Mapping[str, Any], det: Mapping[str, Any]) -> bool:
    parent = det.get("parent_bbox")
    return _box(person) == _box({"bbox": parent}) if isinstance(parent, Mapping) else _contains(person, det)


def is_valid_person(person: Mapping[str, Any], frame_height: int, frame_width: int) -> bool:
    _, _, width, height = _box(person)
    return width >= 0.092 * frame_width and height >= 0.052 * frame_height


def build_roi_tasks(main_detections: list[Mapping[str, Any]], configs: Mapping[int, Mapping[str, Any]],
                    frame_height: int, frame_width: int) -> list[RoiTask]:
    """Exact main_lynxi person ROI geometry; no model inference is performed."""
    tasks: list[RoiTask] = []
    smoking_algorithms = tuple(
        sorted(algorithm for algorithm in ROI_ROLE_ALGORITHMS["smoking_phone"] if algorithm in configs)
    )
    if smoking_algorithms:
        for detection in main_detections:
            if str(detection.get("class_name")) == "person":
                continue
            left, top, width, height = _box(detection)
            parent = _bbox(left, top, width, height)
            roi = _clip_roi(
                int(left - width / 2),
                top,
                int(left + width + width / 2),
                int(top + height + height * 0.7),
                frame_width,
                frame_height,
            )
            if roi:
                tasks.append(RoiTask("smoking_phone", parent, roi, smoking_algorithms))

    mask_algorithms = tuple(
        sorted(
            algorithm
            for algorithm in CLASSIFIER_ROI_ROLE_ALGORITHMS["mask"]
            if algorithm in configs
        )
    )
    if mask_algorithms:
        for detection in main_detections:
            if str(detection.get("class_name")) != "head":
                continue
            left, top, width, height = _box(detection)
            parent = _bbox(left, top, width, height)
            roi = _clip_roi(left, top, left + width, top + height, frame_width, frame_height)
            if roi:
                tasks.append(RoiTask("mask", parent, roi, mask_algorithms))

    for person in main_detections:
        if str(person.get("class_name")) != "person" or not is_valid_person(person, frame_height, frame_width):
            continue
        left, top, width, height = _box(person)
        right, bottom = left + width, top + height
        parent = _bbox(left, top, width, height)
        enabled = lambda role: tuple(sorted(algorithm for algorithm in ROI_ROLE_ALGORITHMS[role] if algorithm in configs))

        roi = _clip_roi(left, top, right, int(top + height * 0.7), frame_width, frame_height)
        if enabled("sleep") and width / max(height, 1) >= 0.5 and roi: tasks.append(RoiTask("sleep", parent, roi, enabled("sleep")))
        roi = _clip_roi(left - 10, top, right + 10, bottom, frame_width, frame_height)
        if enabled("reflective_clothes") and roi: tasks.append(RoiTask("reflective_clothes", parent, roi, enabled("reflective_clothes")))
        if enabled("blue_uniform"): tasks.append(RoiTask("blue_uniform", parent, parent, enabled("blue_uniform")))
        roi = _clip_roi(int(left - width / 2), top, int(right + width / 2), int(bottom + height / 2), frame_width, frame_height)
        if enabled("shoes_gloves_glasses") and roi: tasks.append(RoiTask("shoes_gloves_glasses", parent, roi, enabled("shoes_gloves_glasses")))
    return tasks


def _alarm(algorithm: int, det: Mapping[str, Any], configs: Mapping[int, Mapping[str, Any]],
           bbox: Mapping[str, int] | None = None,
           minimum_hw: tuple[float, float] | None = None,
           frame_height: int | None = None, frame_width: int | None = None) -> list[Any] | None:
    cfg = configs.get(algorithm)
    if not cfg:
        return None
    score = float(det.get("score", 0))
    threshold = float(cfg.get("algorithm_threshold", 0))
    # Redis stores many legacy thresholds as percentages (for example 75.0),
    # while RKNN postprocess scores are normalized to the [0, 1] range.
    if threshold > 1:
        threshold = min(threshold / 100.0, 1.0)
    x, y, width, height = _box({"bbox": bbox} if bbox else det)
    if width <= 0 or height <= 0 or score <= threshold:
        return None
    if minimum_hw and frame_height and frame_width:
        min_width, min_height = minimum_hw
        if width < min_width * frame_width and height < min_height * frame_height:
            return None
    invalids = cfg.get("invalid_rois", [])
    if invalids and not invalid_region(invalids, x, y, x + width, y + height):
        return None
    return [algorithm, round(score, 4), x, y, width, height, cfg.get("alarm_grade", "0"),
            float(cfg.get("alarm_Interval_period", 0)), int(cfg.get("target_threshold", 0))]


def _best(event: Any, person: Mapping[str, Any], names: set[str], min_area: int = 0) -> Mapping[str, Any] | None:
    values = [det for det in event.detections if str(det.get("class_name")) in names and _associated(person, det)]
    if min_area: values = [det for det in values if _box(det)[2] * _box(det)[3] >= min_area]
    return max(values, key=lambda det: float(det.get("score", 0)), default=None)


def _best_with_area_fallback(event: Any, person: Mapping[str, Any], names: set[str],
                             minimum_area_ratio: float) -> Mapping[str, Any] | None:
    values = [det for det in event.detections
              if str(det.get("class_name")) in names and _associated(person, det)]
    valid: list[Mapping[str, Any]] = []
    for det in values:
        roi = det.get("roi_bbox")
        _, _, roi_width, roi_height = _box({"bbox": roi}) if isinstance(roi, Mapping) else _box(person)
        _, _, width, height = _box(det)
        if width * height >= int(roi_width * roi_height * minimum_area_ratio):
            valid.append(det)
    return max(valid or values, key=lambda det: float(det.get("score", 0)), default=None)


def _uniform_kind(det: Mapping[str, Any]) -> str | None:
    """The reference two-class model uses class 0=person and class 1=workwear."""
    if det.get("class_id") is not None:
        class_id = int(det["class_id"])
        if class_id == 0:
            return "person"
        if class_id == 1:
            return "workwear"
        return None
    name = str(det.get("class_name", "")).strip().lower()
    if name in UNIFORM_PERSON_NAMES:
        return "person"
    if name in UNIFORM_CLOTHES_NAMES:
        return "workwear"
    return None


def evaluate(results: Mapping[str, Any], configs: Mapping[int, Mapping[str, Any]],
             result_maps: Mapping[str, Mapping[str, Mapping[str, Any]]],
             frame_height: int | None = None, frame_width: int | None = None) -> tuple[list[list[Any]], list[list[list[Any]]]]:
    main = results.get("person_helmet")
    persons = [det for det in (main.detections if main else []) if str(det.get("class_name")) == "person"]
    if frame_height and frame_width: persons = [det for det in persons if is_valid_person(det, frame_height, frame_width)]
    person_boxes = [[0, round(float(det.get("score", 0)), 4), *_box(det)] for det in persons]
    alarms: list[list[Any]] = []

    # The main model owns person/helmet semantics. Persons feed bg_task and
    # helmet classes remain direct alarms exactly as in main_lynxi.infer.
    if main:
        for det in main.detections:
            if str(det.get("class_name")) == "person":
                continue
            rule = result_maps.get("person_helmet", {}).get(str(det.get("class_name", "")), {})
            if rule.get("kind") == "alarm":
                algorithm = int(rule["algorithm"])
                value = _alarm(algorithm, det, configs,
                               minimum_hw=PRIMARY_MIN_HW.get(algorithm),
                               frame_height=frame_height, frame_width=frame_width)
                if value:
                    alarms.append(value)

    for role, event in results.items():
        if role in SPECIAL_ROLES: continue
        for det in event.detections:
            rule = result_maps.get(role, {}).get(str(det.get("class_name", "")), {})
            if rule.get("kind") == "alarm":
                value = _alarm(int(rule["algorithm"]), det, configs)
                if value: alarms.append(value)

    smoke = results.get("smoking_phone")
    if smoke:
        grouped: dict[tuple[int, int, int, int], list[Mapping[str, Any]]] = {}
        for detection in smoke.detections:
            parent = detection.get("parent_bbox")
            if not isinstance(parent, Mapping):
                continue
            grouped.setdefault(_box({"bbox": parent}), []).append(detection)
        for local in grouped.values():
            names = {str(det.get("class_name", "")) for det in local}
            if "hand" not in names: continue
            for name, algorithm in (("smoking", 0), ("phone", 1)):
                source = next((det for det in local if str(det.get("class_name")) == name), None)
                if source:
                    value = _alarm(
                        algorithm,
                        source,
                        configs,
                        source.get("roi_bbox") or source.get("parent_bbox"),
                    )
                    if value: alarms.append(value)

    sleep = results.get("sleep")
    if sleep:
        for person in persons:
            left, top, width, height = _box(person)
            source = _best(sleep, person, {"sleep"}) if width / max(height, 1) >= 0.5 else None
            if source:
                value = _alarm(15, source, configs, _bbox(left, top, width, _even(int(height * 0.7))))
                if value: alarms.append(value)

    reflective = results.get("reflective_clothes")
    if reflective:
        for person in persons:
            source = _best_with_area_fallback(reflective, person, REFLECTIVE_CLASSES, 0.1)
            if source:
                name = str(source.get("class_name"))
                if name not in REFLECTIVE_POSITIVE | REFLECTIVE_NEGATIVE:
                    continue
                normalized = dict(source)
                normalized["score"] = round(float(source.get("score", 0)), 2)
                algorithm = 7 if name in REFLECTIVE_POSITIVE else 6
                value = _alarm(algorithm, normalized, configs, _bbox(*_box(person)),
                               minimum_hw=REFLECTIVE_MIN_HW,
                               frame_height=frame_height, frame_width=frame_width)
                if value: alarms.append(value)

    for role, algorithm in (("oil_leak", 29), ("pipe_oil_leak", 31)):
        event = results.get(role)
        if event:
            for det in event.detections:
                value = _alarm(algorithm, det, configs)
                if value:
                    alarms.append(value)

    uniform = results.get("uniform")
    if uniform:
        workers = [det for det in uniform.detections if _uniform_kind(det) == "person"]
        workwear_centers: list[tuple[float, float]] = []
        for det in uniform.detections:
            if _uniform_kind(det) != "workwear":
                continue
            x, y, width, height = _box(det)
            if width > 0 and height > 0:
                workwear_centers.append((x + width / 2, y + height / 2))
        for worker in workers:
            x, y, width, height = _box(worker)
            clothed = any(x <= center_x <= x + width and y <= center_y <= y + height
                          for center_x, center_y in workwear_centers)
            if not clothed:
                value = _alarm(30, worker, configs)
                if value:
                    alarms.append(value)

    blue = results.get("blue_uniform")
    if blue:
        for person in persons:
            source = _best(blue, person, {"blue_uniform", "no_blue_uniform"})
            if source:
                value = _alarm(27 if str(source.get("class_name")) == "blue_uniform" else 28, source, configs, _bbox(*_box(person)))
                if value: alarms.append(value)

    ppe = results.get("shoes_gloves_glasses")
    if ppe:
        for person in persons:
            for name, algorithm in {"shoes": 19, "glove": 20, "glass": 21}.items():
                source = _best(ppe, person, {name})
                if source:
                    value = _alarm(algorithm, source, configs, source.get("roi_bbox") or _bbox(*_box(person)))
                    if value: alarms.append(value)

    safety = results.get("safety_belt")
    if safety:
        for person in persons:
            source = _best(safety, person, SAFETY_CLASSES)
            if source is None:
                continue
            name = str(source.get("class_name", ""))
            if name == "uncertain_safety":
                continue
            algorithm = 3 if name == "safety" else 16
            value = _alarm(algorithm, source, configs, _bbox(*_box(person)))
            if value:
                alarms.append(value)

    dedup = {(box[0], box[2], box[3], box[4], box[5]): box for box in alarms}
    return person_boxes, split_result(list(dedup.values()), [])
