#!/usr/bin/env python3
"""Discover and validate RK1820 cards before assigning camera slots."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile


DEVICE_LINE = re.compile(r"^(\d{4}:\d{2}:\d{2}\.\d)\s+\S+\s+PCIE\s*$")


def parse_devices(output: str) -> list[str]:
    devices = [match.group(1) for line in output.splitlines()
               if (match := DEVICE_LINE.match(line.strip()))]
    if len(devices) != len(set(devices)):
        raise ValueError("duplicate RK182X device ID")
    if len(devices) > 2:
        raise ValueError(f"at most two RK182X cards are supported; found {len(devices)}")
    return sorted(devices)


def build_config(devices: list[str], manifest: Path, probe) -> tuple[dict, list[dict]]:
    cards: dict[str, dict] = {}
    report: list[dict] = []
    for slot, device_id in enumerate(devices, start=1):
        ok, reason = probe(device_id)
        report.append({"slot": slot, "device_id": device_id,
                       "status": "ready" if ok else "unavailable", "detail": reason})
        if ok:
            cards[str(slot)] = {"device_id": device_id, "target": "rk1820",
                                "model_manifest": str(manifest)}
    return {"default": 0, "cameras": {}, "rk182x_cards": cards}, report


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=path.parent,
                                     prefix=f".{path.name}.", delete=False) as target:
        temporary = Path(target.name)
        try:
            json.dump(value, target, ensure_ascii=False, indent=2)
            target.write("\n")
            target.flush()
            os.fsync(target.fileno())
        except Exception:
            temporary.unlink(missing_ok=True)
            raise
    os.chmod(temporary, 0o644)
    os.replace(temporary, path)


def probe_model(manifest: Path, device_id: str) -> int:
    from rknn3lite.api import RKNN3Lite

    document = json.loads(manifest.read_text(encoding="utf-8"))
    model_root = Path(document["model_root"])
    primary = next(model for model in document["models"] if model["role"] == "person_helmet")
    directory = model_root / primary["directory"]
    model_path = directory / primary.get("model", "model.rknn")
    weight_path = directory / primary.get("weight", "model.weight")
    runtime = RKNN3Lite(verbose=False)
    try:
        if runtime.load_rknn(str(model_path), str(weight_path)) != 0:
            return 1
        return int(runtime.init_runtime(target="rk1820", core_mask=1,
                                        device_id=device_id.encode("ascii")) != 0)
    finally:
        runtime.release()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--proxy", type=Path)
    parser.add_argument("--model-manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--probe-id")
    args = parser.parse_args()
    if args.probe_id:
        return probe_model(args.model_manifest, args.probe_id)
    if not args.proxy or not args.output or not args.report:
        parser.error("--proxy, --output, and --report are required")

    proxy_error = None
    try:
        result = subprocess.run([str(args.proxy), "devices"], check=True,
                                capture_output=True, text=True, timeout=15)
        devices = parse_devices(result.stdout)
    except (OSError, subprocess.SubprocessError) as error:
        devices, proxy_error = [], str(error)

    def probe(device_id: str) -> tuple[bool, str]:
        try:
            result = subprocess.run([sys.executable, str(Path(__file__).resolve()),
                                     "--model-manifest", str(args.model_manifest),
                                     "--probe-id", device_id], capture_output=True,
                                    text=True, timeout=45)
            return result.returncode == 0, (result.stderr or result.stdout).strip()[-400:]
        except (OSError, subprocess.SubprocessError) as error:
            return False, str(error)

    config, report = build_config(devices, args.model_manifest, probe)
    write_json(args.output, config)
    write_json(args.report, {"proxy_error": proxy_error, "cards": report})
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
