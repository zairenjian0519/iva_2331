#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
MODEL_ROOT=/userdata/sps_test/iva/weights/rknn3
PYTHON=/home/forlinx/chaoyu_workplace/rknn3_env/bin/python
export PYTHONPATH=/usr/local/lib/python3.10/dist-packages:/usr/lib/python3/dist-packages:/home/forlinx/.local/lib/python3.10/site-packages:/userdata/sps_test/iva_python/python_mpp/.venv/lib/python3.10/site-packages:$PROJECT_DIR:$PROJECT_DIR/python

[ "$(id -u)" -eq 0 ] || { echo "run preflight_sync.sh with sudo" >&2; exit 1; }

cd "$PROJECT_DIR"
[ "$(uname -m)" = aarch64 ]
[ -x "$PYTHON" ]
[ -f "$MODEL_ROOT/SHA256SUMS" ]
(cd "$MODEL_ROOT" && sha256sum -c SHA256SUMS --quiet)

"$PYTHON" - <<'PY'
import json
from pathlib import Path

root = Path('/userdata/sps_test/iva/weights/rknn3')
manifest = json.loads((root / 'model_manifest.json').read_text())
validation = json.loads((root / 'validation_report.json').read_text())
expected = {
    'person_helmet', 'smoking_phone', 'reflective_clothes', 'fire_smoke',
    'safety_belt', 'mask', 'vehicle', 'bolt', 'oil_leak', 'uniform',
    'pipe_oil_leak',
}
roles = {item['role'] for item in manifest['models']}
assert roles == expected, (roles, expected)
assert validation['status'] == 'passed'
assert {item['role'] for item in validation['models']} == expected
for item in manifest['models']:
    directory = root / item['directory']
    assert (directory / item['model']).stat().st_size > 0
    assert (directory / item['weight']).stat().st_size > 0
    assert (directory / item['labels']).stat().st_size > 0
    assert item['core_num'] == 1
PY

"$PYTHON" -m compileall -q app compat scripts tests
rm -rf build
bash scripts/build_on_board.sh
ldd rk_mpp_pipeline.cpython-310-aarch64-linux-gnu.so | grep -q 'not found' && {
    echo "rk_mpp_pipeline has unresolved libraries" >&2
    exit 1
}

"$PYTHON" -m unittest discover -s tests -p 'test_*.py' -v
"$PYTHON" scripts/rknn3_smoke.py all
date -Is > .sync-preflight-ok
echo "RK182X staging preflight passed"
