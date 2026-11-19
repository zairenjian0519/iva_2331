#!/usr/bin/env python3
"""Load and exercise one YOLOv5 RKNN model on an RK3588 board."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from types import SimpleNamespace

import cv2
import numpy as np

from app.model_specs import load_specs
from app.yolo_lite import RknnLiteYoloV5


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
    parser.add_argument("--role", default="safety_belt")
    parser.add_argument(
        "--image",
        default="/userdata/sps_test/iva/weights/rknn/weight_512/"
        "head_person_helmet_10s_all_512/bus.jpg",
    )
    parser.add_argument("--confidence", type=float, default=0.05)
    args = parser.parse_args()

    spec = load_specs(args.manifest)[args.role]
    source = cv2.imread(str(Path(args.image)), cv2.IMREAD_COLOR)
    if source is None:
        raise RuntimeError(f"cannot read image: {args.image}")

    model = RknnLiteYoloV5(spec)
    try:
        detections = model.infer_frame(
            letterbox(source, spec.width, spec.height),
            confidence=args.confidence,
        )
        print(json.dumps({
            "role": spec.role,
            "labels": list(model.labels),
            "outputs": [tensor_dims(attr) for attr in model._output_attrs],
            "detection_count": len(detections),
            "detections": [value.__dict__ for value in detections[:20]],
        }, ensure_ascii=False))
    finally:
        model.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
