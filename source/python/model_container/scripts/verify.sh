#!/usr/bin/env bash
set -Eeuo pipefail

CONFIG_FILE=${AI_MODEL_MONITOR_CONFIG:-/etc/ai-model-monitor.conf}
FAULT_TEST=0
[ "${1:-}" = --fault-test ] && FAULT_TEST=1
[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
[ -r "$CONFIG_FILE" ] || { echo "cannot read $CONFIG_FILE" >&2; exit 1; }
# shellcheck source=/dev/null
. "$CONFIG_FILE"

container_pid() {
    ctr --namespace "$NAMESPACE" tasks list 2>/dev/null |
        awk -v name="$CONTAINER_NAME" 'NR > 1 && $1 == name && $3 == "RUNNING" { print $2; exit }'
}

wait_for_active_stack() {
    local attempt
    for attempt in $(seq 1 120); do
        if [ -n "$(container_pid || true)" ] && mountpoint -q "$ROOTFS_DIR" && mountpoint -q "$HOST_MODEL_DIR" && systemctl is-active --quiet "$SERVER_UNIT" && systemctl is-active --quiet "$ENGINE_UNIT"; then
            return 0
        fi
        sleep 1
    done
    return 1
}

verify_running() {
    local source_path="$ROOTFS_DIR$CONTAINER_MODEL_DIR"
    systemctl is-enabled --quiet ai-model-service.service
    systemctl is-enabled --quiet ai-model-monitor.service
    systemctl is-active --quiet ai-model-service.service
    systemctl is-active --quiet ai-model-monitor.service
    [ -n "$(container_pid || true)" ]
    mountpoint -q "$ROOTFS_DIR"
    mountpoint -q "$HOST_MODEL_DIR"
    findmnt -n -o OPTIONS -M "$ROOTFS_DIR" | tr ',' '\n' | grep -qx ro
    findmnt -n -o OPTIONS -M "$HOST_MODEL_DIR" | tr ',' '\n' | grep -qx ro
    [ "$(stat -c '%d:%i' "$source_path")" = "$(stat -c '%d:%i' "$HOST_MODEL_DIR")" ]
    (cd "$HOST_MODEL_DIR" && sha256sum -c "$BUNDLE_MANIFEST" --quiet)
    systemctl is-active --quiet "$SERVER_UNIT"
    systemctl is-active --quiet "$ENGINE_UNIT"
    curl -fsS http://127.0.0.1:8899/healthcheck >/dev/null
    for path in \
        weight_512/head_person_helmet_10s_all_512/yolov5s_relu.rknn \
        smoke_phone_3s_engine_320/model.rknn \
        reflective_5_s_uncertain/model_fp.rknn \
        fire_smoke_smoking_phone/model_fp.rknn \
        car_detect/model_fp.rknn \
        bolt_512/model_fp.rknn \
        mask_classifier/model_fp.rknn \
        pump_has_oil/model_fp.rknn \
        workwear/model_fp.rknn \
        pipeline_has_oil/model_fp.rknn; do
        [ -r "$HOST_MODEL_DIR/$path" ] || { echo "missing model: $path" >&2; return 1; }
    done
    /usr/local/sbin/ai-model-monitor status
}

wait_for_active_stack
verify_running

if [ "$FAULT_TEST" = 1 ]; then
    echo "fault test: stopping model task"
    ctr --namespace "$NAMESPACE" tasks kill --signal SIGTERM "$CONTAINER_NAME"
    for _ in $(seq 1 40); do
        if ! systemctl is-active --quiet "$ENGINE_UNIT" && ! systemctl is-active --quiet "$SERVER_UNIT" && ! mountpoint -q "$HOST_MODEL_DIR" && ! mountpoint -q "$ROOTFS_DIR"; then
            break
        fi
        sleep 1
    done
    ! systemctl is-active --quiet "$ENGINE_UNIT"
    ! systemctl is-active --quiet "$SERVER_UNIT"
    ! mountpoint -q "$HOST_MODEL_DIR"
    ! mountpoint -q "$ROOTFS_DIR"
    sleep 5
    [ -z "$(container_pid || true)" ]
    echo "fault test: model gate must reject an isolated engine start"
    if systemctl start "$ENGINE_UNIT"; then
        echo "$ENGINE_UNIT started without model readiness" >&2
        exit 1
    fi
    ! systemctl is-active --quiet "$ENGINE_UNIT"
    echo "fault test: manually recovering model task"
    systemctl restart ai-model-service.service
    wait_for_active_stack
    verify_running
fi

echo "verification passed"
