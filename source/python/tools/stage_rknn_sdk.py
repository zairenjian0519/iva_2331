#!/usr/bin/env python3
"""Stage the supplied RKNN SDK's exact RK3588 runtime artifacts for deployment."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
from pathlib import Path


WHEEL_PATTERN = re.compile(
    r"rknn_toolkit_lite2-(?P<version>[^-]+)-(?P<tag>cp\d+)-(?P=tag)-linux_aarch64\.whl$"
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def choose_one(paths: list[Path], description: str) -> Path:
    if len(paths) != 1:
        choices = "\n".join(str(path) for path in paths)
        raise SystemExit(f"expected exactly one {description}; found {len(paths)}:\n{choices}")
    return paths[0]


def copy_asset(source: Path, destination: Path) -> dict[str, str]:
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)
    return {"source": str(source), "destination": str(destination), "sha256": sha256(destination)}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--sdk-root", required=True, type=Path)
    parser.add_argument("--target-dir", required=True, type=Path)
    parser.add_argument("--python-tag", default="cp310", help="target CPython ABI, for example cp310")
    parser.add_argument("--sdk-version", default="1.6.0")
    parser.add_argument("--model", type=Path)
    parser.add_argument("--labels", type=Path)
    args = parser.parse_args()

    root = args.sdk_root.resolve()
    target = args.target_dir.resolve()
    wheels = []
    for wheel in root.rglob("rknn_toolkit_lite2-*-linux_aarch64.whl"):
        match = WHEEL_PATTERN.match(wheel.name)
        if match and match["tag"] == args.python_tag and match["version"] == args.sdk_version:
            wheels.append(wheel)
    wheel = choose_one(wheels, f"Lite2 {args.sdk_version} {args.python_tag} aarch64 wheel")

    runtimes = list(root.rglob("rknpu2/runtime/Linux/librknn_api/aarch64/librknnrt.so"))
    runtime = choose_one(runtimes, "RK3588 librknnrt.so")
    model = args.model or choose_one(
        list(root.rglob("rknpu2/examples/rknn_yolov5_demo/model/RK3588/yolov5s-640-640.rknn")),
        "RK3588 YOLOv5 model",
    )
    labels = args.labels or choose_one(
        list(root.rglob("rknpu2/examples/rknn_yolov5_demo/model/coco_80_labels_list.txt")),
        "YOLOv5 COCO labels",
    )

    assets = {
        "lite2_wheel": copy_asset(wheel, target / "vendor" / "wheels" / wheel.name),
        "librknnrt": copy_asset(runtime, target / "vendor" / "lib" / "librknnrt.so"),
        "model": copy_asset(model, target / "models" / "yolov5s-640-640.rknn"),
        "labels": copy_asset(labels, target / "models" / "coco_80_labels_list.txt"),
    }
    lock = {
        "sdk_root": str(root),
        "sdk_version": args.sdk_version,
        "python_tag": args.python_tag,
        "assets": assets,
    }
    (target / "sdk-lock.json").write_text(json.dumps(lock, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(lock, indent=2))


if __name__ == "__main__":
    main()
