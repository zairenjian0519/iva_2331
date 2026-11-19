"""Production-compatible HTTP and MQTT side of the RK3588 Python pipeline."""

from __future__ import annotations

import json
import logging
import os
from pathlib import Path
from threading import RLock
from typing import Any

import redis
from fastapi import BackgroundTasks, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from app.legacy_config import decode_double_json
from app.legacy_frontend_api import (
    LegacyFrontendManager,
    enabled_from_environment as legacy_frontend_enabled,
    router as legacy_frontend_router,
)
from iva_utils import bg_task, mqttProducer

LOG = logging.getLogger("duchun.rk3588_python.server")
PROJECT_ROOT = Path(__file__).resolve().parents[1]
ALARM_ROOT = Path(os.getenv("DUCHUN_ALARM_ROOT", str(PROJECT_ROOT / "alarm_picture" / "download"))).resolve()
ALARM_ROOT.mkdir(parents=True, exist_ok=True)


class AnalyzeResult(BaseModel):
    result1: list[Any]
    result2: list[Any]
    detectmode: int = 0
    img: str
    ori_hw: list[int]
    rtsp_ip: str
    rois: dict[str, Any]
    camera_id: str
    statis: dict[str, Any]
    line1: int
    line2: int
    is_resize: bool


app = FastAPI(title="Duchen RK3588 Python IVA", version="1.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])
app.mount("/alarm_picture", StaticFiles(directory=str(ALARM_ROOT)), name="alarm_picture")
LEGACY_FRONTEND_ENABLED = legacy_frontend_enabled()
if LEGACY_FRONTEND_ENABLED:
    app.include_router(legacy_frontend_router)


def _load_system_config() -> dict[str, Any]:
    client = redis.Redis(host=os.getenv("DUCHUN_REDIS_HOST", "127.0.0.1"), port=int(os.getenv("DUCHUN_REDIS_PORT", "6379")), db=int(os.getenv("DUCHUN_REDIS_DB", "1")))
    try:
        payload = decode_double_json(client.get("system_list_info"))
    finally:
        client.close()
    if isinstance(payload, list) and payload and isinstance(payload[0], dict):
        return payload[0]
    if isinstance(payload, dict):
        return payload
    raise RuntimeError("Redis key system_list_info does not contain a system configuration")


def _process_result(request: AnalyzeResult) -> None:
    try:
        with app.state.alarm_lock:
            bg_task(request.detectmode, str(ALARM_ROOT), tuple(request.ori_hw), request.result1, request.result2, request.img, request.camera_id, request.rtsp_ip, app.state.mqtt_producer, request.rois, request.statis, app.state.statis_func_dict, app.state.last_alarm_time, request.line1, request.line2, request.is_resize)
    except Exception:
        LOG.exception("failed to process result for camera %s", request.camera_id)


@app.on_event("startup")
def startup() -> None:
    app.state.system_config = _load_system_config()
    app.state.mqtt_producer = mqttProducer(app.state.system_config)
    app.state.last_alarm_time = {}
    app.state.alarm_lock = RLock()
    app.state.statis_func_dict = {10: lambda count: count == 0, 11: lambda count: count == 1, 12: lambda count: count > 0, 13: lambda count: count > 0}
    if LEGACY_FRONTEND_ENABLED:
        try:
            app.state.legacy_frontend_manager = LegacyFrontendManager.from_environment()
            app.state.legacy_frontend_manager.start()
        except Exception:
            app.state.legacy_frontend_manager = None
            LOG.exception("failed to initialize legacy frontend compatibility API")
    LOG.info("connected to Redis DB %s and initialized MQTT publisher", os.getenv("DUCHUN_REDIS_DB", "1"))


@app.on_event("shutdown")
def shutdown() -> None:
    legacy_manager = getattr(app.state, "legacy_frontend_manager", None)
    if legacy_manager is not None:
        legacy_manager.stop()
    producer = getattr(app.state, "mqtt_producer", None)
    if producer is not None:
        producer.disconnect()


@app.get("/healthcheck")
def healthcheck() -> dict[str, Any]:
    return {"code": 2000, "msg": "rk3588 python service ready"}


@app.get("/refreshcover")
def refreshcover() -> dict[str, Any]:
    topic = os.getenv("DUCHUN_PREVIEW_TOPIC", "web_picrefresh")
    app.state.mqtt_producer.publish(topic, json.dumps({"refresh": 1}))
    return {"code": 2000, "msg": "camera preview refresh queued"}


@app.post("/analyresult")
def analyresult(request: AnalyzeResult, background_tasks: BackgroundTasks) -> dict[str, Any]:
    background_tasks.add_task(_process_result, request)
    return {"code": 2000, "msg": "result accepted"}
