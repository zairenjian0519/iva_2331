#!/usr/bin/env bash
set -Eeuo pipefail

STATE_FILE=/userdata/sps_test/iva/model-container/state/current.env
HOST_MODEL_DIR=/userdata/sps_test/iva/weights/rknn
ROOTFS_DIR=/userdata/sps_test/iva/model-container/container-rootfs
NAMESPACE=ai-model
CONTAINER_NAME=ai_model_service

[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
[ -r "$STATE_FILE" ] || { echo "deployment state is missing: $STATE_FILE" >&2; exit 1; }
# shellcheck source=/dev/null
. "$STATE_FILE"
case "$MODEL_BACKUP" in /userdata/sps_test/iva/model-container/backups/initial-*/host-models) ;; *) echo "unsafe backup path" >&2; exit 1 ;; esac
[ -d "$MODEL_BACKUP" ] || { echo "host model backup is missing: $MODEL_BACKUP" >&2; exit 1; }

# Prevent Restart=always and the monitor reconciliation loop from racing the
# planned shutdown. cleanup performs the normal unmount after the daemon exits.
systemctl disable ai-model-monitor.service ai-model-service.service >/dev/null 2>&1 || true
systemctl mask --runtime ai-model-monitor.service >/dev/null 2>&1
systemctl kill --kill-who=all --signal=SIGKILL ai-model-monitor.service >/dev/null 2>&1 || true
systemctl stop ai-model-monitor.service >/dev/null 2>&1 || true
systemctl stop ai_engine.service ai_server.service >/dev/null 2>&1 || true
/usr/local/sbin/ai-model-monitor cleanup >/dev/null 2>&1 || true
systemctl stop ai-model-service.service >/dev/null 2>&1 || true
systemctl reset-failed ai-model-monitor.service >/dev/null 2>&1 || true
systemctl unmask --runtime ai-model-monitor.service >/dev/null 2>&1 || true
mountpoint -q "$HOST_MODEL_DIR" && umount "$HOST_MODEL_DIR"
mountpoint -q "$ROOTFS_DIR" && umount "$ROOTFS_DIR"
/usr/local/sbin/ai-model-container stop >/dev/null 2>&1 || true
ctr --namespace "$NAMESPACE" tasks delete --force "$CONTAINER_NAME" >/dev/null 2>&1 || true
ctr --namespace "$NAMESPACE" containers delete "$CONTAINER_NAME" >/dev/null 2>&1 || true

systemctl disable ai-model-monitor.service ai-model-service.service >/dev/null 2>&1 || true
rm -f -- \
    /etc/ai-model-monitor.conf \
    /etc/systemd/system/ai-model-service.service \
    /etc/systemd/system/ai-model-monitor.service \
    /etc/systemd/system/ai_engine.service.d/30-ai-model-monitor.conf \
    /etc/systemd/system/ai_server.service.d/30-ai-model-monitor.conf \
    /usr/local/sbin/ai-model-monitor \
    /usr/local/sbin/ai-model-container

[ -z "$(find "$HOST_MODEL_DIR" -mindepth 1 -print -quit 2>/dev/null)" ] || {
    echo "refusing to replace non-empty host model directory" >&2; exit 1;
}
rmdir "$HOST_MODEL_DIR"
mv "$MODEL_BACKUP" "$HOST_MODEL_DIR"
rmdir "$ROOTFS_DIR" 2>/dev/null || true
systemctl daemon-reload
systemctl start ai_server.service
systemctl start ai_engine.service
mv "$STATE_FILE" "$STATE_FILE.rolled-back-$(date +%Y%m%d-%H%M%S)"
echo "restored host models from $MODEL_BACKUP"
