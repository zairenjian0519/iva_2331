"""Load and run one or all deployed RKNN3 models using validation inputs."""

from __future__ import annotations

import argparse
from types import SimpleNamespace

import numpy as np

from app.model_specs import load_specs
from app.rknn3_classifier import Rknn3Classifier
from app.rknn3_yolo import Rknn3YoloV5


def run_role(role: str, spec: object, validation_root: str) -> None:
    image = np.load(f"{validation_root}/inputs/{role}.npy")
    if image.ndim == 4 and image.shape[0] == 1:
        image = image[0]
    expected = (spec.height, spec.width, 3)
    if image.shape != expected or image.dtype != np.uint8:
        raise ValueError(f"{role} validation input {image.shape}/{image.dtype} != {expected}/uint8")
    if spec.adapter == "classifier":
        model = Rknn3Classifier(spec)
        try:
            detection = model.infer_rgb(image)
            print(f"rknn3-smoke-ok role={role} class={detection.class_name} score={detection.score}")
        finally:
            model.close()
        return
    frame = SimpleNamespace(
        rgb=image,
        width=spec.width,
        height=spec.height,
        letterbox=SimpleNamespace(scale=1.0, pad_left=0, pad_top=0),
    )
    model = Rknn3YoloV5(spec)
    try:
        detections = model.infer_frame(frame)
        print(f"rknn3-smoke-ok role={role} detections={len(detections)}")
    finally:
        model.close()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("role", nargs="?", default="all")
    parser.add_argument("--manifest", default="models/initial_models.rk182x.json")
    parser.add_argument("--validation-root", default="/userdata/sps_test/iva/weights/rknn3/validation")
    args = parser.parse_args()
    specs = load_specs(args.manifest)
    roles = list(specs) if args.role == "all" else [args.role]
    for role in roles:
        run_role(role, specs[role], args.validation_root)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
