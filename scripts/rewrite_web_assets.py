#!/usr/bin/env python3
"""Make validated frontend assets use the target board's browser hostname."""

from __future__ import annotations

import argparse
import gzip
from pathlib import Path


SOURCE_URL = '"http://192.168.16.163:8088/kyscene"'
PORTABLE_URL = '("http://"+window.location.hostname+":8088/kyscene")'


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("web_root", type=Path)
    parser.add_argument("ui_source", type=Path)
    parser.add_argument("java_source", type=Path)
    args = parser.parse_args()

    changed = 0
    for path in args.web_root.rglob("*.js"):
        original = path.read_text(encoding="utf-8")
        occurrences = original.count(SOURCE_URL)
        if not occurrences:
            continue
        updated = original.replace(SOURCE_URL, PORTABLE_URL)
        path.write_text(updated, encoding="utf-8")
        compressed = path.with_name(path.name + ".gz")
        if compressed.exists():
            compressed.write_bytes(gzip.compress(updated.encode("utf-8"), compresslevel=9, mtime=0))
        changed += occurrences
    if changed != 2:
        raise SystemExit(f"expected two compiled frontend URLs, found {changed}")

    environment = args.ui_source / ".env.production"
    content = environment.read_text(encoding="utf-8")
    if "http://192.168.16.163:8088/kyscene" not in content:
        raise SystemExit("production frontend source URL not found")
    environment.write_text(
        content.replace("http://192.168.16.163:8088/kyscene", "http://localhost:8088/kyscene"),
        encoding="utf-8",
    )

    settings = args.ui_source / "src/hooks/setting/index.ts"
    source = settings.read_text(encoding="utf-8").replace("\r\n", "\n")
    marker = "  // Take global configuration\n"
    if source.count(marker) != 1:
        raise SystemExit("frontend global settings marker not found")
    source = source.replace(
        marker,
        "  const browserDomainUrl = import.meta.env.PROD\n"
        "    ? `http://${window.location.hostname}:8088/kyscene`\n"
        "    : VITE_GLOB_DOMAIN_URL;\n\n" + marker,
    )
    source = source.replace("domainUrl: VITE_GLOB_DOMAIN_URL,", "domainUrl: browserDomainUrl,")
    source = source.replace("uploadUrl: VITE_GLOB_DOMAIN_URL,", "uploadUrl: browserDomainUrl,")
    source = source.replace("window._CONFIG['domianURL'] = VITE_GLOB_DOMAIN_URL;", "window._CONFIG['domianURL'] = browserDomainUrl;")
    settings.write_text(source, encoding="utf-8")

    for root in (args.ui_source, args.java_source):
        for path in root.rglob("*"):
            if path.suffix not in {".vue", ".java"}:
                continue
            source = path.read_bytes()
            if b"rtsp://admin:123456@" in source:
                path.write_bytes(source.replace(b"rtsp://admin:123456@", b"rtsp://user:password@"))


if __name__ == "__main__":
    main()
