#!/usr/bin/env python3
"""Check that the installed RK3588 engine idles before loading models."""

from __future__ import annotations

import ast
from pathlib import Path
import sys


def calls(node: ast.AST, name: str) -> bool:
    return any(isinstance(item, ast.Call) and isinstance(item.func, ast.Name)
               and item.func.id == name for item in ast.walk(node))


def main() -> None:
    tree = ast.parse(Path(sys.argv[1]).read_text(encoding="utf-8"))
    entry = next(item for item in tree.body
                 if isinstance(item, ast.FunctionDef) and item.name == "main")
    source = ast.unparse(entry)
    assert calls(entry, "load_camera_deployment")
    assert "deployment.jobs_for(jobs, 0)" in source
    idle = next(item for item in ast.walk(entry)
                if isinstance(item, ast.If) and isinstance(item.test, ast.UnaryOp)
                and isinstance(item.test.op, ast.Not)
                and isinstance(item.test.operand, ast.Name)
                and item.test.operand.id == "jobs")
    assert any(isinstance(item, ast.Return) and isinstance(item.value, ast.Constant)
               and item.value.value == 0 for item in idle.body)
    assert "stop.wait()" in ast.unparse(idle)
    assert source.index("deployment.jobs_for(jobs, 0)") < source.index("TRTInferCompat(")


if __name__ == "__main__":
    main()
