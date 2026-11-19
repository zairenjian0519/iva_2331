#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
PROJECT_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)

VERSION=${1:-rk3588-rknn-1.0.0}
MODEL_SOURCE_DIR=${MODEL_SOURCE_DIR:-/userdata/sps_test/iva/weights/rknn}
BUILD_ROOT=${BUILD_ROOT:-/userdata/sps_test/iva/model-container/build}
RELEASE_DIR=${RELEASE_DIR:-/userdata/sps_test/iva/model-container/releases}
IMAGE_REF="docker.io/library/ai_model_service:${VERSION}"
ARCHIVE="$RELEASE_DIR/ai_model_service-${VERSION}-arm64.tar"

case "$VERSION" in
    *[!A-Za-z0-9._-]*|'') echo "invalid version: $VERSION" >&2; exit 2 ;;
esac
[ -d "$MODEL_SOURCE_DIR" ] || { echo "model source is missing: $MODEL_SOURCE_DIR" >&2; exit 1; }
[ -n "$(find "$MODEL_SOURCE_DIR" -type f -print -quit)" ] || { echo "model source is empty" >&2; exit 1; }

for command_name in awk cmp docker du find gcc mktemp sha256sum sort wc xargs; do
    command -v "$command_name" >/dev/null || { echo "missing command: $command_name" >&2; exit 1; }
done

[ ! -e "$ARCHIVE" ] && [ ! -e "$ARCHIVE.sha256" ] || {
    echo "release archive already exists: $ARCHIVE" >&2
    exit 1
}
if docker image inspect "$IMAGE_REF" >/dev/null 2>&1; then
    echo "docker image tag already exists: $IMAGE_REF" >&2
    exit 1
fi

mkdir -p "$BUILD_ROOT" "$RELEASE_DIR"
work_dir=$(mktemp -d "$BUILD_ROOT/image.XXXXXX")
cleanup() {
    case "$work_dir" in "$BUILD_ROOT"/image.*) rm -rf -- "$work_dir" ;; esac
}
trap cleanup EXIT

context="$work_dir/context"
source_before="$work_dir/source.before.sha256"
source_after="$work_dir/source.after.sha256"

write_source_manifest() {
    (
        cd "$MODEL_SOURCE_DIR"
        find . -type f ! -path './.ai-model-bundle/*' -print0 |
            sort -z | xargs -0 sha256sum
    )
}

write_source_manifest > "$source_before"
source_file_count=$(wc -l < "$source_before")
source_payload_bytes=$(
    find "$MODEL_SOURCE_DIR" -type f ! -path '*/.ai-model-bundle/*' -printf '%s\n' |
        awk '{total += $1} END {print total+0}'
)
[ "$source_file_count" -gt 0 ] || { echo "source manifest is empty" >&2; exit 1; }
[ "$source_payload_bytes" -gt 0 ] || { echo "source payload is empty" >&2; exit 1; }

mkdir -p "$context/models/.ai-model-bundle"
cp -a "$MODEL_SOURCE_DIR"/. "$context/models/"
rm -rf -- "$context/models/.ai-model-bundle"
mkdir -p "$context/models/.ai-model-bundle"

(
    cd "$context/models"
    find . -type f ! -path './.ai-model-bundle/*' -print0 |
        sort -z | xargs -0 sha256sum > .ai-model-bundle/manifest.sha256
)
cmp -s "$source_before" "$context/models/.ai-model-bundle/manifest.sha256" || {
    echo "copied model payload differs from the source manifest" >&2
    exit 1
}

created=$(date -u +%Y-%m-%dT%H:%M:%SZ)
file_count=$(find "$context/models" -type f ! -path '*/.ai-model-bundle/*' | wc -l)
byte_count=$(du -sb "$context/models" | awk '{print $1}')
cat > "$context/models/.ai-model-bundle/bundle.json" <<EOF
{"schema":"sps.rk3588-model-bundle.v1","version":"$VERSION","created":"$created","platform":"linux/arm64","source":"$MODEL_SOURCE_DIR","file_count":$file_count,"payload_bytes":$source_payload_bytes,"bytes":$byte_count}
EOF

gcc -Os -static -s -Wall -Wextra -o "$context/model-holder" "$PROJECT_DIR/build/model-holder.c"
cp "$PROJECT_DIR/build/Containerfile" "$context/Containerfile"

docker build --network none --pull=false \
    --build-arg "BUNDLE_VERSION=$VERSION" \
    --build-arg "BUNDLE_CREATED=$created" \
    --tag "$IMAGE_REF" \
    --file "$context/Containerfile" "$context"

[ "$(docker image inspect "$IMAGE_REF" --format '{{.Architecture}}')" = arm64 ]
test_name="ai-model-build-test-$$"
docker run --detach --name "$test_name" --read-only --network none --memory 16m "$IMAGE_REF" >/dev/null
trap 'docker rm --force "$test_name" >/dev/null 2>&1 || true; cleanup' EXIT
[ "$(docker inspect "$test_name" --format '{{.State.Running}}')" = true ]
docker rm --force "$test_name" >/dev/null
trap cleanup EXIT

write_source_manifest > "$source_after"
if ! cmp -s "$source_before" "$source_after"; then
    docker image rm "$IMAGE_REF" >/dev/null 2>&1 || true
    echo "model source changed during the image build; release was not published" >&2
    exit 1
fi

archive_tmp="$ARCHIVE.tmp.$$"
docker save --output "$archive_tmp" "$IMAGE_REF"
mv -f -- "$archive_tmp" "$ARCHIVE"
chmod 0644 "$ARCHIVE"
(
    cd "$RELEASE_DIR"
    sha256sum "$(basename "$ARCHIVE")" > "$(basename "$ARCHIVE").sha256"
)
chmod 0644 "$ARCHIVE.sha256"

echo "IMAGE_REF=$IMAGE_REF"
echo "ARCHIVE=$ARCHIVE"
echo "ARCHIVE_SHA256=$ARCHIVE.sha256"
echo "PAYLOAD_FILES=$source_file_count"
echo "PAYLOAD_BYTES=$source_payload_bytes"
echo "SOURCE_MANIFEST_SHA256=$(sha256sum "$source_before" | awk '{print $1}')"
