#!/usr/bin/env python3
"""Point the installed RKNN3Lite Python package at the bundled API library."""

from __future__ import annotations

import os
from pathlib import Path
import stat
import sys
import sysconfig
import tempfile


ORIGINAL = "LIBRKNN3RT_PATH = '/usr/lib/librknn3_api.so'"


def patch_file(source: Path, library: Path) -> None:
    if not library.is_file():
        raise FileNotFoundError(library)
    text = source.read_text(encoding="utf-8")
    if text.count(ORIGINAL) != 1:
        raise ValueError(f"unexpected RKNN3Lite library declaration: {source}")
    result = text.replace(ORIGINAL, f"LIBRKNN3RT_PATH = {str(library)!r}")
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=source.parent,
                                     prefix=f".{source.name}.", delete=False) as target:
        temporary = Path(target.name)
        try:
            target.write(result)
            target.flush()
            os.fsync(target.fileno())
        except Exception:
            temporary.unlink(missing_ok=True)
            raise
    os.chmod(temporary, stat.S_IMODE(source.stat().st_mode))
    os.replace(temporary, source)


def main() -> None:
    library = Path(sys.argv[1]).resolve()
    source = Path(sysconfig.get_paths()["purelib"]) / "rknn3lite/api/rknn3_types.py"
    patch_file(source, library)
    assert str(library) in source.read_text(encoding="utf-8")


if __name__ == "__main__":
    main()
