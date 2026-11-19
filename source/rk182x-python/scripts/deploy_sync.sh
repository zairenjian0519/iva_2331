#!/usr/bin/env bash
set -Eeuo pipefail

STAGING=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
PROD=/userdata/sps_test/iva_python/duchun_rk182x_python
BACKUP_ROOT=/userdata/sps_test/iva_python/rk182x-sync-backups
stamp=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR=$BACKUP_ROOT/$stamp
STATE_FILE=$BACKUP_ROOT/current.env

[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
[ -f "$STAGING/.sync-preflight-ok" ] || { echo "staging preflight marker is missing" >&2; exit 1; }
case "$STAGING:$PROD:$BACKUP_DIR" in
  /userdata/sps_test/iva_python/duchun_rk182x_python-staging-*:/userdata/sps_test/iva_python/duchun_rk182x_python:/userdata/sps_test/iva_python/rk182x-sync-backups/*) ;;
  *) echo "unexpected deployment paths" >&2; exit 1 ;;
esac

mkdir -p "$BACKUP_DIR"
chmod 0700 "$BACKUP_DIR"
tar -C "$PROD" --exclude=backups --exclude=runtime --exclude=build --exclude=__pycache__ --exclude=.pytest_cache -czf "$BACKUP_DIR/project.tar.gz" .
cp -a /etc/systemd/system/ai_engine.service "$BACKUP_DIR/ai_engine.service"
systemd_paths=(ai_engine.service.d ai_engine.service.d.pre-backend-selector)
existing=()
for path in "${systemd_paths[@]}"; do [ ! -e "/etc/systemd/system/$path" ] || existing+=("$path"); done
[ "${#existing[@]}" -eq 0 ] || tar -C /etc/systemd/system -czf "$BACKUP_DIR/engine-systemd.tar.gz" "${existing[@]}"
[ ! -f /etc/duchun-rk182x-python.env ] || cp -a /etc/duchun-rk182x-python.env "$BACKUP_DIR/duchun-rk182x-python.env"
install -m 0755 "$STAGING/scripts/rollback_sync.sh" "$BACKUP_DIR/rollback_sync.sh"
systemctl show ai_engine.service ai_server.service -p Id -p MainPID -p ActiveState > "$BACKUP_DIR/services.before.txt"
redis-cli -n 1 --raw GET camera_list_info > "$BACKUP_DIR/camera_list_info.json" || true
redis-cli -n 1 --raw GET algorithm_list_info > "$BACKUP_DIR/algorithm_list_info.json" || true
ai_server_pid=$(systemctl show ai_server.service -p MainPID --value)

rollback_on_error() {
    local rc=$?
    trap - ERR
    "$BACKUP_DIR/rollback_sync.sh" "$BACKUP_DIR" || true
    exit "$rc"
}
trap rollback_on_error ERR

for directory in app compat config models src scripts tests systemd; do
    rsync -a --exclude=__pycache__ "$STAGING/$directory/" "$PROD/$directory/"
done
install -m 0644 "$STAGING/CMakeLists.txt" "$PROD/CMakeLists.txt"
install -m 0755 "$STAGING/rk_mpp_pipeline.cpython-310-aarch64-linux-gnu.so" "$PROD/rk_mpp_pipeline.cpython-310-aarch64-linux-gnu.so"
install -m 0755 "$STAGING/vendor/lib/librknnrt_redirect.so" "$PROD/vendor/lib/librknnrt_redirect.so"
chown -R forlinx:forlinx "$PROD/app" "$PROD/compat" "$PROD/config" "$PROD/models" "$PROD/src" "$PROD/scripts" "$PROD/tests" "$PROD/systemd"
mkdir -p "$PROD/runtime/logs"
[ ! -f "$PROD/runtime/logs/inference_metrics.jsonl" ] || cp -a "$PROD/runtime/logs/inference_metrics.jsonl" "$BACKUP_DIR/inference_metrics.before.jsonl"
: > "$PROD/runtime/logs/inference_metrics.jsonl"

cd "$PROD"
/home/forlinx/chaoyu_workplace/rknn3_env/bin/python -m compileall -q app compat
bash scripts/select_engine_backend.sh rk182x
sleep 20
systemctl is-active --quiet ai_engine.service
[ "$(systemctl show ai_server.service -p MainPID --value)" = "$ai_server_pid" ]
grep -Fq '/duchun_rk182x_python' <(systemctl show ai_engine.service -p WorkingDirectory -p ExecStart)
test -s "$PROD/runtime/logs/inference_metrics.jsonl"
! journalctl -u ai_engine.service --since "@$(( $(date +%s) - 120 ))" --no-pager | grep -Eq 'Traceback|NPUTransfer.*(failed|error)|DMA.*(failed|error)|camera worker failed'

cat > "$STATE_FILE" <<EOF
BACKUP_DIR=$BACKUP_DIR
DEPLOYED_AT=$stamp
BACKEND=rk182x
EOF
trap - ERR
echo "RK182X flow synchronization deployed successfully"
echo "backup=$BACKUP_DIR"
