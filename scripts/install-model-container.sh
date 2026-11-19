#!/usr/bin/env bash
set -Eeuo pipefail

IMAGE_ARCHIVE=${1:?usage: install-model-container.sh IMAGE_ARCHIVE IMAGE_REF MODEL_PROJECT}
IMAGE_REF=${2:?usage: install-model-container.sh IMAGE_ARCHIVE IMAGE_REF MODEL_PROJECT}
MODEL_PROJECT=${3:?usage: install-model-container.sh IMAGE_ARCHIVE IMAGE_REF MODEL_PROJECT}
NAMESPACE=ai-model
CONTAINER_NAME=ai_model_service

[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
[ -f "$IMAGE_ARCHIVE" ] || { echo "model image archive is missing: $IMAGE_ARCHIVE" >&2; exit 1; }
[ -f "$MODEL_PROJECT/scripts/install.sh" ] || {
    echo "model container installer is missing: $MODEL_PROJECT/scripts/install.sh" >&2
    exit 1
}
find "$MODEL_PROJECT/scripts" -type f -name '*.sh' -exec chmod 0755 {} +
[ -x "$MODEL_PROJECT/scripts/install.sh" ] || {
    echo "model container installer is not executable: $MODEL_PROJECT/scripts/install.sh" >&2
    exit 1
}
[ -f "$IMAGE_ARCHIVE.sha256" ] || {
    echo "model image checksum is missing: $IMAGE_ARCHIVE.sha256" >&2
    exit 1
}

(
    cd "$(dirname "$IMAGE_ARCHIVE")"
    sha256sum -c "$(basename "$IMAGE_ARCHIVE").sha256"
)
systemctl is-active --quiet containerd.service || {
    echo "containerd.service is not active" >&2
    exit 1
}
ctr namespaces list | awk 'NR > 1 {print $1}' | grep -qx "$NAMESPACE" ||
    ctr namespaces create "$NAMESPACE"
ctr --namespace "$NAMESPACE" containers info "$CONTAINER_NAME" >/dev/null 2>&1 && {
    echo "model container already exists; refusing to replace it" >&2
    exit 1
}
ctr --namespace "$NAMESPACE" images import "$IMAGE_ARCHIVE"
ctr --namespace "$NAMESPACE" images list -q | grep -Fx "$IMAGE_REF" >/dev/null || {
    echo "imported model image not found: $IMAGE_REF" >&2
    exit 1
}
exec bash "$MODEL_PROJECT/scripts/install.sh" "$IMAGE_REF"
