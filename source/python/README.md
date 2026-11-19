# Duchen RK3588 Python RKNNLite Shadow Migration

This project is an independent Python migration of the Duchen visual inference
service. It preserves the legacy Redis, ROI, alarm-box, HTTP, MQTT and image
business contracts while replacing Lynxi decode/IPE/APU/plugin calls with MPP,
RGA, RKNNLite and RKNN postprocessing.

## Initial Scope

- `person_helmet`: person, head and helmet detections from the deployed RKNN model.
- `smoking_phone`: RGA crop of the original NV12 MPP frame for hand/smoking/phone.
- Read-only Redis shadow mode. Results are written to JSONL and no production MQTT
  or `/analyresult` endpoint is called.

## Optional Full-Frame Models

Algorithms 29 (oil leak), 30 (missing workwear), and 31 (pipe oil leak) are
registered as lazy full-frame models. They are loaded only when enabled by a
camera configuration. Their default board paths are declared in
`models/initial_models.json` under `pump_has_oil`, `workwear`, and
`pipeline_has_oil`.

The existing C++ migration is a reference for role and rule compatibility only.
This project does not invoke its GStreamer workers or native inference binaries.

## Board Build And Shadow Run

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python
bash scripts/build_on_board.sh
PYTHONPATH="$PWD:$PWD/platform/python" \
  /userdata/sps_test/iva_python/python_mpp/.venv/bin/python -m unittest tests/test_yolo_lite.py
bash scripts/run_shadow.sh
```

`DUCHUN_REDIS_*` selects a read-only configuration source. Results default to
`runtime/shadow/events.jsonl`; configure a separate writable output path before
running from systemd.

## Memory Boundary

MPP decoded NV12/NV21 is shared with RGA through DMA-BUF. RGA returns a NumPy
view for full-frame and ROI model inputs. RKNNLite consumes an NHWC NumPy view;
its runtime owns the final NPU input transfer and therefore is not a DMA-BUF
end-to-end zero-copy API.
