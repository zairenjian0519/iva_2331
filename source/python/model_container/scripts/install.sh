#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
PROJECT_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)
IMAGE_REF=${1:?usage: install.sh IMAGE_REF}

NAMESPACE=ai-model
CONTAINER_NAME=ai_model_service
HOST_MODEL_DIR=/userdata/sps_test/iva/weights/rknn
RUNTIME_ROOT=/userdata/sps_test/iva/model-container
STATE_DIR=$RUNTIME_ROOT/state
BACKUP_ROOT=$RUNTIME_ROOT/backups
ROOTFS_DIR=$RUNTIME_ROOT/container-rootfs
stamp=$(date +%Y%m%d-%H%M%S)
backup_dir=$BACKUP_ROOT/initial-$stamp
model_backup=$backup_dir/host-models
moved_models=0
installed=0

[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
case "$HOST_MODEL_DIR:$RUNTIME_ROOT:$backup_dir" in
    /userdata/sps_test/iva/weights/rknn:/userdata/sps_test/iva/model-container:/userdata/sps_test/iva/model-container/backups/initial-*) ;;
    *) echo "refusing unexpected deployment paths" >&2; exit 1 ;;
esac
systemctl is-active --quiet containerd.service
ctr --namespace "$NAMESPACE" images list -q | grep -Fx "$IMAGE_REF" >/dev/null
ctr --namespace "$NAMESPACE" containers info "$CONTAINER_NAME" >/dev/null 2>&1 && {
    echo "container already exists; use upgrade.sh" >&2; exit 1;
}
mountpoint -q "$HOST_MODEL_DIR" && { echo "$HOST_MODEL_DIR is already mounted" >&2; exit 1; }
[ -d "$HOST_MODEL_DIR" ] && [ -n "$(find "$HOST_MODEL_DIR" -type f -print -quit)" ] || {
    echo "host model directory is missing or empty" >&2; exit 1;
}

for path in \
    /etc/ai-model-monitor.conf \
    /etc/systemd/system/ai-model-service.service \
    /etc/systemd/system/ai-model-monitor.service \
    /etc/systemd/system/ai_engine.service.d/30-ai-model-monitor.conf \
    /etc/systemd/system/ai_server.service.d/30-ai-model-monitor.conf \
    /usr/local/sbin/ai-model-monitor \
    /usr/local/sbin/ai-model-container; do
    [ ! -e "$path" ] || { echo "existing model-container installation found: $path" >&2; exit 1; }
done

mkdir -p "$backup_dir" "$STATE_DIR" "$ROOTFS_DIR"
systemctl cat ai_engine.service > "$backup_dir/ai_engine.service.before"
systemctl cat ai_server.service > "$backup_dir/ai_server.service.before"
find "$HOST_MODEL_DIR" -type f -exec sha256sum {} + | sort -k2 > "$backup_dir/host-models.sha256"

rollback_install() {
    local rc=$?
    set +e
    trap - ERR EXIT
    if [ "$installed" -eq 1 ]; then
        systemctl disable --now ai-model-monitor.service ai-model-service.service >/dev/null 2>&1 || true
    fi
    mountpoint -q "$HOST_MODEL_DIR" && umount "$HOST_MODEL_DIR" || true
    mountpoint -q "$ROOTFS_DIR" && umount "$ROOTFS_DIR" || true
    /usr/local/sbin/ai-model-container stop >/dev/null 2>&1 || true
    ctr --namespace "$NAMESPACE" tasks delete --force "$CONTAINER_NAME" >/dev/null 2>&1 || true
    ctr --namespace "$NAMESPACE" containers delete "$CONTAINER_NAME" >/dev/null 2>&1 || true
    rm -f -- \
        /etc/ai-model-monitor.conf \
        /etc/systemd/system/ai-model-service.service \
        /etc/systemd/system/ai-model-monitor.service \
        /etc/systemd/system/ai_engine.service.d/30-ai-model-monitor.conf \
        /etc/systemd/system/ai_server.service.d/30-ai-model-monitor.conf \
        /usr/local/sbin/ai-model-monitor \
        /usr/local/sbin/ai-model-container
    systemctl daemon-reload
    if [ "$moved_models" -eq 1 ]; then
        rmdir "$HOST_MODEL_DIR" 2>/dev/null || true
        [ -d "$model_backup" ] && mv "$model_backup" "$HOST_MODEL_DIR"
    fi
    systemctl start ai_server.service >/dev/null 2>&1 || true
    systemctl start ai_engine.service >/dev/null 2>&1 || true
    exit "$rc"
}
trap rollback_install ERR

install -D -m 0755 "$PROJECT_DIR/scripts/ai-model-monitor" /usr/local/sbin/ai-model-monitor
install -D -m 0755 "$PROJECT_DIR/scripts/ai-model-container" /usr/local/sbin/ai-model-container
install -D -m 0644 "$PROJECT_DIR/config/ai-model-monitor.conf" /etc/ai-model-monitor.conf
install -D -m 0644 "$PROJECT_DIR/systemd/ai-model-service.service" /etc/systemd/system/ai-model-service.service
install -D -m 0644 "$PROJECT_DIR/systemd/ai-model-monitor.service" /etc/systemd/system/ai-model-monitor.service
install -D -m 0644 "$PROJECT_DIR/systemd/ai_engine.service.d/30-ai-model-monitor.conf" \
    /etc/systemd/system/ai_engine.service.d/30-ai-model-monitor.conf
install -D -m 0644 "$PROJECT_DIR/systemd/ai_server.service.d/30-ai-model-monitor.conf" \
    /etc/systemd/system/ai_server.service.d/30-ai-model-monitor.conf
installed=1

ctr --namespace "$NAMESPACE" containers create \
    --snapshotter overlayfs --read-only --user 65532:65532 \
    --memory-limit 16777216 \
    --label "com.sps.platform=rk3588" \
    --label "com.sps.role=model-bundle" \
    "$IMAGE_REF" "$CONTAINER_NAME"

systemctl stop ai_engine.service
systemctl stop ai_server.service
mv "$HOST_MODEL_DIR" "$model_backup"
moved_models=1
install -d -o root -g root -m 0755 "$HOST_MODEL_DIR" "$ROOTFS_DIR"

systemctl daemon-reload
systemctl enable ai-model-service.service ai-model-monitor.service
systemctl start ai-model-service.service
systemctl start ai-model-monitor.service
"$PROJECT_DIR/scripts/verify.sh"

cat > "$STATE_DIR/current.env" <<EOF
IMAGE_REF=$IMAGE_REF
INITIAL_BACKUP_DIR=$backup_dir
MODEL_BACKUP=$model_backup
INSTALLED_AT=$stamp
EOF

trap - ERR
echo "installation completed"
echo "image=$IMAGE_REF"
echo "backup=$backup_dir"
