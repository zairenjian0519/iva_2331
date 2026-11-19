#!/usr/bin/env bash
set -euo pipefail

duration_seconds="${1:-7200}"
interval_seconds="${2:-300}"
metrics_path="${DUCHUN_INFERENCE_METRICS_PATH:-/userdata/sps_test/iva_python/duchun_rk182x_python/runtime/logs/inference_metrics.jsonl}"
unit="ai_engine.service"

start_epoch="$(date +%s)"
start_time="$(date '+%Y-%m-%d %H:%M:%S')"
initial_pid="$(systemctl show "$unit" -p MainPID --value)"
initial_restarts="$(systemctl show "$unit" -p NRestarts --value)"
initial_lines="$(wc -l < "$metrics_path")"
failed=0

echo "stability-start timestamp=$start_time duration_s=$duration_seconds interval_s=$interval_seconds pid=$initial_pid restarts=$initial_restarts metrics_lines=$initial_lines"

while true; do
    now_epoch="$(date +%s)"
    elapsed="$((now_epoch - start_epoch))"
    state="$(systemctl is-active "$unit" || true)"
    pid="$(systemctl show "$unit" -p MainPID --value)"
    restarts="$(systemctl show "$unit" -p NRestarts --value)"
    rss_kb="$(ps -o rss= -p "$pid" 2>/dev/null | tr -d ' ' || true)"
    metrics_lines="$(wc -l < "$metrics_path")"
    latest_timestamp="$(tail -n 1 "$metrics_path" | sed -n 's/.*"timestamp":"\([^"]*\)".*/\1/p')"

    echo "stability-sample timestamp=$(date --iso-8601=seconds) elapsed_s=$elapsed state=$state pid=$pid restarts=$restarts rss_kb=${rss_kb:-0} metrics_lines=$metrics_lines latest_metric=$latest_timestamp"

    if [[ "$state" != "active" || "$pid" != "$initial_pid" || "$restarts" != "$initial_restarts" ]]; then
        failed=1
    fi
    if (( elapsed >= duration_seconds )); then
        break
    fi
    sleep_for="$interval_seconds"
    if (( elapsed + sleep_for > duration_seconds )); then
        sleep_for="$((duration_seconds - elapsed))"
    fi
    sleep "$sleep_for"
done

final_lines="$(wc -l < "$metrics_path")"
fatal_count="$(journalctl -u "$unit" --since "$start_time" --no-pager | grep -Ec 'Traceback|NPUTransfer|MODEL_SETUP|ACK_FAIL|DMA.*(fail|error)|camera worker failed|RGA.*fail|MPP.*buffer.*not released' || true)"
if (( final_lines <= initial_lines || fatal_count != 0 )); then
    failed=1
fi

echo "stability-end timestamp=$(date --iso-8601=seconds) result=$([[ "$failed" -eq 0 ]] && echo passed || echo failed) metrics_added=$((final_lines - initial_lines)) fatal_count=$fatal_count"
exit "$failed"
