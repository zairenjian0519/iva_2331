#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
reference=/userdata/sps_test/iva_python/python_mpp

export PYTHONPATH="$root:$root/python"
export RKNNRT_REDIRECT="${RKNNRT_REDIRECT:-$reference/vendor/lib/librknnrt.so}"
export LD_PRELOAD="${LD_PRELOAD:-$reference/vendor/lib/librknnrt_redirect.so}"
exec "$reference/.venv/bin/python" -m app.shadow_engine "$@"
