"""Shadow-only implementation of the legacy /analyresult HTTP contract."""

from __future__ import annotations

import json
import os
from pathlib import Path
import threading
from typing import Any, Optional

from fastapi import FastAPI
from pydantic import BaseModel


class AnalyzeResult(BaseModel):
    result1: list[Any]
    result2: list[Any]
    img: Optional[str] = None
    camera_id: str
    ori_hw: list[int]
    rtsp_ip: str
    rois: dict[str, Any]
    statis: dict[str, Any]
    line1: int
    line2: int
    is_resize: bool


class JsonlWriter:
    def __init__(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        self.path = path
        self.lock = threading.Lock()

    def write(self, value: dict[str, Any]) -> None:
        with self.lock, self.path.open("a", encoding="utf-8") as target:
            target.write(json.dumps(value, ensure_ascii=False, sort_keys=True) + "\n")


writer = JsonlWriter(Path(os.getenv("DUCHUN_SHADOW_HTTP_OUTPUT", "runtime/shadow/analyresult.jsonl")))
app = FastAPI(title="Duchen RK3588 Python Shadow API", version="1")


@app.get("/healthcheck")
def healthcheck() -> dict[str, Any]:
    return {"code": 2000, "msg": "shadow service ready"}


@app.post("/analyresult")
def analyresult(request: AnalyzeResult) -> dict[str, Any]:
    payload = request.dict()
    payload["schema"] = "duchun.rk3588-python.analyresult.v1"
    writer.write(payload)
    return {"code": 2000, "msg": "shadow result recorded"}
