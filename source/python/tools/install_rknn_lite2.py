#!/usr/bin/env python3
"""Install the staged Lite2 wheel into an isolated board-side virtual environment."""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path


def run(command: list[str], env: dict[str, str] | None = None) -> None:
    print("+", " ".join(command))
    subprocess.run(command, check=True, env=env)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-dir", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--model", type=Path)
    parser.add_argument("--no-verify-model", action="store_true")
    args = parser.parse_args()

    project = args.project_dir.resolve()
    wheel_dir = project / "vendor" / "wheels"
    wheels = sorted(wheel_dir.glob("rknn_toolkit_lite2-*-linux_aarch64.whl"))
    if len(wheels) != 1:
        raise SystemExit(f"expected one Lite2 wheel under {wheel_dir}, found {len(wheels)}")
    venv = project / ".venv"
    if not venv.exists():
        run([sys.executable, "-m", "venv", "--system-site-packages", str(venv)])
    python = venv / "bin" / "python"
    run([str(python), "-m", "pip", "install", "--no-deps", "--no-index", str(wheels[0])])

    environment = os.environ.copy()
    runtime = project / "vendor" / "lib" / "librknnrt.so"
    redirect = project / "vendor" / "lib" / "librknnrt_redirect.so"
    if not runtime.exists() or not redirect.exists():
        raise SystemExit("build the project before installing Lite2 so the runtime redirect shim exists")
    environment["RKNNRT_REDIRECT"] = str(runtime)
    environment["LD_PRELOAD"] = f"{redirect}:{environment.get('LD_PRELOAD', '')}"
    run([str(python), "-c", "from rknnlite.api import RKNNLite; print('RKNNLite import: OK')"], environment)
    if not args.no_verify_model:
        model = (args.model or project / "models" / "yolov5s-640-640.rknn").resolve()
        code = (
            "from rknnlite.api import RKNNLite; "
            f"r=RKNNLite(); assert r.load_rknn({str(model)!r}) == 0; "
            "assert r.init_runtime() == 0; print(r.get_sdk_version()); r.release()"
        )
        run([str(python), "-c", code], environment)


if __name__ == "__main__":
    main()
