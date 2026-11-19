# H264 RKMPP encoder variant

This package is the hardware encoder variant of the generic history-clean
release. H265 preview uses:

```text
hevc_rkmpp -> h264_rkmpp -> rtmp://127.0.0.1/live/<cameraId>
```

The software encoder variant uses `libx264` and is kept in its original
release package. Do not replace the running software-encoder JAR with this
variant unless the target BSP H264 RKMPP encoder has been tested.
