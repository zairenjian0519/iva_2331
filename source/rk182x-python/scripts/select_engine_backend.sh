#!/usr/bin/env bash
set -euo pipefail

backend=${1:-}
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
project_dir=$(cd -- "$script_dir/.." && pwd)
dropin_dir=/etc/systemd/system/ai_engine.service.d
dropin_backup=/etc/systemd/system/ai_engine.service.d.pre-backend-selector
stamp=$(date +%Y%m%d-%H%M%S)

if [[ ${EUID:-$(id -u)} -ne 0 ]]; then
  echo "run this selector with sudo" >&2
  exit 1
fi

case "$backend" in
  rk3588)
    template="$project_dir/systemd/ai_engine_rk3588.service"
    ;;
  rk182x)
    template="$project_dir/systemd/ai_engine_rk182x.service"
    ;;
  *)
    echo "usage: $0 rk3588|rk182x" >&2
    exit 2
    ;;
esac

if [[ ! -f "$template" ]]; then
  echo "missing service template: $template" >&2
  exit 1
fi

# The old RK3588 drop-ins override ExecStart. Preserve them once, then use
# complete backend-specific service files as the user-visible ai_engine unit.
if [[ -d "$dropin_dir" ]]; then
  if [[ -e "$dropin_backup" ]]; then
      dropin_backup="${dropin_backup}.${stamp}"
  fi
  mv "$dropin_dir" "$dropin_backup"
    echo "preserved engine drop-ins at $dropin_backup"
fi

systemctl stop ai-engine-config-restart.timer 2>/dev/null || true
systemctl disable --now ai_engine_rk182x_python.service 2>/dev/null || true
install -m 0644 "$template" /etc/systemd/system/ai_engine.service
systemctl daemon-reload
systemctl enable ai_engine.service
systemctl restart ai_engine.service
systemctl is-active ai_engine.service
echo "active backend: $backend"
