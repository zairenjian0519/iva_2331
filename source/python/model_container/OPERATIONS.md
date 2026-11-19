# RK3588 Model Container Operations

The complete repeatable workflow for entering host model debugging and
publishing the debugged models back into a new container image is documented in
[HOST_CONTAINER_ROUND_TRIP.md](HOST_CONTAINER_ROUND_TRIP.md).

## Runtime identifiers

```text
containerd namespace: ai-model
container name:        ai_model_service
active image:          docker.io/library/ai_model_service:rk3588-rknn-1.0.0
container models:      /models/rknn
host model mount:      /userdata/sps_test/iva/weights/rknn
container rootfs:      /userdata/sps_test/iva/model-container/container-rootfs
```

The model container only carries RKNN files. Inference continues to run in the
host `ai_engine.service` process.

## Status checks

Show the combined container, mount, Server, and Engine state:

```bash
sudo /usr/local/sbin/ai-model-monitor status
```

Show all related systemd units:

```bash
systemctl status \
  ai-model-service.service \
  ai-model-monitor.service \
  ai_server.service \
  ai_engine.service \
  --no-pager -l
```

Show the containerd task and container:

```bash
sudo ctr --namespace ai-model tasks list
sudo ctr --namespace ai-model containers list
sudo ctr --namespace ai-model images list
```

Show both read-only mounts:

```bash
findmnt -M /userdata/sps_test/iva/model-container/container-rootfs
findmnt -M /userdata/sps_test/iva/weights/rknn
```

The model mount must contain these options:

```text
ro,nosuid,nodev
```

## Normal model-container stop and start

Use systemd for a planned model-container stop:

```bash
sudo systemctl stop ai-model-service.service
```

Within approximately two seconds, the monitor should stop Engine and Server and
remove both model mounts. Check the fail-closed state:

```bash
sudo /usr/local/sbin/ai-model-monitor status
systemctl is-active ai_engine.service ai_server.service
mountpoint /userdata/sps_test/iva/weights/rknn
mountpoint /userdata/sps_test/iva/model-container/container-rootfs
```

Start the model container again:

```bash
sudo systemctl start ai-model-service.service
```

The monitor detects the new task, verifies SHA256, remounts the models, and
starts `ai_server` followed by `ai_engine` automatically.

## Recovery after directly killing the task

For a failure test, terminate the containerd task directly:

```bash
sudo ctr --namespace ai-model tasks kill \
  --signal SIGTERM ai_model_service
```

`ai-model-service.service` is a oneshot unit and may still appear active after a
direct task kill. Recover with `restart`, not `start`:

```bash
sudo systemctl restart ai-model-service.service
```

Then verify recovery:

```bash
sudo /usr/local/sbin/ai-model-monitor status
```

## Monitor stop and start

Stopping the monitor deliberately stops Engine and Server and removes both
mounts, but leaves the model task under systemd control:

```bash
sudo systemctl stop ai-model-monitor.service
```

Start monitoring and inference again:

```bash
sudo systemctl start ai-model-monitor.service
```

## Complete model stack stop and start

Stop in this order:

```bash
sudo systemctl stop ai-model-monitor.service
sudo systemctl stop ai-model-service.service
```

Start in this order:

```bash
sudo systemctl start ai-model-service.service
sudo systemctl start ai-model-monitor.service
```

The RK3588 monitor starts `ai_server` before `ai_engine` because Engine is
ordered after Server and posts inference results to `/analyresult`.

## Engine and Server operations

When the model task and mounts are healthy, these normal commands remain valid:

```bash
sudo systemctl restart ai_server.service
sudo systemctl restart ai_engine.service
```

If the model container or mounts are unavailable, `ExecStartPre` rejects an
Engine or Server start. Do not bypass this gate.

## Logs

Follow model lifecycle logs:

```bash
sudo journalctl -fu \
  ai-model-service.service \
  -u ai-model-monitor.service
```

Follow inference logs:

```bash
sudo journalctl -fu ai_engine.service
sudo journalctl -fu ai_server.service
```

Read the independent inference metrics:

```bash
tail -f \
  /userdata/sps_test/iva_python/duchun_rk3588_python/runtime/logs/inference_metrics.jsonl
```

## Integrity and acceptance checks

Run the non-disruptive verification:

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
sudo ./scripts/verify.sh
```

Run the disruptive fail-closed and recovery test:

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
sudo ./scripts/verify.sh --fault-test
```

The fault test intentionally stops the task and briefly stops inference.

Verify the mounted model files manually:

```bash
cd /userdata/sps_test/iva/weights/rknn
sudo sha256sum -c .ai-model-bundle/manifest.sha256 --quiet
```

## Build and import a model image

Build an offline ARM64 image from the current fixed model directory:

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
sudo ./scripts/build-image.sh rk3588-rknn-1.0.1
```

Import and preflight the image in the isolated `ai-model` namespace:

```bash
sudo ./scripts/import-image.sh \
  docker.io/library/ai_model_service:rk3588-rknn-1.0.1 \
  /userdata/sps_test/iva/model-container/releases/ai_model_service-rk3588-rknn-1.0.1-arm64.tar
```

Building from the fixed directory is safe while it is mounted read-only. The
new image is not activated until the upgrade command is run.

## Upgrade to a new model image

After building and importing the new image:

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
sudo ./scripts/upgrade.sh \
  docker.io/library/ai_model_service:rk3588-rknn-1.0.1
```

The upgrade stops inference, replaces the same container name, verifies the new
bundle, and restores Server and Engine. If activation fails, the script recreates
the previous image container.

## Return to host-managed models

### Restore the original host backup

The original host model backup is stored under:

```text
/userdata/sps_test/iva/model-container/backups/initial-20261023-011258/host-models
```

Remove model-container integration and restore that backup with:

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
sudo ./scripts/rollback.sh
```

This disables the model services, removes the gates and mounts, restores the
host directory, and starts Server followed by Engine.

### Host model debugging round trip

Use the normal rollback when the deployment state and initial host backup both
exist. The fixed model path becomes a writable host directory without changing
the Python model manifest:

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
sudo ./scripts/rollback.sh
findmnt -T /userdata/sps_test/iva/weights/rknn
curl --fail http://127.0.0.1:8899/healthcheck
```

Replace and test models only under `/userdata/sps_test/iva/weights/rknn`.
Restart `ai_engine.service` after a model change so RKNNLite reloads the files.

After debugging, create a unique image version and reinstall the complete model
container integration:

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
VERSION="rk3588-rknn-$(date +%Y%m%d-%H%M%S)"
IMAGE_REF="docker.io/library/ai_model_service:${VERSION}"
ARCHIVE="/userdata/sps_test/iva/model-container/releases/ai_model_service-${VERSION}-arm64.tar"

sudo ./scripts/build-image.sh "$VERSION"
sudo ./scripts/import-image.sh "$IMAGE_REF" "$ARCHIVE"
sudo ./scripts/install.sh "$IMAGE_REF"
sudo ./scripts/verify.sh
```

Use `install.sh`, not `upgrade.sh`, for this transition because `rollback.sh`
removes the active container object and model-container systemd integration.
Do not reuse an existing image tag. Keep the previous imported image until the
new model bundle has passed camera inference and alarm validation.

### Copy the active bundle to host storage

Use this procedure when `state/current.env` or the initial host backup is
missing. It verifies and copies the active bundle before stopping inference:

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
sudo ./scripts/rollback-to-host-copy.sh --check
sudo ./scripts/rollback-to-host-copy.sh --apply
```

The copy rollback leaves `/userdata/sps_test/iva/weights/rknn` unchanged from
the Python application's point of view. It removes the direct model-monitor
dependency from Server and Engine, disables the model-container units, and
retains the container object and image. The completed transaction directory
contains checksums, the previous unit files, logs, and a generated
`restore-container-mode.sh` script.

After the copy rollback, verify the host-managed state:

```bash
mountpoint /userdata/sps_test/iva/weights/rknn
findmnt -T /userdata/sps_test/iva/weights/rknn
systemctl is-active ai-model-monitor.service ai-model-service.service \
  ai_server.service ai_engine.service
systemctl is-enabled ai-model-monitor.service ai-model-service.service \
  ai_server.service ai_engine.service
curl --fail http://127.0.0.1:8899/healthcheck
```

Do not run the original `rollback.sh` when its state file or backup is absent.

## Commands that must not be used

Do not use Docker commands for this model container:

```bash
docker stop ai_model_service
docker rm ai_model_service
```

Docker uses the `moby` namespace; the model container uses the separate
containerd `ai-model` namespace.

Do not delete the production container object manually:

```bash
ctr --namespace ai-model containers delete ai_model_service
```

Deleting it prevents `ai-model-service.service` from recovering the task. Use
`upgrade.sh`, `rollback.sh`, or the documented systemd commands instead.

Do not manually copy files into the fixed model directory while it is mounted.
It is intentionally read-only; model changes must be delivered through a new
versioned image.
