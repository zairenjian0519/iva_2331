"""Board integration check for offline-to-running stream recovery."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import shutil
import threading
import time

from rk_mpp_pipeline import CameraConfig, MppRtspManager


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("--restore-after", type=float, default=7.0)
    parser.add_argument("--duration", type=float, default=16.0)
    args = parser.parse_args()

    source = Path(args.source)
    target = Path("/tmp/rk3588-reconnect-recovery.mp4")
    target.unlink(missing_ok=True)

    config = CameraConfig()
    config.camera_id = 1
    config.rtsp_url = str(target)
    config.codec = "auto"
    config.reconnect_delay_ms = 5000
    config.timeout_ms = 5000
    manager = MppRtspManager([config])

    def restore() -> None:
        time.sleep(args.restore_after)
        shutil.copyfile(source, target)

    restorer = threading.Thread(target=restore, name="restore-test-stream")
    restorer.start()
    frames = 0
    saw_backoff = False
    saw_running = False
    started_at = time.monotonic()
    manager.start()
    start_seconds = time.monotonic() - started_at
    try:
        deadline = time.monotonic() + args.duration
        while time.monotonic() < deadline:
            if manager.read_latest(1, timeout_ms=100) is not None:
                frames += 1
            state = manager.stream_stats(1).state
            saw_backoff = saw_backoff or state == "backoff"
            saw_running = saw_running or state == "running"
        stats = manager.stream_stats(1)
        result = {
            "manager_start_seconds": round(start_seconds, 3),
            "frames": frames,
            "decoded_frames": stats.decoded_frames,
            "errors": stats.errors,
            "reconnects": stats.reconnects,
            "saw_backoff": saw_backoff,
            "saw_running": saw_running,
        }
        print(json.dumps(result, sort_keys=True))
        if start_seconds >= 1.0:
            raise RuntimeError("manager.start() blocked on an unavailable source")
        if not saw_backoff or stats.errors == 0:
            raise RuntimeError("unavailable source did not enter backoff")
        if not saw_running or frames == 0 or stats.decoded_frames == 0:
            raise RuntimeError("source did not recover without restarting the manager")
    finally:
        stopped_at = time.monotonic()
        manager.stop()
        stop_seconds = time.monotonic() - stopped_at
        restorer.join()
        target.unlink(missing_ok=True)
        print(json.dumps({"manager_stop_seconds": round(stop_seconds, 3)}))
        if stop_seconds >= 2.0:
            raise RuntimeError("manager.stop() did not interrupt reconnect backoff promptly")


if __name__ == "__main__":
    main()
