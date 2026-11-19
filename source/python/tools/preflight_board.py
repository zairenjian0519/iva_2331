#!/usr/bin/env python3
"""Report the board dependencies needed by rk_mpp_pipeline without changing the system."""

from __future__ import annotations

import importlib.util
import json
import platform
import shutil
import subprocess
import sys
from pathlib import Path


def command_output(*command: str) -> str:
    try:
        return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT).strip()
    except (OSError, subprocess.CalledProcessError) as error:
        return f"unavailable: {error}"


def main() -> None:
    include_roots = (Path("/usr/include/rockchip"), Path("/usr/include/rga"))
    report = {
        "machine": platform.machine(),
        "python": sys.version,
        "python_tag": f"cp{sys.version_info.major}{sys.version_info.minor}",
        "cmake": command_output("cmake", "--version").splitlines()[0],
        "pkg_config_ffmpeg": command_output("pkg-config", "--modversion", "libavformat", "libavcodec", "libavutil"),
        "headers": {str(root): root.exists() for root in include_roots},
        "libraries": command_output("ldconfig", "-p"),
        "rknnlite": importlib.util.find_spec("rknnlite") is not None,
        "pybind11": importlib.util.find_spec("pybind11") is not None,
        "cmake_binary": shutil.which("cmake"),
    }
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
