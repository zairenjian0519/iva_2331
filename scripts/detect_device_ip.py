#!/usr/bin/env python3
"""Pick a stable local IPv4 for server-side device registration."""

from __future__ import annotations

import ipaddress
import json
import subprocess
import sys


VIRTUAL_PREFIXES = ("docker", "veth", "br-", "virbr", "cni", "flannel", "kube", "tun", "tailscale")


def ip_json(*arguments: str) -> list[dict]:
    result = subprocess.run(
        ["ip", "-j", "-4", *arguments], check=True, text=True, capture_output=True
    )
    return json.loads(result.stdout)


def detect(configured: str) -> str:
    interfaces = ip_json("address", "show", "scope", "global")
    available = [
        (entry["ifname"], address["local"])
        for entry in interfaces
        for address in entry.get("addr_info", [])
        if not entry["ifname"].startswith(VIRTUAL_PREFIXES)
        if address.get("family") == "inet"
        and not ipaddress.IPv4Address(address["local"]).is_loopback
    ]
    if not available:
        raise ValueError("no global IPv4 address found on the target board")
    addresses = {address for _, address in available}
    if configured != "auto":
        requested = str(ipaddress.IPv4Address(configured))
        if requested not in addresses:
            raise ValueError(f"DEVICE_IP is not assigned to this board: {requested}")
        return requested

    routes = ip_json("route", "show", "default")
    for route in routes:
        preferred = route.get("prefsrc")
        if preferred in addresses:
            return preferred
        for interface, address in available:
            if interface == route.get("dev"):
                return address
    return sorted(available)[0][1]


if __name__ == "__main__":
    try:
        print(detect(sys.argv[1] if len(sys.argv) > 1 else "auto"))
    except (OSError, ValueError, subprocess.CalledProcessError) as exc:
        raise SystemExit(str(exc)) from exc
