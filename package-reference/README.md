# RK3588 and RK182X IVA offline installer 3.0.9-history-clean-generic-h264-rkmpp-container

This archive is a generic history-clean variant for same-profile OK3588 boards.
It targets Ubuntu 22.04.4, the board's running `5.10.0` BSP
kernel, a 40 GB virtio root disk at `/dev/vda`, and root-backed `/userdata`.
It installs the Vue frontend, Kyscene Java backend, RK3588 and RK182X Python
engines, media server, 22 model roles, MySQL schema, and all non-BSP runtime
dependencies without target-board network access. Source code is included
under `source/`; prebuilt ARM64 artifacts run on the board.

For H265 camera preview, the RK3588 path uses `hevc_rkmpp` decoding followed by
`h264_rkmpp` hardware H264 encoding. The H264 direct proxy path is unchanged.
This package is intended for BSPs whose RKMPP H264 encoder driver is working.

## New board installation

1. Verify the archive using the accompanying SHA256 file.
2. Extract it on the target board under `/userdata`. Run `sudo ./preflight.sh`,
   then `sudo ./install.sh`, then `sudo ./verify.sh` from the extracted folder.
3. Log in with `admin` / `123456` (System Administrator role) and change the
   password after first login.
   The frontend uses the address in the browser, so no public IP is required;
   `DEVICE_IP=auto` chooses a local address for the device record. The Java
   device MQTT client uses `127.0.0.1`; the selected address stays in
   `network_ip`. The `DeviceCategory` system parameter is seeded as `N150`.

The installer refuses an existing IVA application or database. A new board
starts with one local device record, a 20-camera algorithm quota, and no
cameras or alarms. Add cameras and algorithms in the UI. All cameras initially
run on RK3588, even if an RK1820 card is present.
The archive contains the reference board's nine `camera_factory` rows, including
camera usernames, passwords, IP addresses, and RTSP URLs. Keep the archive and
extracted `database/camera_factory_reference.sql` restricted to trusted users.
If MySQL 8.0 or Mosquitto 2.0 is already installed, the installer keeps its
installed version and checks the offline APT plan before changing packages.
APT archives are cached under `/userdata` to reduce root-partition pressure.

This variant imports the bundled ARM64 RK3588 model image into the `ai-model`
containerd namespace after the full application is installed. The model
container is a read-only file carrier; RKNN inference continues in the host
Python engine. The installer creates and starts `ai-model-service.service` and
`ai-model-monitor.service` only after the business services are ready. The
target kernel and runc/containerd device-cgroup path must support starting the
model holder; this package was not runtime-validated on the current board
because its kernel still reports `BPF_CGROUP_DEVICE` as unsupported.

The original host-mode package remains separate and unchanged. The installer
also backs up `/etc/sudoers.d`, repairs its directory mode to 0755, and
restores the backup during rollback.

The board has no separate `/userdata` partition. The preflight requires at
least 20 GiB free on the root filesystem; application, MySQL, model, and APT
data all consume that root filesystem. The package accepts any IP address on
the same validated hardware and system profile; `DEVICE_IP=auto` selects the
actual default-route address for the device record.

## RK182X cards

The installer sorts discovered PCI device IDs and labels them card 1 and card
2. It tests each card with a bundled RK1820 model before recording a binding
in `/userdata/sps_test/iva/config/camera_deployment.json`. The mapping and
probe results are written to the installation report. No card is required;
unvalidated cards are not assigned. RK1828 models are not included.

To move a camera to a validated card, back up the JSON file, set
`cameras["<camera ID>"]` to `1` or `2` while preserving the `rk182x_cards`
section, validate it with `python3 -m json.tool`, and run
`sudo systemctl restart ai_engine.service`. Remove that camera key to return
it to RK3588. The Java backend removes stale mappings when a camera is
deleted. Check `journalctl -u ai_engine_rk182x_python.service` and
`runtime/logs/inference_metrics.jsonl` in the RK182X project after routing.

The RK182X service starts with `ai_engine.service` and exits successfully
when no cameras are assigned to a card. An unavailable card or unsupported
model produces a clear RK182X startup error; RK3588 remains available for
its assigned cameras. There is no automatic fallback for a camera explicitly
assigned to a failed card.

## Verification and rollback

`verify.sh` checks 102 database tables, clean operational tables, the 20-camera
quota, all local IPv4 HTTP routes, media and read-only container model mounts,
both Python
environments, 11 RK3588 and 11 RK182X model groups, and card bindings. The
dependency versions, sources, architectures, and hashes are in
`DEPENDENCIES.md` and `dependencies/*-manifest.tsv`.

Run `sudo ./rollback.sh` to remove this new business installation and restore
Nginx, the previous udev rule, and legacy path entries. Shared APT packages
remain installed. The source board was read-only during packaging. No blank
target board was available for this release, so login, RTSP preview, and
inference on another board still require first-board acceptance testing.
