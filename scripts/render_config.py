#!/usr/bin/env python3
"""Use the browser's current host for the frontend backend URL."""

from __future__ import annotations

import argparse
import re
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("web_config", type=Path)
    args = parser.parse_args()

    content = args.web_config.read_text(encoding="utf-8")
    content, count = re.subn(
        r'("VITE_GLOB_DOMAIN_URL"\s*:\s*)"[^"]*"',
        lambda match: match.group(1) + '("http://"+window.location.hostname+":8088/kyscene")',
        content,
    )
    if count != 1:
        raise SystemExit(f"expected one frontend domain URL, found {count}")
    args.web_config.write_text(content, encoding="utf-8")


if __name__ == "__main__":
    main()
