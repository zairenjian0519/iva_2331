from __future__ import annotations

import json
import stat

from app.preview import PreviewRefreshController
from app.snapshot import write_camera_snapshot


class FakeFrame:
    def __init__(self) -> None:
        self.calls = 0

    def jpeg(self, quality: int) -> bytes:
        self.calls += 1
        assert quality == 80
        return b"\xff\xd8preview\xff\xd9"


def test_camera_snapshot_is_atomic_and_readable(tmp_path):
    frame = FakeFrame()
    output = write_camera_snapshot(frame, tmp_path / "show_image", "camera-1")

    assert output.read_bytes() == b"\xff\xd8preview\xff\xd9"
    assert stat.S_IMODE(output.stat().st_mode) == 0o644
    assert stat.S_IMODE(output.parent.stat().st_mode) == 0o755
    assert list(output.parent.glob("*.tmp")) == []
    assert frame.calls == 1


def test_preview_refresh_first_periodic_and_requested():
    controller = PreviewRefreshController(["camera-1"], interval_seconds=300)

    assert controller.should_refresh("camera-1", 10.0)
    controller.mark_success("camera-1", 10.0)
    assert not controller.should_refresh("camera-1", 309.0)
    assert controller.should_refresh("camera-1", 310.0)
    controller.mark_success("camera-1", 310.0)
    controller.request("camera-1")
    assert controller.should_refresh("camera-1", 311.0)


def test_preview_failure_uses_retry_backoff():
    controller = PreviewRefreshController(["camera-1"], retry_seconds=5)

    controller.mark_failure("camera-1", 20.0)
    assert not controller.should_refresh("camera-1", 24.9)
    assert controller.should_refresh("camera-1", 25.0)


def test_mqtt_message_requests_all_or_one_camera():
    controller = PreviewRefreshController(["camera-1", "camera-2"])
    controller.mark_success("camera-1", 100.0)
    controller.mark_success("camera-2", 100.0)

    message = type("Message", (), {"payload": json.dumps({"camera_id": "camera-1"}).encode()})()
    controller._on_message(None, None, message)
    assert controller.should_refresh("camera-1", 101.0)
    assert not controller.should_refresh("camera-2", 101.0)

    message.payload = json.dumps({"refresh": 1}).encode()
    controller._on_message(None, None, message)
    assert controller.should_refresh("camera-2", 101.0)


def test_mqtt_unavailable_does_not_raise():
    def unavailable(**_kwargs):
        raise OSError("broker unavailable")

    controller = PreviewRefreshController(["camera-1"], client_factory=unavailable)
    assert controller.start() is False
    assert controller.should_refresh("camera-1", 1.0)
