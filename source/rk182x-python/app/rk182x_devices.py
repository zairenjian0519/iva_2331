"""Discover RK182X IDs exposed by the installed transfer proxy."""

from __future__ import annotations

import subprocess


DEVICE_TRANSPORTS = frozenset({"PCIE", "USB_DEVICE", "ETHERNET", "NETWORK"})


def discover_device_ids() -> set[str]:
    result = subprocess.run(
        ["rknn3_transfer_proxy", "devices"],
        check=True,
        capture_output=True,
        text=True,
        timeout=10,
    )
    devices: set[str] = set()
    for line in result.stdout.splitlines():
        columns = line.split()
        if len(columns) >= 2 and columns[-1] in DEVICE_TRANSPORTS:
            devices.add(columns[0])
    return devices
