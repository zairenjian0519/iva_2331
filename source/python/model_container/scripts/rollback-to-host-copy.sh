#!/usr/bin/env bash
set -Eeuo pipefail

HOST_MODEL_DIR=/userdata/sps_test/iva/weights/rknn
RUNTIME_ROOT=/userdata/sps_test/iva/model-container
ROOTFS_DIR=$RUNTIME_ROOT/container-rootfs
ENGINE_UNIT=/etc/systemd/system/ai_engine.service
SERVER_UNIT=/etc/systemd/system/ai_server.service
ENGINE_DROPIN=/etc/systemd/system/ai_engine.service.d/30-ai-model-monitor.conf
SERVER_DROPIN=/etc/systemd/system/ai_server.service.d/30-ai-model-monitor.conf
MODEL_MONITOR_UNIT=ai-model-monitor.service
MODEL_SERVICE_UNIT=ai-model-service.service
NAMESPACE=ai-model
CONTAINER_NAME=ai_model_service
EXPECTED_VERSION=${EXPECTED_VERSION:-}
LOCK_FILE=/run/rollback-to-host-copy.lock

BUNDLE_VERSION=
BUNDLE_PAYLOAD_FILES=0
BUNDLE_PAYLOAD_BYTES=0
BUNDLE_METADATA_BYTES=0

MODE=${1:-}
TXN_DIR=
BACKUP_DIR=
STAGE_DIR=
CANDIDATE_DIR=
CUTOVER_STARTED=0
ROLLBACK_RUNNING=0

log() {
    printf '[rollback-to-host] %s\n' "$*"
}

fail() {
    log "ERROR: $*" >&2
    return 1
}

require_root() {
    [ "$(id -u)" -eq 0 ] || fail "run as root"
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "required command is missing: $1"
}

manifest_payload_bytes() {
    local model_dir=$1
    local manifest=$model_dir/.ai-model-bundle/manifest.sha256
    local line rel size total=0

    while IFS= read -r line || [ -n "$line" ]; do
        [[ "$line" =~ ^[0-9a-fA-F]{64}[[:space:]][[:space:]].+ ]] || {
            fail "invalid manifest line: $line"
            return 1
        }
        rel=${line:66}
        rel=${rel#./}
        case "/$rel" in
            *'/../'*|*'/..'|'/..'|/*'//'* )
                fail "unsafe manifest path: $rel"
                return 1
                ;;
        esac
        [ -f "$model_dir/$rel" ] || {
            fail "manifest file is missing: $rel"
            return 1
        }
        size=$(stat -c %s -- "$model_dir/$rel")
        total=$((total + size))
    done < "$manifest"

    printf '%s\n' "$total"
}

verify_bundle_dir() {
    local model_dir=$1
    local manifest=$model_dir/.ai-model-bundle/manifest.sha256
    local metadata=$model_dir/.ai-model-bundle/bundle.json
    local metadata_version metadata_files metadata_bytes metadata_payload_bytes metadata_platform
    local payload_files total_files payload_bytes unexpected

    [ -d "$model_dir" ] || fail "model directory is missing: $model_dir"
    [ -r "$manifest" ] || fail "bundle manifest is missing: $manifest"
    [ -r "$metadata" ] || fail "bundle metadata is missing: $metadata"

    IFS=$'\t' read -r metadata_version metadata_files metadata_bytes metadata_payload_bytes metadata_platform < <(
        python3 - "$metadata" <<'PY'
import json
import re
import sys

with open(sys.argv[1], "r", encoding="utf-8") as stream:
    value = json.load(stream)

if value.get("schema") != "sps.rk3588-model-bundle.v1":
    raise SystemExit("unsupported bundle schema")
version = value.get("version")
if not isinstance(version, str) or not re.fullmatch(r"[A-Za-z0-9._-]+", version):
    raise SystemExit("invalid bundle version")
if value.get("platform") != "linux/arm64":
    raise SystemExit("unexpected bundle platform")
file_count = value.get("file_count")
byte_count = value.get("bytes")
payload_bytes = value.get("payload_bytes", 0)
if not isinstance(file_count, int) or file_count <= 0:
    raise SystemExit("invalid bundle file_count")
if not isinstance(byte_count, int) or byte_count <= 0:
    raise SystemExit("invalid bundle byte count")
if not isinstance(payload_bytes, int) or payload_bytes < 0:
    raise SystemExit("invalid bundle payload_bytes")
print(version, file_count, byte_count, payload_bytes, value["platform"], sep="\t")
PY
    ) || fail "bundle metadata validation failed: $metadata"

    if [ -n "$EXPECTED_VERSION" ] && [ "$metadata_version" != "$EXPECTED_VERSION" ]; then
        fail "bundle version is $metadata_version; expected $EXPECTED_VERSION"
    fi

    payload_files=$(wc -l < "$manifest")
    [ "$payload_files" -eq "$metadata_files" ] ||
        fail "manifest has $payload_files entries; metadata declares $metadata_files"

    total_files=$(find "$model_dir" -type f -printf '.' | wc -c)
    [ "$total_files" -eq $((metadata_files + 2)) ] ||
        fail "bundle has $total_files files; expected $((metadata_files + 2))"

    unexpected=$(find "$model_dir" -mindepth 1 ! -type f ! -type d -print -quit)
    [ -z "$unexpected" ] || fail "bundle contains unsupported object: $unexpected"

    payload_bytes=$(manifest_payload_bytes "$model_dir")
    [ "$payload_bytes" -gt 0 ] || fail "manifest payload is empty"
    if [ "$metadata_payload_bytes" -gt 0 ] && [ "$payload_bytes" -ne "$metadata_payload_bytes" ]; then
        fail "payload has $payload_bytes bytes; metadata declares $metadata_payload_bytes"
    fi

    (
        cd "$model_dir"
        sha256sum -c .ai-model-bundle/manifest.sha256 --quiet
    ) || fail "bundle checksum verification failed: $model_dir"
    if [ -z "$BUNDLE_VERSION" ]; then
        BUNDLE_VERSION=$metadata_version
        BUNDLE_PAYLOAD_FILES=$payload_files
        BUNDLE_PAYLOAD_BYTES=$payload_bytes
        BUNDLE_METADATA_BYTES=$metadata_bytes
    else
        [ "$metadata_version" = "$BUNDLE_VERSION" ] || fail "bundle version changed during rollback staging"
        [ "$payload_files" -eq "$BUNDLE_PAYLOAD_FILES" ] || fail "bundle file count changed during rollback staging"
        [ "$payload_bytes" -eq "$BUNDLE_PAYLOAD_BYTES" ] || fail "bundle payload bytes changed during rollback staging"
    fi
    log "verified bundle directory=$model_dir version=$metadata_version payload_files=$payload_files payload_bytes=$payload_bytes"
}

unit_fragment_is_expected() {
    local unit_name=$1
    local expected_path=$2
    local fragment
    fragment=$(systemctl show "$unit_name" -p FragmentPath --value)
    [ "$fragment" = "$expected_path" ] ||
        fail "$unit_name fragment is $fragment; expected $expected_path"
}

check_container_mode_units() {
    unit_fragment_is_expected ai_engine.service "$ENGINE_UNIT"
    unit_fragment_is_expected ai_server.service "$SERVER_UNIT"

    [ -r "$ENGINE_DROPIN" ] || fail "ai_engine.service model-monitor drop-in is missing"
    [ -r "$SERVER_DROPIN" ] || fail "ai_server.service model-monitor drop-in is missing"
    grep -Fx 'Requires=ai-model-monitor.service' "$ENGINE_DROPIN" >/dev/null ||
        fail "ai_engine.service drop-in does not contain the expected model-monitor dependency"
    grep -Fx 'Requires=ai-model-monitor.service' "$SERVER_DROPIN" >/dev/null ||
        fail "ai_server.service drop-in does not contain the expected model-monitor dependency"
    grep -F 'ExecStartPre=/usr/local/sbin/ai-model-monitor check-ready engine' "$ENGINE_DROPIN" >/dev/null ||
        fail "ai_engine.service model readiness gate is missing"
    grep -F 'ExecStartPre=/usr/local/sbin/ai-model-monitor check-ready server' "$SERVER_DROPIN" >/dev/null ||
        fail "ai_server.service model readiness gate is missing"
}

check_source_mount() {
    local mount_info
    mountpoint -q "$HOST_MODEL_DIR" || fail "$HOST_MODEL_DIR is not a mount point"
    mount_info=$(findmnt -rn -o FSTYPE,OPTIONS -M "$HOST_MODEL_DIR")
    printf '%s\n' "$mount_info" | grep -Eq '^overlay .*\bro\b' ||
        fail "model mount is not the expected read-only overlay: $mount_info"
    mountpoint -q "$ROOTFS_DIR" || fail "$ROOTFS_DIR is not mounted"
}

check_services() {
    local unit
    for unit in "$MODEL_MONITOR_UNIT" "$MODEL_SERVICE_UNIT" ai_server.service ai_engine.service; do
        systemctl is-active --quiet "$unit" || fail "$unit is not active"
    done
    ctr --namespace "$NAMESPACE" tasks list |
        awk -v name="$CONTAINER_NAME" 'NR > 1 && $1 == name && $3 == "RUNNING" { found=1 } END { exit !found }' ||
        fail "$CONTAINER_NAME task is not running"
}

check_disk_space() {
    local available required
    available=$(df -B1 --output=avail "$RUNTIME_ROOT" | awk 'NR == 2 {print $1}')
    [ "$BUNDLE_PAYLOAD_BYTES" -gt 0 ] || fail "bundle must be verified before checking disk space"
    required=$((BUNDLE_PAYLOAD_BYTES + 64 * 1024 * 1024))
    [ "$available" -ge "$required" ] ||
        fail "insufficient /userdata space: available=$available required=$required"
}

run_preflight() {
    local command_name
    for command_name in awk cp ctr curl df find findmnt flock grep install mountpoint mv python3 sed sha256sum stat systemctl systemd-analyze wc; do
        require_command "$command_name"
    done

    [ -x /usr/local/sbin/ai-model-container ] || fail "ai-model-container helper is missing"
    [ -r /etc/ai-model-monitor.conf ] || fail "ai-model-monitor configuration is missing"
    [ -r "$ENGINE_UNIT" ] || fail "ai_engine.service file is missing"
    [ -r "$SERVER_UNIT" ] || fail "ai_server.service file is missing"
    grep -F '"model_root": "/userdata/sps_test/iva/weights/rknn"' \
        /userdata/sps_test/iva_python/duchun_rk3588_python/models/initial_models.json >/dev/null ||
        fail "Python model_root is not the expected host path"

    check_source_mount
    check_services
    check_container_mode_units
    verify_bundle_dir "$HOST_MODEL_DIR"
    check_disk_space

    log "preflight passed"
    log "bundle=$BUNDLE_VERSION payload_files=$BUNDLE_PAYLOAD_FILES payload_bytes=$BUNDLE_PAYLOAD_BYTES metadata_bytes=$BUNDLE_METADATA_BYTES"
}

capture_state() {
    mkdir -p "$BACKUP_DIR" "$STAGE_DIR" "$CANDIDATE_DIR"
    chmod 0700 "$TXN_DIR" "$BACKUP_DIR" "$CANDIDATE_DIR"

    cp -a -- "$ENGINE_UNIT" "$BACKUP_DIR/ai_engine.service"
    cp -a -- "$SERVER_UNIT" "$BACKUP_DIR/ai_server.service"
    cp -a -- "$ENGINE_DROPIN" "$BACKUP_DIR/ai_engine.model-monitor.conf"
    cp -a -- "$SERVER_DROPIN" "$BACKUP_DIR/ai_server.model-monitor.conf"
    cp -a -- /etc/ai-model-monitor.conf "$BACKUP_DIR/ai-model-monitor.conf"
    cp -a -- /etc/systemd/system/ai-model-monitor.service "$BACKUP_DIR/ai-model-monitor.service"
    cp -a -- /etc/systemd/system/ai-model-service.service "$BACKUP_DIR/ai-model-service.service"
    cp -a -- /usr/local/sbin/ai-model-monitor "$BACKUP_DIR/ai-model-monitor"
    cp -a -- /usr/local/sbin/ai-model-container "$BACKUP_DIR/ai-model-container"

    systemctl show ai_engine.service ai_server.service \
        -p FragmentPath -p DropInPaths -p Requires -p Wants -p After -p ExecStartPre \
        > "$BACKUP_DIR/business-units.before.txt"
    systemctl is-enabled "$MODEL_MONITOR_UNIT" "$MODEL_SERVICE_UNIT" ai_server.service ai_engine.service \
        > "$BACKUP_DIR/unit-enablement.before.txt" || true
    systemctl is-active "$MODEL_MONITOR_UNIT" "$MODEL_SERVICE_UNIT" ai_server.service ai_engine.service \
        > "$BACKUP_DIR/unit-state.before.txt" || true
    ctr --namespace "$NAMESPACE" containers list > "$BACKUP_DIR/containers.before.txt"
    ctr --namespace "$NAMESPACE" tasks list > "$BACKUP_DIR/tasks.before.txt"
    ctr --namespace "$NAMESPACE" images list > "$BACKUP_DIR/images.before.txt"
    findmnt -rn -o SOURCE,TARGET,FSTYPE,OPTIONS -M "$HOST_MODEL_DIR" > "$BACKUP_DIR/model-mount.before.txt"
    findmnt -rn -o SOURCE,TARGET,FSTYPE,OPTIONS -M "$ROOTFS_DIR" > "$BACKUP_DIR/rootfs-mount.before.txt"
    sha256sum "$ENGINE_UNIT" "$SERVER_UNIT" "$ENGINE_DROPIN" "$SERVER_DROPIN" \
        /etc/ai-model-monitor.conf \
        /etc/systemd/system/ai-model-monitor.service \
        /etc/systemd/system/ai-model-service.service \
        /usr/local/sbin/ai-model-monitor /usr/local/sbin/ai-model-container \
        > "$BACKUP_DIR/control-files.sha256"
}

copy_models_to_stage() {
    log "copying the verified model bundle to $STAGE_DIR"
    cp -a -- "$HOST_MODEL_DIR/." "$STAGE_DIR/"
    verify_bundle_dir "$STAGE_DIR"
    log "staged model bundle verified"
}

build_host_units() {
    cp -a -- "$ENGINE_UNIT" "$CANDIDATE_DIR/ai_engine.service"
    cp -a -- "$SERVER_UNIT" "$CANDIDATE_DIR/ai_server.service"

    if grep -R -n 'ai-model-monitor' "$CANDIDATE_DIR"; then
        fail "base business units unexpectedly contain inline ai-model-monitor integration"
    fi

    systemd-analyze verify \
        "$CANDIDATE_DIR/ai_engine.service" \
        "$CANDIDATE_DIR/ai_server.service"
    log "candidate systemd units verified"
}

write_restore_script() {
    local restore_script=$TXN_DIR/restore-container-mode.sh
    cat > "$restore_script" <<EOF
#!/usr/bin/env bash
set -Eeuo pipefail
HOST_MODEL_DIR=$HOST_MODEL_DIR
ROOTFS_DIR=$ROOTFS_DIR
TXN_DIR=$TXN_DIR
BACKUP_DIR=$BACKUP_DIR
[ "\$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
systemctl stop ai_engine.service ai_server.service || true
systemctl stop ai-model-monitor.service ai-model-service.service || true
systemctl unmask --runtime ai-model-monitor.service ai-model-service.service || true
mountpoint -q "\$HOST_MODEL_DIR" && { echo "model path is still mounted" >&2; exit 1; }
if [ -d "\$HOST_MODEL_DIR" ]; then
    mv "\$HOST_MODEL_DIR" "\$TXN_DIR/host-models-preserved-\$(date +%Y%m%d-%H%M%S)"
fi
install -d -o root -g root -m 0755 "\$HOST_MODEL_DIR" "\$ROOTFS_DIR"
install -m 0644 "\$BACKUP_DIR/ai_engine.service" /etc/systemd/system/ai_engine.service
install -m 0644 "\$BACKUP_DIR/ai_server.service" /etc/systemd/system/ai_server.service
install -D -m 0644 "\$BACKUP_DIR/ai_engine.model-monitor.conf" "$ENGINE_DROPIN"
install -D -m 0644 "\$BACKUP_DIR/ai_server.model-monitor.conf" "$SERVER_DROPIN"
systemctl daemon-reload
systemctl enable ai-model-service.service ai-model-monitor.service
systemctl start ai-model-service.service
systemctl start ai-model-monitor.service
systemctl is-active --quiet ai_server.service ai_engine.service
echo "container model mode restored"
EOF
    chmod 0700 "$restore_script"
}

move_host_directory_aside() {
    if [ -d "$HOST_MODEL_DIR" ]; then
        if [ -n "$(find "$HOST_MODEL_DIR" -mindepth 1 -print -quit)" ]; then
            mv "$HOST_MODEL_DIR" "$TXN_DIR/hidden-host-models"
            log "preserved pre-existing hidden host files in $TXN_DIR/hidden-host-models"
        else
            rmdir "$HOST_MODEL_DIR"
        fi
    fi
}

restore_container_mode_after_failure() {
    local original_rc=$1
    local failed_models=$TXN_DIR/failed-host-models

    [ "$ROLLBACK_RUNNING" -eq 0 ] || exit "$original_rc"
    ROLLBACK_RUNNING=1
    set +e
    trap - ERR EXIT INT TERM
    log "apply failed; restoring the original container model mode"

    systemctl mask --runtime "$MODEL_MONITOR_UNIT" >/dev/null 2>&1 || true
    systemctl kill --kill-who=all --signal=SIGKILL "$MODEL_MONITOR_UNIT" >/dev/null 2>&1 || true
    systemctl stop "$MODEL_MONITOR_UNIT" >/dev/null 2>&1 || true
    systemctl stop ai_engine.service ai_server.service >/dev/null 2>&1 || true
    /usr/local/sbin/ai-model-monitor cleanup >/dev/null 2>&1 || true
    systemctl stop "$MODEL_SERVICE_UNIT" >/dev/null 2>&1 || true
    mountpoint -q "$HOST_MODEL_DIR" && umount "$HOST_MODEL_DIR" >/dev/null 2>&1 || true
    mountpoint -q "$ROOTFS_DIR" && umount "$ROOTFS_DIR" >/dev/null 2>&1 || true

    if ! mountpoint -q "$HOST_MODEL_DIR"; then
        if [ -d "$HOST_MODEL_DIR" ] && [ -n "$(find "$HOST_MODEL_DIR" -mindepth 1 -print -quit 2>/dev/null)" ]; then
            [ -e "$failed_models" ] || mv "$HOST_MODEL_DIR" "$failed_models"
        else
            rmdir "$HOST_MODEL_DIR" >/dev/null 2>&1 || true
        fi
        install -d -o root -g root -m 0755 "$HOST_MODEL_DIR" "$ROOTFS_DIR"
    fi

    install -m 0644 "$BACKUP_DIR/ai_engine.service" "$ENGINE_UNIT"
    install -m 0644 "$BACKUP_DIR/ai_server.service" "$SERVER_UNIT"
    install -D -m 0644 "$BACKUP_DIR/ai_engine.model-monitor.conf" "$ENGINE_DROPIN"
    install -D -m 0644 "$BACKUP_DIR/ai_server.model-monitor.conf" "$SERVER_DROPIN"
    systemctl daemon-reload
    systemctl unmask --runtime "$MODEL_MONITOR_UNIT" "$MODEL_SERVICE_UNIT" >/dev/null 2>&1 || true
    systemctl reset-failed "$MODEL_MONITOR_UNIT" >/dev/null 2>&1 || true
    systemctl enable "$MODEL_SERVICE_UNIT" "$MODEL_MONITOR_UNIT" >/dev/null 2>&1 || true
    systemctl start "$MODEL_SERVICE_UNIT" >/dev/null 2>&1 || true
    systemctl start "$MODEL_MONITOR_UNIT" >/dev/null 2>&1 || true
    log "automatic recovery finished; inspect $TXN_DIR/apply.log"
    exit "$original_rc"
}

wait_for_health() {
    local attempt
    for attempt in $(seq 1 30); do
        if curl --fail --silent --show-error --max-time 2 \
            http://127.0.0.1:8899/healthcheck >/dev/null; then
            return 0
        fi
        sleep 1
    done
    fail "ai_server healthcheck did not become ready"
}

verify_host_mode() {
    local task_count

    mountpoint -q "$HOST_MODEL_DIR" && fail "$HOST_MODEL_DIR is still a mount point"
    findmnt -rn -o FSTYPE -T "$HOST_MODEL_DIR" | grep -Fx overlay >/dev/null &&
        fail "$HOST_MODEL_DIR still resolves to overlay"
    verify_bundle_dir "$HOST_MODEL_DIR"

    systemctl is-enabled --quiet "$MODEL_MONITOR_UNIT" &&
        fail "$MODEL_MONITOR_UNIT is still enabled"
    systemctl is-enabled --quiet "$MODEL_SERVICE_UNIT" &&
        fail "$MODEL_SERVICE_UNIT is still enabled"
    systemctl is-active --quiet "$MODEL_MONITOR_UNIT" &&
        fail "$MODEL_MONITOR_UNIT is still active"
    systemctl is-active --quiet "$MODEL_SERVICE_UNIT" &&
        fail "$MODEL_SERVICE_UNIT is still active"

    task_count=$(ctr --namespace "$NAMESPACE" tasks list |
        awk -v name="$CONTAINER_NAME" 'NR > 1 && $1 == name {count++} END {print count+0}')
    [ "$task_count" -eq 0 ] || fail "$CONTAINER_NAME task still exists"

    systemctl is-active --quiet ai_server.service || fail "ai_server.service is not active"
    systemctl is-active --quiet ai_engine.service || fail "ai_engine.service is not active"
    [ ! -e "$ENGINE_DROPIN" ] || fail "ai_engine.service model-monitor drop-in still exists"
    [ ! -e "$SERVER_DROPIN" ] || fail "ai_server.service model-monitor drop-in still exists"
    if grep -n 'ai-model-monitor' "$ENGINE_UNIT" "$SERVER_UNIT"; then
        fail "business unit files still reference ai-model-monitor"
    fi
    systemctl show ai_engine.service ai_server.service -p Requires -p ExecStartPre |
        grep -q 'ai-model-monitor' && fail "business units still require ai-model-monitor"
    wait_for_health
}

apply_rollback() {
    local stamp
    stamp=$(date +%Y%m%d-%H%M%S)
    TXN_DIR=$RUNTIME_ROOT/host-rollback-$stamp
    BACKUP_DIR=$TXN_DIR/before
    STAGE_DIR=$TXN_DIR/models.staging
    CANDIDATE_DIR=$TXN_DIR/candidate-units

    mkdir -p "$TXN_DIR"
    chmod 0700 "$TXN_DIR"
    exec > >(tee -a "$TXN_DIR/apply.log") 2>&1
    log "transaction=$TXN_DIR"

    capture_state
    copy_models_to_stage
    build_host_units
    write_restore_script

    CUTOVER_STARTED=1
    trap 'restore_container_mode_after_failure $?' ERR

    log "stopping inference and model container services"
    systemctl disable "$MODEL_MONITOR_UNIT" "$MODEL_SERVICE_UNIT"
    systemctl mask --runtime "$MODEL_MONITOR_UNIT"
    systemctl kill --kill-who=all --signal=SIGKILL "$MODEL_MONITOR_UNIT" || true
    systemctl stop "$MODEL_MONITOR_UNIT" || true
    systemctl stop ai_engine.service ai_server.service
    /usr/local/sbin/ai-model-monitor cleanup
    systemctl stop "$MODEL_SERVICE_UNIT"
    systemctl reset-failed "$MODEL_MONITOR_UNIT" || true

    mountpoint -q "$HOST_MODEL_DIR" && fail "$HOST_MODEL_DIR remained mounted after monitor stop"
    mountpoint -q "$ROOTFS_DIR" && fail "$ROOTFS_DIR remained mounted after monitor stop"

    move_host_directory_aside
    mv "$STAGE_DIR" "$HOST_MODEL_DIR"

    rm -f -- "$ENGINE_DROPIN" "$SERVER_DROPIN"
    install -m 0644 "$CANDIDATE_DIR/ai_engine.service" "$ENGINE_UNIT"
    install -m 0644 "$CANDIDATE_DIR/ai_server.service" "$SERVER_UNIT"
    systemctl daemon-reload
    systemctl reset-failed "$MODEL_MONITOR_UNIT" || true

    log "starting business services with host-managed models"
    systemctl start ai_server.service
    wait_for_health
    systemctl start ai_engine.service
    verify_host_mode

    log "restarting business services for persistence verification"
    systemctl restart ai_server.service
    wait_for_health
    systemctl restart ai_engine.service
    verify_host_mode

    systemctl show ai_engine.service ai_server.service \
        -p FragmentPath -p DropInPaths -p Requires -p Wants -p After -p ExecStartPre \
        > "$TXN_DIR/business-units.after.txt"
    systemctl is-enabled "$MODEL_MONITOR_UNIT" "$MODEL_SERVICE_UNIT" ai_server.service ai_engine.service \
        > "$TXN_DIR/unit-enablement.after.txt" || true
    systemctl is-active "$MODEL_MONITOR_UNIT" "$MODEL_SERVICE_UNIT" ai_server.service ai_engine.service \
        > "$TXN_DIR/unit-state.after.txt" || true
    findmnt -rn -o SOURCE,TARGET,FSTYPE,OPTIONS -T "$HOST_MODEL_DIR" \
        > "$TXN_DIR/model-filesystem.after.txt"
    (
        cd "$HOST_MODEL_DIR"
        sha256sum -c .ai-model-bundle/manifest.sha256 --quiet
    )
    touch "$TXN_DIR/COMPLETED"
    trap - ERR
    log "host model rollback completed"
    log "restore_script=$TXN_DIR/restore-container-mode.sh"
}

usage() {
    echo "usage: $0 {--check|--apply}" >&2
    exit 2
}

require_root
case "$MODE" in
    --check)
        run_preflight
        ;;
    --apply)
        exec 9>"$LOCK_FILE"
        flock -n 9 || fail "another rollback-to-host operation is running"
        run_preflight
        apply_rollback
        ;;
    *)
        usage
        ;;
esac
