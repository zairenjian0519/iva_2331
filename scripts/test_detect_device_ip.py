#!/usr/bin/env python3
"""Check device registration IP selection with two network interfaces."""

from __future__ import annotations

import importlib.util
from pathlib import Path


def main() -> None:
    source = Path(__file__).with_name("detect_device_ip.py")
    spec = importlib.util.spec_from_file_location("detect_device_ip", source)
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    interfaces = [
        {"ifname": "docker0", "addr_info": [{"family": "inet", "local": "172.17.0.1"}]},
        {"ifname": "eth0", "addr_info": [{"family": "inet", "local": "192.168.1.66"}]},
        {"ifname": "eth1", "addr_info": [{"family": "inet", "local": "192.168.16.163"}]},
    ]
    routes = [{"dev": "eth1", "prefsrc": "192.168.16.163"}]
    module.ip_json = lambda *args: interfaces if args[0] == "address" else routes
    assert module.detect("auto") == "192.168.16.163"
    assert module.detect("192.168.1.66") == "192.168.1.66"
    try:
        module.detect("192.168.2.99")
    except ValueError:
        pass
    else:
        raise AssertionError("unassigned DEVICE_IP must be rejected")

    module.ip_json = lambda *args: interfaces if args[0] == "address" else []
    assert module.detect("auto") == "192.168.1.66"


if __name__ == "__main__":
    main()
