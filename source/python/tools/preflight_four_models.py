#!/usr/bin/env python3
"""Load and exercise the four migrated RKNN models on an RK3588 board."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from types import SimpleNamespace

import cv2
import numpy as np

from app.classifier_lite import RknnLiteClassifier
from app.model_specs import load_specs
from app.yolo_lite import RknnLiteYoloV5


DETECTOR_ROLES = ("fire_smoke", "vehicle", "bolt")


def letterbox(image: np.ndarray, width: int, height: int) -> SimpleNamespace:
    source_height, source_width = image.shape[:2]
    scale = min(width / source_width, height / source_height)
    resized_width = int(round(source_width * scale))
    resized_height = int(round(source_height * scale))
    resized = cv2.resize(image, (resized_width, resized_height), interpolation=cv2.INTER_LINEAR)
    left = (width - resized_width) // 2
    top = (height - resized_height) // 2
    output = np.full((height, width, 3), 114, dtype=np.uint8)
    output[top:top + resized_height, left:left + resized_width] = resized
    return SimpleNamespace(
        rgb=np.ascontiguousarray(output[:, :, ::-1]),
        width=source_width,
        height=source_height,
        letterbox=SimpleNamespace(scale=scale, pad_left=left, pad_top=top),
    )


def tensor_dims(attr: object) -> list[int]:
    count = int(getattr(attr, "n_dims", 0))
    return [int(value) for value in getattr(attr, "dims", ())[:count]]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", default="models/initial_models.json")
    parser.add_argument(
        "--image",
        default="/userdata/sps_test/iva/weights/rknn/weight_512/"
        "head_person_helmet_10s_all_512/bus.jpg",
    )
    args = parser.parse_args()
    specs = load_specs(args.manifest)
    source = cv2.imread(str(Path(args.image)), cv2.IMREAD_COLOR)
    if source is None:
        raise RuntimeError(f"cannot read image: {args.image}")

    for role in DETECTOR_ROLES:
        spec = specs[role]
        frame = letterbox(source, spec.width, spec.height)
        model = RknnLiteYoloV5(spec)
        try:
            detections = model.infer_frame(frame, confidence=0.25)
            print(json.dumps({
                "role": role,
                "adapter": spec.adapter,
                "outputs": [tensor_dims(attr) for attr in model._output_attrs],
                "detections": [value.__dict__ for value in detections[:10]],
                "detection_count": len(detections),
            }, ensure_ascii=False))
        finally:
            model.close()

    mask_spec = specs["mask"]
    mask_rgb = cv2.cvtColor(
        cv2.resize(source, (mask_spec.width, mask_spec.height), interpolation=cv2.INTER_LINEAR),
        cv2.COLOR_BGR2RGB,
    )
    classifier = RknnLiteClassifier(mask_spec)
    try:
        result = classifier.infer_rgb(np.ascontiguousarray(mask_rgb))
        print(json.dumps({
            "role": "mask",
            "adapter": mask_spec.adapter,
            "classification": result.__dict__,
            "probability_finite": bool(np.isfinite(result.score)),
        }, ensure_ascii=False))
    finally:
        classifier.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
