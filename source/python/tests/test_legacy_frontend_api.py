from __future__ import annotations

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.legacy_frontend_api import LegacyFrontendManager, ScreenSessionState, router


class FakeBackend:
    def __init__(self) -> None:
        self.active: set[str] = set()
        self.standard: set[str] = set()
        self.closed = False

    def close(self) -> None:
        self.closed = True

    def ensure_standard_stream(self, camera_id: str) -> None:
        self.standard.add(camera_id)

    def alias_exists(self, alias: str) -> bool:
        return alias in self.active

    def page_aliases(self) -> set[str]:
        return {value for value in self.active if value.startswith("page")}

    def create_page_alias(self, camera_id: str) -> str:
        alias = f"page{camera_id}"
        self.active.add(alias)
        return alias

    def close_page_alias(self, alias: str) -> bool:
        existed = alias in self.active
        self.active.discard(alias)
        return existed


class FailingBackend(FakeBackend):
    def alias_exists(self, alias: str) -> bool:
        raise RuntimeError("backend unavailable")


def test_screen_session_expires_and_returns_aliases() -> None:
    now = [10.0]
    state = ScreenSessionState(5, clock=lambda: now[0])
    claimed, expired = state.claim()
    assert claimed is True
    assert expired == set()
    state.add_alias("page1")

    now[0] = 14.9
    assert state.collect_expired_aliases() == set()
    now[0] = 15.1
    assert state.collect_expired_aliases() == {"page1"}

    claimed, expired = state.claim()
    assert claimed is True
    assert expired == set()


def test_manager_stream_lifecycle_keeps_standard_stream() -> None:
    backend = FakeBackend()
    manager = LegacyFrontendManager(backend, timeout_seconds=5)

    assert manager.start_screen("42")["code"] == 2000
    assert backend.standard == {"42"}
    assert backend.active == {"page42"}
    assert manager.start_screen("42")["code"] == 2006
    assert manager.stop_screen("42")["code"] == 2000
    assert backend.standard == {"42"}
    assert manager.stop_screen("42")["code"] == 2007


def test_manager_reports_backend_failure_without_raising() -> None:
    manager = LegacyFrontendManager(FailingBackend(), timeout_seconds=5)
    assert manager.start_screen("42")["code"] == 2003
    assert manager.stop_screen("42")["code"] == 2003


def test_legacy_routes_and_disabled_polling_contract() -> None:
    app = FastAPI()
    app.include_router(router)
    app.state.legacy_frontend_manager = LegacyFrontendManager(
        FakeBackend(), timeout_seconds=5
    )

    with TestClient(app) as client:
        assert client.get("/firstscreen").json()["code"] == 2000
        assert client.get("/firstscreen").json()["code"] == 2005
        assert client.post("/heartbeat", json={"heart_num": 0}).json()["code"] == 2001
        assert client.post("/heartbeat", json={"heart_num": 1}).json()["code"] == 2000

        stream = {"camera_id": "42", "rtsp_url": "rtsp://user:password@example/live"}
        assert client.post("/startscreen", json=stream).json()["code"] == 2000
        assert client.post("/startscreen", json=stream).json()["code"] == 2006
        assert client.post("/stopscreen", json={"camera_id": "42"}).json()["code"] == 2000
        assert client.post("/stopscreen", json={"camera_id": "42"}).json()["code"] == 2007

        expected = {"code": 2016, "msg": "RK3588 Python轮询推理暂未启用"}
        assert client.post("/addjob", json={"group_id": "g1"}).json() == expected
        assert client.post("/removejob", json={"group_id": "g1"}).json() == expected
