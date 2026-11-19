#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
NEW_IMAGE=${1:?usage: upgrade.sh NEW_IMAGE_REF}
CONFIG_FILE=${AI_MODEL_MONITOR_CONFIG:-/etc/ai-model-monitor.conf}
STATE_FILE=/userdata/sps_test/iva/model-container/state/current.env

[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
[ -r "$CONFIG_FILE" ] && [ -r "$STATE_FILE" ]
# shellcheck source=/dev/null
. "$CONFIG_FILE"
# shellcheck source=/dev/null
. "$STATE_FILE"
OLD_IMAGE=$(ctr --namespace "$NAMESPACE" containers info "$CONTAINER_NAME" | jq -r .Image)
ctr --namespace "$NAMESPACE" images list -q | grep -Fx "$NEW_IMAGE" >/dev/null
[ "$OLD_IMAGE" != "$NEW_IMAGE" ] || { echo "image is already active: $NEW_IMAGE"; exit 0; }

create_container() {
    local image=$1
    ctr --namespace "$NAMESPACE" containers create \
        --snapshotter overlayfs --read-only --user 65532:65532 \
        --memory-limit 16777216 \
        --label "com.sps.platform=rk3588" \
        --label "com.sps.role=model-bundle" \
        "$image" "$CONTAINER_NAME"
}

remove_container() {
    /usr/local/sbin/ai-model-container stop >/dev/null 2>&1 || true
    ctr --namespace "$NAMESPACE" tasks delete --force "$CONTAINER_NAME" >/dev/null 2>&1 || true
    ctr --namespace "$NAMESPACE" containers delete "$CONTAINER_NAME" >/dev/null 2>&1 || true
}

rollback_upgrade() {
    local rc=$?
    set +e
    trap - ERR
    systemctl stop ai-model-monitor.service ai-model-service.service >/dev/null 2>&1 || true
    remove_container
    create_container "$OLD_IMAGE" || true
    systemctl start ai-model-service.service ai-model-monitor.service >/dev/null 2>&1 || true
    exit "$rc"
}
trap rollback_upgrade ERR

systemctl stop ai-model-monitor.service
systemctl stop ai-model-service.service
remove_container
create_container "$NEW_IMAGE"
systemctl start ai-model-service.service
systemctl start ai-model-monitor.service
"$SCRIPT_DIR/verify.sh"

sed -i "s|^IMAGE_REF=.*|IMAGE_REF=$NEW_IMAGE|" "$STATE_FILE"
printf 'PREVIOUS_IMAGE=%s\n' "$OLD_IMAGE" >> "$STATE_FILE"
trap - ERR
echo "upgrade completed: $OLD_IMAGE -> $NEW_IMAGE"
