# Lynxi 与 RK3588 Python 业务处理对比

## 范围

灵犀基线：`/home/forlinx/Projects/RK3588/iva/duchen`。

RK3588 Python：`/userdata/sps_test/iva_python/duchun_rk3588_python`。

本轮只修改 RK3588 Python Engine，覆盖算法 `0/1、4/5、6/7、10-13、29/30/31`。

## 1. RTSP 拉流

灵犀使用 `lyn_demux_open/read_packet`，每摄像头有独立发送和接收线程。发送线程按
`fps_reduce_rate=12` 调用 `lyn_vdec_send_skipped_packet_async` 跳过压缩包。

RK3588 使用 FFmpeg `avformat_open_input/av_read_frame` 通过 RTSP/TCP 解复用，H.264/H.265
经过 Annex-B bitstream filter 后全部送入 MPP。FFmpeg 不执行软件视频解码。

RK3588 保留全量拉流，并记录 packet 数量、packet 字节数、packet FPS 和 Mbps。

## 2. 图像解码与队列

灵犀使用 Lynxi VDEC 硬件解码，解码帧进入容量为 2 的阻塞队列。生产者在队列满时等待。

RK3588 使用 Rockchip MPP 硬件解码为 NV12/NV21。每路只有一个 latest-frame 槽位；消费者
没有取走旧帧时，新帧覆盖旧帧并增加 `dropped_frames`，不会积累延迟。

MPP 统计增加 `packet_bytes` 和 `consumed_frames`，可分别计算拉流、解码、Python 消费和丢帧速率。

## 3. 模型初始化与摄像头并发

灵犀在 `create_threads()` 中创建一个全局 `TRTInfer`，所有 `VideoScreen` 共享模型。每摄像头
有独立推理线程，但 JPEG、主模型和二级模型被一个全局 `RLock` 包围，实际推理串行；同时
每路硬限制为 2 FPS。

RK3588 创建一个全局 `TRTInferCompat`，按所有摄像头算法并集预加载需要的模型。每摄像头
一个 worker；模型实例全局共享。每个 RKNNLite context 有独立锁，只串行化同一模型的
`inference()`，不同模型可以并行。

所有 RKNNLite context 使用 `NPU_CORE_0_1_2`。具体三核拆分由 RKNN runtime 决定，程序不做
显式 per-core 调度。

## 4. 模型预处理

共同语义为：原始 NV12 -> crop/resize -> 等比例 letterbox -> padding 114 -> RGB uint8。

- 灵犀由 IPE 完成。
- RK3588 由 RGA 直接使用 MPP DMA-BUF 完成。
- RKNNLite 公共接口接收 NumPy NHWC 输入，NumPy 到 NPU 是数据传输边界。

主模型和算法 29/30/31 使用整帧。抽烟电话和反光衣使用主模型候选产生的 ROI。

## 5. YOLOv5 后处理与参数

灵犀插件和 RK3588 Python 都计算 `objectness * class_score`，按类别执行 NMS，并还原
letterbox 坐标。区别是灵犀使用设备端 FP16 插件，RK3588 在 CPU 上解码三个 raw head。

| role | 灵犀内部 confidence/NMS | RK3588 默认值 | 最终业务阈值 |
| --- | --- | --- | --- |
| person_helmet | 0.5 / 0.5 | 0.5 / 0.5 | Redis algorithm_threshold |
| smoking_phone | 0.3 / 0.5 | 0.3 / 0.5 | Redis algorithm_threshold |
| reflective_clothes | 0.5 / 0.5 | 0.5 / 0.5 | Redis algorithm_threshold |
| oil_leak | 无灵犀基线 | 0.5 / 0.45 | Redis algorithm_threshold |
| uniform | 无灵犀基线 | 0.5 / 0.45 | Redis algorithm_threshold |
| pipe_oil_leak | 无灵犀基线 | 0.5 / 0.45 | Redis algorithm_threshold |

内部阈值只生成候选；前端阈值只在业务出口过滤，不覆盖内部 NMS 参数。

## 6. 模型关联规则

### 主模型

`head -> 4`，`helmet -> 5`，`person` 用于人员统计和 person 类二级模型。人员及安全帽保留
灵犀最小宽高比例和 invalid ROI 过滤。

### 抽烟与打电话

严格按灵犀当前代码：对主模型中非 `person` 的 head/helmet 框左右各扩半框宽，向下扩
`0.7 * 框高`。同一 ROI 必须同时出现 `hand + smoking` 或 `hand + phone` 才报警。

### 反光衣

对合格 person 框左右各扩 10 像素。优先选择面积不少于 ROI 10% 的最高分候选，若全部
过小则回退到全部候选最高分。`uncertain` 忽略，告警坐标使用原 person 框。

### 漏油、工服、管道漏油

29 和 31 是全帧直接检测。30 使用 `workwear` 框中心是否落入 `person` 框判断该人员是否
穿工服。这三类是 RK3588 扩展，没有灵犀结果可直接对照。

## 7. 参数获取

两边都从 Redis DB 1 的双层 JSON 读取 `camera_list_info` 和 `algorithm_list_info`，将前端百分比
阈值除以 100，并按真实帧尺寸缩放 camera ROI 和 invalid ROI。

Redis 提供有效时段、最终阈值、告警等级、告警间隔、目标数量等业务参数；RK3588 的
`models/initial_models.json` 额外提供模型路径、输入尺寸、labels、anchors、stride、内部
confidence 和 NMS IoU。

配置仍在 Engine 启动时读取，前端变更后通过现有 `engine_restart.sh` 重启生效。

## 相同业务出口

两边保持相同的 `result1/result2`、`/analyresult`、`bg_task()`、电子围栏、告警间隔、图片、
MQTT 和前端数据格式。RK3588 Engine 的线程、统计与模型锁修改不改变该契约。

## 未迁移算法

算法 `2/3/8/9/14/15/16/19-28` 在当前开发板没有完整的对应 RKNN 模型和推理实现。本轮不
伪造支持；后续应逐模型完成转换、预处理、后处理、关联规则和对照样本验收。
