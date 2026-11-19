# RK3588 Python 视觉检测系统：业务流程、编译部署与调试手册

## 1. 文档范围

本文描述当前部署在 RK3588 开发板上的 Python 版本：

```text
/userdata/sps_test/iva_python/duchun_rk3588_python
```

开发机源码目录为：

```text
/home/forlinx/Projects/RK3588/iva/duchun_rk3588_python
```

系统复用 Kyscene 的 Redis、MQTT、MySQL、前端和 Java 后端，不修改旧 C++ 可执行程序 `/userdata/sps_test/iva`。当前正式服务为：

| 服务 | 实际实现 | 作用 |
| --- | --- | --- |
| `ai_engine.service` | `python -m app.active_engine` | RTSP、MPP/RGA、RKNNLite 推理和结果投递 |
| `ai_server.service` | `uvicorn app.active_server:app` | 告警规则、图片落盘、MQTT 发布和封面刷新接口 |
| `kyscene.service` | Kyscene Java JAR | 配置管理、MQTT 消费、数据库、WebSocket |
| `ai_business.service` | 已停用 | 旧 C++ 业务适配器，不参与 Python 链路 |

## 2. 总体架构

```text
Kyscene 页面/数据库
        |
        | 写配置
        v
Redis DB 1
  camera_list_info / algorithm_list_info / system_list_info
        |
        v
app.active_engine
  配置解析 -> FFmpeg RTSP 解复用 -> MPP 硬解 -> RGA letterbox/RGB
                                         |              |
                                         |              +-> NumPy view -> RKNNLite
                                         |                                  |
                                         +-> NV12/NV21 DMA-BUF              v
                                                                    YOLOv5 后处理
                                                                          |
                                  原视频坐标 person_boxes/result_other <---+
                                                                          |
                          MPP JPEG -> Base64 -> POST /analyresult ---------+
                                                                          v
app.active_server -> iva_utils.bg_task -> ROI/人数/间隔判断 -> 告警图落盘
                                                        |
                                                        v
                                                MQTT alarm_result
                                                        |
                                                        v
Kyscene Java -> MySQL + WebSocket -> 前端实时告警

摄像头封面：
首帧/每 300 秒/web_picrefresh -> MPP JPEG -> show_image/<camera_id>.jpg
                                               |
                                               v
                         Nginx /cameraImg 与 /kyscene/cameraImg
```

## 3. 主要代码职责

| 模块 | 职责 |
| --- | --- |
| `app/active_engine.py` | engine 进程入口、流管理、推理节流、HTTP 投递、封面刷新 |
| `app/active_server.py` | `/analyresult`、`/refreshcover`、Redis 系统配置和 MQTT publisher |
| `app/legacy_config.py` | 解析 Redis 双层 JSON，生成 `CameraJob`，按真实帧尺寸缩放 ROI |
| `app/inference.py` | `TRTInferCompat`，保持灵犀版 `infer()` 输出契约 |
| `app/yolo_lite.py` | RKNNLite 初始化、YOLOv5 三头解码、NMS、原图坐标还原 |
| `compat/business_rules.py` | 人员过滤、二级 ROI、类别与算法编号映射 |
| `app/snapshot.py` | 告警 Base64 和摄像头封面 JPEG 原子落盘 |
| `app/preview.py` | 首帧、周期和 MQTT 封面刷新状态机 |
| `src/rk_mpp_pipeline.cpp` | pybind11、FFmpeg、MPP、RGA、DMA-BUF、JPEG 编码器 |
| `iva_utils.py` | 原业务 `bg_task()`、报警间隔、电子围栏、画框、MQTT payload |
| `models/initial_models.json` | RKNN 模型路径、输入尺寸、anchors、stride、阈值和 NMS |

## 4. 服务启动流程

### 4.1 systemd 启动

systemd 的 `99-duchun-rk3588-python.conf` 清空旧服务的 `ExecStart`，改为 Python 入口。有效配置可通过以下命令确认：

```bash
SYSTEMD_PAGER=cat systemctl cat ai_engine.service
SYSTEMD_PAGER=cat systemctl cat ai_server.service
```

关键运行环境来自 `/etc/duchun-rk3588-python.env`：

```text
DUCHUN_REDIS_HOST=127.0.0.1
DUCHUN_REDIS_DB=1
DUCHUN_RESULT_ENDPOINT=http://127.0.0.1:8899/analyresult
DUCHUN_ALARM_ROOT=/userdata/sps_test/iva-kyscene/data/alarm_picture/download
DUCHUN_CAMERA_IMAGE_ROOT=/userdata/sps_test/iva-kyscene/data/alarm_picture/download/show_image
DUCHUN_CAMERA_REFRESH_SECONDS=300
DUCHUN_PREVIEW_TOPIC=web_picrefresh
TZ=Asia/Shanghai
```

`RKNNRT_REDIRECT` 和 `LD_PRELOAD` 用于让 RKNNLite 使用项目锁定的 `librknnrt.so`。`TZ=Asia/Shanghai` 必须保留，否则算法有效时段会按 UTC 判断。

### 4.2 engine 初始化

`app.active_engine.main()` 按以下顺序执行：

1. 连接 Redis DB 1。
2. 读取 `camera_list_info` 和 `algorithm_list_info`。
3. `build_jobs()` 解析双层 JSON，按 `camera_id` 合并相机和算法。
4. 加载 `models/initial_models.json` 与 `models/roles.json`。
5. 创建主模型和抽烟/手机模型各自的 `RKNNLite` 上下文。
6. 每个模型调用 `load_rknn()` 和 `init_runtime(NPU_CORE_0_1_2)`。
7. 为每路活动相机创建 `CameraConfig` 和 `MppRtspManager` worker。
8. 启动封面 MQTT subscriber，订阅 `web_picrefresh`。
9. 启动 RTSP/MPP worker，进入取帧循环。

业务 `camera_id` 可能超过 C++ `int` 范围，因此 MPP 内部使用从 1 开始的连续 `stream_id`；HTTP、MQTT 和数据库始终使用原始业务 `camera_id`。

## 5. Redis 配置解析

Redis 的三个关键值都是“JSON 字符串中再包含 JSON”的双层结构，必须通过 `decode_double_json()` 解析：

| Key | 内容 |
| --- | --- |
| `camera_list_info` | RTSP URL、相机 ID、IP、任务状态等 |
| `algorithm_list_info` | 算法编号、阈值、ROI、有效时段、报警间隔 |
| `system_list_info` | MQTT 地址和平台系统配置 |

Kyscene 重启后生成的相机配置可能不包含 `width/height`。`CameraJob` 会保留原始归一化 ROI，engine 获得第一帧后调用：

```python
ai_dict, rois_dict = job.geometry(frame.width, frame.height)
```

因此 ROI 使用解码帧真实尺寸，不依赖写死的 704x576、1920x1080 等值。分辨率变化时缓存键 `(stream_id, width, height)` 改变，ROI 会重新计算。

算法有效时段由 `_within_valid_time()` 按北京时间、星期键 `0..6` 判断。不在时段内的算法不会推理或报警。

## 6. RTSP、MPP 与 RGA 数据路径

### 6.1 FFmpeg 只负责解复用

`StreamWorker` 在 C++ 扩展中执行：

1. `avformat_open_input()` 使用 RTSP/TCP 打开流。
2. `avformat_find_stream_info()` 自动识别 H.264/H.265 和实际分辨率。
3. H.264 使用 `h264_mp4toannexb`，H.265 使用 `hevc_mp4toannexb`。
4. `av_read_frame()` 只读取压缩 packet，不做软件解码。
5. Annex-B packet 送入 `MppDecoder::submit()`。

`codec=auto` 时由 FFmpeg 探测，不需要在摄像头配置中写死 H.264。

### 6.2 MPP 硬件解码

`MppDecoder` 通过 RAII 管理 `MppCtx`、`MppApi` 和 buffer group：

```text
mpp_create
  -> mpp_init(MPP_CTX_DEC, AVC/HEVC)
  -> decode_put_packet
  -> decode_get_frame
  -> mpp_frame_get_buffer / DMA-BUF fd
```

解码帧保持 NV12/NV21，`FrameStorage` 持有 MPP buffer。Python `Frame`、NumPy 数组和 capsule 共同维护所有权，避免 Python 仍在使用时底层 buffer 被释放。

每路流只有一个 latest-frame 槽位。新帧到达而旧帧未消费时覆盖旧帧并增加 `dropped_frames`，不会形成无界队列。

### 6.3 RGA 转换

解码后的 DMA-BUF fd 直接交给 RGA：

- 整帧：letterbox 到 640x640 RGB888，提供 `frame.rgb`。
- 二级模型：`frame.rga_rgb()` 从原 NV12/NV21 做 ROI crop、缩放、letterbox 和 RGB 转换。
- Python 获得的是底层 MPP buffer 的 NumPy view，不复制 RGB 像素。
- 检测框通过 scale、padding 和 ROI 原点还原到原视频坐标。

### 6.4 JPEG 编码

`MppJpegEncoder` 使用 `MPP_VIDEO_CodingMJPEG`、`FIXQP` 和完整 qfactor 配置。相同尺寸/格式/质量时复用编码上下文，避免每帧重复初始化和 MPP 日志刷屏。

告警图与封面图都使用原始分辨率 NV12/NV21 输入，不经过 OpenCV 整帧颜色转换。

## 7. RKNNLite 推理与 YOLOv5 后处理

### 7.1 模型

当前正式加载三份模型，每个 engine 进程各加载一次并由所有摄像头共享：

| role | 用途 | 输入 | 输出 |
| --- | --- | --- | --- |
| `person_helmet` | person/head/helmet | 640x640 RGB NHWC uint8 | YOLOv5 三检测头 |
| `smoking_phone` | hand/smoking/phone | 640x640 RGB NHWC uint8 | YOLOv5 三检测头 |
| `reflective_clothes` | reflective/no_reflective/uncertain | 640x640 RGB NHWC uint8 | YOLOv5 三检测头 |
| `oil_leak` | oil | 640x640 RGB NHWC uint8 | YOLOv5 三检测头 |
| `uniform` | person/workwear | 640x640 RGB NHWC uint8 | YOLOv5 三检测头 |
| `pipe_oil_leak` | pipe_oil | 640x640 RGB NHWC uint8 | YOLOv5 三检测头 |

模型文件位于 `/userdata/sps_test/iva/weights/rknn`，具体相对路径由 `initial_models.json` 定义。

### 7.2 输入

主模型直接读取：

```python
image = frame.rgb
outputs = RKNNLite.inference(inputs=[image[np.newaxis, ...]])
```

`np.newaxis` 通常只增加视图维度。但 RKNNLite 公共 Python API 不接收 DMA-BUF fd，运行时仍需把 NumPy 输入传入 NPU 输入内存。因此准确描述是：

```text
MPP -> RGA -> NumPy view 零像素拷贝
NumPy -> RKNNLite 是公开 API 的输入传输边界
```

不能宣称端到端 NPU DMA-BUF 零拷贝。

### 7.3 后处理

`RknnLiteYoloV5` 执行：

1. 校验输入为 NHWC uint8 `(640, 640, 3)`。
2. 读取 3 个输出头，兼容 NCHW/NHWC 输出排列。
3. 使用 manifest 中的 anchors 和 stride 解码 xywh。
4. 计算 `objectness * class_score`。
5. 置信度过滤。
6. 按类别执行 NMS。
7. 去除 letterbox padding，并除以 scale。
8. ROI 模型再加回 ROI 左上角偏移。
9. 裁剪到原视频边界，输出 `{x,y,width,height}`。

## 8. 业务规则

`TRTInferCompat.infer()` 保持原灵犀接口：

```python
person_boxes, result_other, is_leave = infer(frame, ai_dict)
```

第三个返回值沿用灵犀接口名称，但语义是 `leave_enabled`：当前有效配置中存在算法 10
时返回 `True`，表示即使 `person_boxes` 和 `result_other` 都为空也必须上传当前帧。
真正的离岗判断仍由 `bg_task()` 根据 ROI 内人员数量完成，第三个返回值本身不是检测结果。
`ActiveEngine` 同时保留 `"10" in statistics` 判断作为无人帧上报的双重保护。

### 8.1 输出格式

人员框：

```text
[0, score, x, y, width, height]
```

告警框：

```text
[algorithm, score, x, y, width, height,
 alarm_grade, alarm_interval_seconds, target_threshold]
```

`result_other` 经过 `split_result()` 按算法编号分组。

### 8.2 主模型规则

- `person` 进入人员统计和二级 ROI。
- `head` 映射算法 4（未戴安全帽）。
- `helmet` 映射算法 5（佩戴安全帽）。
- 人员框需满足最小宽高比例，过滤远距离小目标。
- invalid ROI 在业务规则出口过滤。

### 8.3 抽烟/手机规则

只有配置算法 0 或 1 时才为合格 person 构造扩展 ROI：

```text
x: person 左右各扩展约半个人宽
y: 从头部到 person 底部下方约 0.7 人高
```

RGA 直接从 NV12/NV21 裁剪这个 ROI。只有同一 person ROI 内同时出现 `hand` 和 `smoking/phone` 才生成对应告警。

### 8.4 反光衣规则

只有配置算法 6 或 7 时，才为合格 person 构造左右各扩 10 像素的 ROI，并由 RGA 从原始 NV12/NV21 裁剪、letterbox 到 640x640。类别映射为：

```text
reflective / refleactive       -> 算法 7，穿戴反光衣
no_reflective / no_refleactive -> 算法 6，未穿反光衣
uncertain                      -> 忽略
```

人员框还需通过宽 `0.175 * frame_width`、高 `0.104 * frame_height` 的过滤，只有宽高同时不足才丢弃。优先在面积不小于扩展 ROI 10% 的子框中取最高分；如果全部过小，则按灵犀逻辑回退到全部子框中的最高分。最终告警使用原始 person 坐标和两位置信度。

### 8.5 安全带三状态规则

算法 3 和 16 共用全帧 640x640 YOLOv5s 模型，仅在至少一个摄像头启用其中任一算法时按需加载：

```text
/userdata/sps_test/iva/weights/rknn/safety_belt/model_fp.rknn
safety           -> 算法 3，已系安全带
uncertain_safety -> 忽略
no_safety        -> 算法 16，未系安全带
```

每个主模型 person 只关联中心点落在其框内的安全带状态框，并选取最高置信度状态。`uncertain_safety` 和没有状态框都不产生告警；不能再使用灵犀旧模型的“没有 safety 框即未系安全带”推导，否则遮挡、远距离人员会产生误报。最终告警使用原始 person 坐标，算法 3 和 16 分别使用对应摄像头的前端阈值、无效区域、告警等级和告警间隔。

手工运行板端预检时必须使用生产服务相同的 `RKNNRT_REDIRECT` 和 `LD_PRELOAD`，否则 RKNNLite 会误加载 `/usr/lib/librknnrt.so` 1.3.0，无法读取 Toolkit2 1.6.0 生成的模型。

### 8.6 漏油与工服规则

算法 29、30、31 都是全帧 640x640 YOLOv5 模型，仅在当前相机启用对应算法后延迟加载：

```text
/userdata/sps_test/iva/weights/rknn/pump_has_oil/model_fp.rknn     -> 算法 29 漏油检测
/userdata/sps_test/iva/weights/rknn/workwear/model_fp.rknn         -> 算法 30 未穿工服
/userdata/sps_test/iva/weights/rknn/pipeline_has_oil/model_fp.rknn -> 算法 31 管道漏油检测
```

- 算法 29、31：每个超过前端阈值且未落入无效区域的检测框直接生成告警。
- 算法 30：模型类别固定为 `0=person`、`1=workwear`。如果任一 workwear 框的中心点落在 person 框内，则该人员视为已穿工服；否则使用 person 框和人员置信度生成未穿工服告警。
- 三个模型使用各自目录内的 `labels.txt`；RKNN 文件名或目录不同时应先修改 `models/initial_models.json`。
- 模型未安装且算法未启用时不会加载，不影响现有 0-28 算法。

### 8.7 人员入侵示例（算法 12）

1. 主模型产生 `person_boxes`。
2. engine 把算法 12 放入 `statis`。
3. 有人员时生成 MPP JPEG，并 POST `/analyresult`。
4. `bg_task()` 检查 camera ROI；ROI 为空表示全画面。
5. 检查 invalid ROI、`target_threshold` 和报警间隔。
6. `send_result()` 在原视频坐标画框、写 JPEG。
7. MQTT 发布 `alarm_result`。
8. Kyscene 入库并通过 WebSocket 推送前端。

统计算法 11/12/13 只有检测到人员才送图；算法 10（离岗）必须在无人时也送帧。

## 9. HTTP、图片与 MQTT

### 9.1 `/analyresult`

engine POST 的字段保持灵犀版契约：

```text
result1, result2, detectmode, img, camera_id, ori_hw, rtsp_ip,
rois, statis, line1, line2, is_resize
```

`img` 是 MPP JPEG 的 Base64。server 用 FastAPI BackgroundTasks 调用 `bg_task()`，并用锁保护 `last_alarm_time`。

### 9.2 告警图片

物理目录：

```text
/userdata/sps_test/iva-kyscene/data/alarm_picture/download/image/<camera_ip>/
```

MQTT 中保存相对路径 `/image/...`。Kyscene 前端通过 `/alarmImg/image/...` 读取，Nginx alias 指向同一数据目录。

### 9.3 摄像头封面和 ROI 底图

物理目录：

```text
/userdata/sps_test/iva-kyscene/data/alarm_picture/download/show_image/<camera_id>.jpg
```

刷新来源：

- engine 首帧；
- 默认每 300 秒；
- MQTT `web_picrefresh`；
- Kyscene `/kyscene/refreshcover?<deviceIp>` 代理到 Python `/refreshcover`。

封面先写隐藏临时文件，再 `os.replace()`，文件模式 0644、目录模式 0755。

Nginx 同时兼容：

```text
/cameraImg/<camera_id>.jpg
/kyscene/cameraImg/<camera_id>.jpg
```

## 10. 编译

### 10.1 板端依赖预检

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python

python3 tools/preflight_board.py
```

重点确认：

```bash
uname -m
python3 --version
ldconfig -p | grep -E 'rockchip_mpp|librga|libavformat|librknnrt'
ls -l /dev/rga /dev/mpp_service /dev/dri 2>/dev/null
```

### 10.2 编译 pybind11 模块

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python
bash scripts/build_on_board.sh
```

脚本等价于：

```bash
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --parallel
cmake --install build --prefix .
```

生成：

```text
rk_mpp_pipeline.cpython-310-aarch64-linux-gnu.so
vendor/lib/librknnrt_redirect.so
```

检查链接：

```bash
ldd rk_mpp_pipeline*.so | grep -E 'rockchip|rga|avformat|avcodec|avutil'
```

### 10.3 导入测试

```bash
export PROJECT_DIR=/userdata/sps_test/iva_python/duchun_rk3588_python
export PYTHONPATH="$PROJECT_DIR:$PROJECT_DIR/python"
set -a
. "$PROJECT_DIR/systemd/duchun-rk3588-python.env"
set +a

/userdata/sps_test/iva_python/python_mpp/.venv/bin/python -c \
  "import rk_mpp_pipeline; print(rk_mpp_pipeline.mpp_probe())"
```

## 11. 测试

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python
export PYTHONPATH="$PWD:$PWD/python"

/userdata/sps_test/iva_python/python_mpp/.venv/bin/python -m pytest -q \
  tests/test_preview.py \
  tests/test_business_compat.py \
  tests/test_yolo_lite.py
```

封面专项验证：

```bash
curl -i 'http://127.0.0.1/kyscene/refreshcover?127.0.0.1'
stat /userdata/sps_test/iva-kyscene/data/alarm_picture/download/show_image/<camera_id>.jpg
curl -I http://127.0.0.1/cameraImg/<camera_id>.jpg
curl -I http://127.0.0.1/kyscene/cameraImg/<camera_id>.jpg
```

检查 JPEG：

```bash
/userdata/sps_test/iva_python/python_mpp/.venv/bin/python - <<'PY'
import cv2
p = '/userdata/sps_test/iva-kyscene/data/alarm_picture/download/show_image/<camera_id>.jpg'
img = cv2.imread(p)
print(None if img is None else img.shape)
PY
```

## 12. 手工启动（仅调试）

正式环境由 systemd 管理。手工启动前必须先停止同名服务，避免重复占用 8899、RTSP、MPP 和 NPU：

```bash
sudo systemctl stop ai_engine.service ai_server.service

cd /userdata/sps_test/iva_python/duchun_rk3588_python
export PYTHONPATH="$PWD:$PWD/python"
set -a
. systemd/duchun-rk3588-python.env
set +a
```

终端 1：

```bash
/userdata/sps_test/iva_python/python_mpp/.venv/bin/python \
  -m uvicorn app.active_server:app --host 0.0.0.0 --port 8899
```

终端 2：

```bash
/userdata/sps_test/iva_python/python_mpp/.venv/bin/python -m app.active_engine
```

调试结束：

```bash
sudo systemctl start ai_server.service ai_engine.service
```

## 13. 部署

### 13.1 同步代码

开发机源码修改完成后，将变更同步到：

```text
/userdata/sps_test/iva_python/duchun_rk3588_python
```

不要覆盖：

```text
/userdata/sps_test/iva
```

### 13.2 备份

至少备份：

```text
app/
src/
systemd/
/etc/duchun-rk3588-python.env
/etc/systemd/system/ai_engine.service.d/
/etc/systemd/system/ai_server.service.d/
/etc/nginx/sites-enabled/iva-kyscene
```

### 13.3 安装配置

```bash
sudo install -m 600 systemd/duchun-rk3588-python.env \
  /etc/duchun-rk3588-python.env

sudo install -m 644 systemd/iva-kyscene.nginx.conf \
  /etc/nginx/sites-enabled/iva-kyscene

sudo install -m 644 systemd/iva-kyscene.nginx.conf \
  /etc/nginx/sites-available/iva-kyscene

sudo chmod 755 \
  /userdata/sps_test/iva-kyscene/data/alarm_picture/download/show_image

sudo nginx -t
sudo systemctl reload nginx.service
```

如修改了 C++ 扩展，先运行 `scripts/build_on_board.sh`。纯 Python 修改无需重新编译 `.so`。

### 13.4 重启顺序

```bash
sudo systemctl daemon-reload
sudo systemctl restart ai_server.service
sudo systemctl restart ai_engine.service
```

本项目正常部署不需要重启 Kyscene。确认：

```bash
systemctl is-active ai_server.service ai_engine.service kyscene.service
```

## 14. 日志与运行状态

### 14.1 systemd 日志

```bash
journalctl -u ai_engine.service -f
journalctl -u ai_server.service -f
journalctl -u kyscene.service -f
```

最近日志：

```bash
journalctl -u ai_engine.service -n 200 --no-pager
journalctl -u ai_server.service -n 200 --no-pager
```

### 14.2 engine 审计

```text
runtime/active/engine.jsonl
```

每条包含：

```text
camera_id, pts_us, result1, result2, statis_keys, image_bytes,
http_status, packets, decoded_frames, delivered_frames,
dropped_frames, errors, reconnects, state, last_error
```

查看：

```bash
tail -f runtime/active/engine.jsonl
```

关键判断：

- `packets` 增长但 `decoded_frames=0`：检查 MPP/码流格式。
- 两者都不增长：检查 RTSP 和 FFmpeg worker。
- `statis_keys=[]`：算法未加载或不在有效时段。
- `http_status=200`：engine 到 server 正常。
- `errors=0` 且 `state=running`：解码链正常。

### 14.3 MQTT

```bash
mosquitto_sub -h 127.0.0.1 -p 1883 -t alarm_result -v
mosquitto_sub -h 127.0.0.1 -p 1883 -t web_picrefresh -v
```

手工触发封面刷新：

```bash
mosquitto_pub -h 127.0.0.1 -p 1883 -q 1 \
  -t web_picrefresh -m '{"refresh":1}'
```

### 14.4 Kyscene 和 Nginx

```bash
tail -f /userdata/sps_test/iva-kyscene/data/logs/kyland-$(date +%F).0.log
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

## 15. 常见故障

| 现象 | 检查 | 处理 |
| --- | --- | --- |
| engine 重启循环，提示无 camera jobs | Redis key、相机是否缺少宽高 | 当前版本按首帧尺寸生成 geometry；确认部署了最新 `legacy_config.py` |
| 检测到 person 但 `statis_keys=[]` | `TZ`、有效时段、Redis 算法编号 | 保证 `TZ=Asia/Shanghai`，重启 engine |
| `/analyresult` 失败 | 8899、server journal | 启动 `ai_server`，检查 payload 校验错误 |
| MQTT 有告警但前端无图片 | `picUrl`、Nginx alias、文件权限 | `picUrl` 应为 `/image/...`，目录需 0755、图片 0644 |
| `/cameraImg` 404 | 文件、Nginx `^~` route | 检查 `show_image`，执行 `nginx -t` 后 reload |
| `/refreshcover` 404 | active server 版本 | 部署包含该接口的 `active_server.py` 并重启 server |
| 告警时间多 8 小时 | JVM/Python timezone | Python 与 Kyscene 均固定 `Asia/Shanghai` |
| RKNN static shape warning | 模型为静态输入 | 当前模型预期行为，可忽略；输入尺寸仍必须匹配 |
| `qf_min/qf_max` 或 `alloc_bits` | 旧 JPEG 编码器配置 | 重新编译最新 `rk_mpp_pipeline.cpp` |
| `mpp version: unknown` | BSP MPP 无 Git 元数据 | 信息日志；只要解码计数增长即可 |
| 内存持续增长 | MPP buffer 未释放或 Python 长期持帧 | 检查 buffer dump、latest slot 和服务重启前后的 RSS |

## 16. RTSP 与硬件专项调试

检查码流：

```bash
ffprobe -v error -rtsp_transport tcp \
  -select_streams v:0 \
  -show_entries stream=codec_name,width,height,r_frame_rate \
  -of json '<rtsp-url>'
```

检查 TCP：

```bash
ss -tn | grep ':554'
```

检查进程线程：

```bash
ENGINE_PID=$(systemctl show ai_engine.service -p MainPID --value)
ps -p "$ENGINE_PID" -o pid,etime,time,%cpu,%mem,nlwp,stat,cmd
ps -L -p "$ENGINE_PID" -o pid,tid,pcpu,stat,comm,wchan:24
```

典型线程包括 Python 主循环、FFmpeg worker、`mpp_dec_parser` 和 `mpp_dec_hal`。

## 17. 回滚

部署时产生的备份位于：

```text
/userdata/sps_test/iva_python/duchun_rk3588_python/backups/
```

回滚步骤：

1. 停止 `ai_engine` 和 `ai_server`。
2. 恢复备份的 `app/`、`.so`、环境文件和 Nginx 配置。
3. 执行 `sudo nginx -t`。
4. reload Nginx。
5. 启动 `ai_server`，再启动 `ai_engine`。
6. 检查 8899 healthcheck、engine JSONL 和 MQTT。

不要删除 `/userdata/sps_test/iva`，它仍是旧 C++ 版本的可恢复资产和当前 RKNN 模型来源。

## 18. 当前边界

- 当前正式模型覆盖主模型 person/head/helmet 与抽烟/手机二级模型。
- `business_rules.py` 中存在其他角色规则，但没有对应 RKNN 模型时不会产生结果。
- 配置变更目前在 engine 启动时读取；算法、相机或 RTSP 配置变化后需要重启 `ai_engine`。
- 告警画框、Base64 和数据库图片属于 CPU/文件 I/O，不是零拷贝路径。
- RKNNLite Python API 是 NumPy 输入接口，不支持直接提交 DMA-BUF fd。
