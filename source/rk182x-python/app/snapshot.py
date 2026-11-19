"""Hardware MPP JPEG snapshots for legacy alarm payloads."""

from __future__ import annotations

import base64
import os
from pathlib import Path
import threading
from typing import Any


def alarm_snapshot(frame: Any, quality: int = 80) -> str:
    """Encode the original MPP NV12/NV21 frame without a CPU pixel copy."""
    return base64.b64encode(frame.jpeg(quality)).decode("ascii")


def write_camera_snapshot(
    frame: Any, output_root: str | Path, camera_id: str, quality: int = 80
) -> Path:
    """Atomically replace a camera preview using an MPP-encoded JPEG."""
    root = Path(output_root)
    root.mkdir(mode=0o755, parents=True, exist_ok=True)
    root.chmod(0o755)
    destination = root / f"{camera_id}.jpg"
    temporary = root / f".{camera_id}.{os.getpid()}.{threading.get_ident()}.tmp"
    encoded = bytes(frame.jpeg(quality))
    if not encoded.startswith(b"\xff\xd8"):
        raise RuntimeError("MPP camera snapshot is not a JPEG")
    try:
        with temporary.open("wb") as target:
            target.write(encoded)
            target.flush()
            os.fsync(target.fileno())
        temporary.chmod(0o644)
        os.replace(temporary, destination)
        destination.chmod(0o644)
        return destination
    finally:
        temporary.unlink(missing_ok=True)
