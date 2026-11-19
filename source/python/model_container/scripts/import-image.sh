#!/usr/bin/env bash
set -Eeuo pipefail

NAMESPACE=${NAMESPACE:-ai-model}
IMAGE_REF=${1:?usage: import-image.sh IMAGE_REF ARCHIVE}
ARCHIVE=${2:?usage: import-image.sh IMAGE_REF ARCHIVE}
REMOVE_DOCKER_IMAGE_AFTER_IMPORT=${REMOVE_DOCKER_IMAGE_AFTER_IMPORT:-1}

[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
[ -f "$ARCHIVE" ] && [ -f "$ARCHIVE.sha256" ] || { echo "archive or checksum missing" >&2; exit 1; }

(
    cd "$(dirname "$ARCHIVE")"
    sha256sum -c "$(basename "$ARCHIVE").sha256"
)

ctr namespaces list | awk 'NR > 1 {print $1}' | grep -qx "$NAMESPACE" || ctr namespaces create "$NAMESPACE"
ctr --namespace "$NAMESPACE" images import "$ARCHIVE"
ctr --namespace "$NAMESPACE" images list -q | grep -Fx "$IMAGE_REF" >/dev/null

test_name="ai_model_preflight_$$"
mount_dir=$(mktemp -d /userdata/sps_test/iva/model-container/preflight.XXXXXX)
cleanup() {
    ctr --namespace "$NAMESPACE" tasks kill --signal SIGKILL "$test_name" >/dev/null 2>&1 || true
    ctr --namespace "$NAMESPACE" tasks delete --force "$test_name" >/dev/null 2>&1 || true
    mountpoint -q "$mount_dir" && umount "$mount_dir" || true
    ctr --namespace "$NAMESPACE" containers delete "$test_name" >/dev/null 2>&1 || true
    rmdir "$mount_dir" 2>/dev/null || true
}
trap cleanup EXIT

ctr --namespace "$NAMESPACE" containers create \
    --snapshotter overlayfs --read-only --user 65532:65532 \
    --memory-limit 16777216 "$IMAGE_REF" "$test_name"
ctr --namespace "$NAMESPACE" tasks start --detach "$test_name"
sleep 1
ctr --namespace "$NAMESPACE" tasks list |
    awk -v name="$test_name" 'NR > 1 && $1 == name && $3 == "RUNNING" {found=1} END {exit !found}'

mount_line=$(ctr --namespace "$NAMESPACE" snapshots --snapshotter overlayfs mounts "$mount_dir" "$test_name")
prefix="mount -t overlay overlay $mount_dir -o "
case "$mount_line" in
    "$prefix"*) options=${mount_line#"$prefix"} ;;
    *) echo "unexpected snapshot mount command: $mount_line" >&2; exit 1 ;;
esac
mount -t overlay overlay "$mount_dir" -o "$options"
(
    cd "$mount_dir/models/rknn"
    sha256sum -c .ai-model-bundle/manifest.sha256 --quiet
)

cleanup
trap - EXIT

if [ "$REMOVE_DOCKER_IMAGE_AFTER_IMPORT" = 1 ] && command -v docker >/dev/null; then
    docker image rm "$IMAGE_REF" >/dev/null 2>&1 || true
fi

echo "imported and verified: namespace=$NAMESPACE image=$IMAGE_REF"
