# RK3588 model container

This bundle stores RKNN model files in an OCI image while keeping the existing
host path unchanged for the Python engine.

For day-to-day service and container commands, see [OPERATIONS.md](OPERATIONS.md).
For the repeatable host-debug/container round trip, see
[HOST_CONTAINER_ROUND_TRIP.md](HOST_CONTAINER_ROUND_TRIP.md).

## Runtime contract

```text
containerd namespace: ai-model
container:            ai_model_service
container models:     /models/rknn
host models:          /userdata/sps_test/iva/weights/rknn
```

The container is a read-only, non-root file carrier. It does not execute RKNN,
use NPU devices, or expose networking. `ai-model-monitor.service` mounts its
snapshot and model directory read-only. If the task exits, the monitor stops
`ai_engine.service` and `ai_server.service` and removes both mounts.

## Build and import on the RK3588 board

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
sudo ./scripts/build-image.sh rk3588-rknn-1.0.0
sudo ./scripts/import-image.sh \
  docker.io/library/ai_model_service:rk3588-rknn-1.0.0 \
  /userdata/sps_test/iva/model-container/releases/ai_model_service-rk3588-rknn-1.0.0-arm64.tar
```

The build uses `FROM scratch` and a locally compiled static ARM64 holder, so it
does not pull a base image from the network.

## Initial installation

```bash
sudo ./scripts/install.sh docker.io/library/ai_model_service:rk3588-rknn-1.0.0
sudo ./scripts/verify.sh
```

The installer moves the original host model directory into a timestamped
backup and creates an empty mount point. Failure during the transaction restores
the original directory and services.

## Failure and recovery test

```bash
sudo ./scripts/verify.sh --fault-test
```

The model task is not automatically restarted after a crash. Recover it with:

```bash
sudo systemctl restart ai-model-service.service
```

The monitor then remounts models and starts `ai_server` followed by `ai_engine`.

## Upgrade and rollback

Build and import a new image, then run:

```bash
sudo ./scripts/upgrade.sh docker.io/library/ai_model_service:rk3588-rknn-1.0.1
```

To abandon model containerization and restore the initial host model backup:

```bash
sudo ./scripts/rollback.sh
```

If the installation state or initial host backup is unavailable, copy the
currently verified bundle back to host storage instead:

```bash
sudo ./scripts/rollback-to-host-copy.sh --check
sudo ./scripts/rollback-to-host-copy.sh --apply
```

This path keeps the model directory unchanged, disables the two model-container
units, preserves the container object and image, and writes a transaction-specific
`restore-container-mode.sh` script under `/userdata/sps_test/iva/model-container/`.

Image archives and imported containerd images are retained for manual rollback.
