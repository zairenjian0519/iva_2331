package org.jeecg.modules.thirdApi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.CmdUti;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.zlmediakit.StreamMediaConst;
import org.jeecg.modules.zlmediakit.StreamMediaServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 视频流请求处理
 */
@Component
@Slf4j
public class CameraStreamHandler {
    private static final String STREAM_TRANSCODER_ENV = "KYSCENE_STREAM_TRANSCODER";
    private static final long STREAM_START_TIMEOUT_MS = 12_000L;
    private static final long STREAM_POLL_INTERVAL_MS = 250L;

    @Autowired
    private StreamMediaServerConfig mediaServerConfig;

    /**
     * 开启视频流
     */
    public Result<?> startCameraStream(JSONObject requestParams) {
        String streamId = requestParams.getString("cameraId");
        if (streamId == null) return Result.error("参数有误");
        try {
            Result<?> result1 = getStreamServerMediaList();
            if (ObjectUtil.notEqual(result1.getCode(), CommonConstant.SC_OK_200)) return result1;
            if (containsStream(result1, streamId)) {
                JSONObject result = new JSONObject();
                result.put("success", true);
                return Result.OK(result);
            }
            // 开启代理, streamId = cameraId
            String streamPushAddress = getStreamPushAddress(streamId);
            if (streamPushAddress == null) return Result.error("没有在摄像机表中查询到推流地址");
            Result<?> result2 = addCameraStreamProxy(streamId, streamPushAddress);
            if (ObjectUtil.notEqual(result2.getCode(), CommonConstant.SC_OK_200)) return result2;
            return Result.OK();
        } catch (Exception e) {
            log.error("开启流代理失败：{}", e.getLocalizedMessage());
        }
        return Result.error("开启流代理失败, 相机id: " + streamId);
    }

    /**
     * 关闭视频流
     */
    public Result<?> stopCameraStream(JSONObject params) {
        String streamId = params.getString("cameraId");
        if (StrUtil.isBlank(streamId)) return Result.error("相机id不能为空");
        CmdUti.stopRockchipStream(streamId);
        String url = getMediaServerHttpUrl() + StreamMediaConst.CLOSE_STREAM;
        try {
            JSONObject reqParams = new JSONObject();
            reqParams.put("secret", mediaServerConfig.getSecret());
            reqParams.put("stream", streamId);
            String responseStr = HttpClientUtil.requestPostJson(url, reqParams);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int code = responseObj.getIntValue("code");
            if (code != 0) return Result.error(responseObj.getString("msg"));
            return Result.ok();
        } catch (Exception e) {
            log.error("关闭视频流失败，请确认流媒体服务是否正常");
        }
        return Result.error("关闭视频流失败，请确认流媒体服务是否正常");
    }

    /**
     * 关闭所有视频流
     */
    public Result<?> closeAllStream() {
        CmdUti.stopAllRockchipStreams();
        String url = getMediaServerHttpUrl() + StreamMediaConst.CLOSE_STREAM;
        try {
            JSONObject reqParams = new JSONObject();
            reqParams.put("secret", mediaServerConfig.getSecret());
            String responseStr = HttpClientUtil.requestPostJson(url, reqParams);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int code = responseObj.getIntValue("code");
            if (code != 0) return Result.error(responseObj.getString("msg"));
            return Result.ok("成功关闭 " + responseObj.getIntValue("count_closed") + " 个视频流");
        } catch (Exception e) {
            log.error("关闭视频流失败，请确认流媒体服务是否正常");
        }
        return Result.error("关闭视频流失败，请确认流媒体服务是否正常");
    }


    /**
     * 根据设备ip及系统参数配置获取流播放地址webrtc url
     * http://${deviceIp}:8080/index/api/webrtc?app=live&stream=${cameraId}&type=play
     */
    public Result<?> getWebRtcUrl(String deviceIp, String cameraId) {
        Integer port = 8080;
        SysParameter sysParameter = SysParameterUtil.getSysParameter("StreamBusinessIpConfig");
        if (sysParameter != null) {
            try {
                String paramVal = sysParameter.getParamVal();
                JSONObject json = JSONObject.parseObject(paramVal);
                Boolean enable = json.getBoolean("enable");
                if (enable) {
                    deviceIp = json.getString("remoteIp");
                    port = json.getInteger("remotePort");
                }
            } catch (Exception e) {
                return Result.error("message", "查询到返回流地址配置参数，但配置异常，请检查");
            }
        }
        String webrtcUrl = "http://" + deviceIp + ":" + port + "/index/api/webrtc?app=live&stream=" + cameraId + "&type=play";
        return Result.OK(webrtcUrl);
    }

    private Result<?> addCameraStreamProxy(String streamId, String streamUrl) {
        String url = getMediaServerHttpUrl() + StreamMediaConst.DIRECT_STREAM_PROXY;
        JSONObject params = new JSONObject();
        params.put("secret", mediaServerConfig.getSecret());

        String streamFormat = getStreamFormat(streamId);
        log.info("视频流添加代理，streamId: {}, streamFormat: {}, streamUrl: {}",
                streamId, streamFormat, CmdUti.maskStreamUrl(streamUrl));

        // 获取设备类型
        String deviceCategory = SysParameterUtil.getDeviceCategory();
        if (StrUtil.isBlank(deviceCategory)) return Result.error("设备分类为空");

        // 提前编码 URL 避免重复计算
        String encodedStreamUrl = URLUtil.encode(streamUrl);
        String rtspAddress = "rtsp://127.0.0.1/live/" + streamId;

        // 编码格式匹配
        boolean equalH264 = StreamMediaConst.STREAM_FORMAT_H264.equalsIgnoreCase(streamFormat);

        // RK3588 显式配置优先于历史设备分类，避免复用数据库时误入灵犀转码路径。
        String configuredTranscoder = System.getenv(STREAM_TRANSCODER_ENV);
        boolean autoTranscoder = StrUtil.isBlank(configuredTranscoder) || "auto".equalsIgnoreCase(configuredTranscoder);
        boolean useRockchipTranscoder = !equalH264 &&
                ("rockchip".equalsIgnoreCase(configuredTranscoder) ||
                        (autoTranscoder && "N50".equalsIgnoreCase(deviceCategory)));
        if (useRockchipTranscoder) {
            String rtmpAddress = "rtmp://127.0.0.1/live/" + streamId;
            startStreamThread("RockchipStream-" + streamId,
                    () -> CmdUti.startRockchipStream(streamId, streamUrl, rtmpAddress, streamFormat));
            return waitForStreamRegistration(streamId);
        }

        // N200设备，使用ffmpeg代理
        boolean n200Device = "N200".equals(deviceCategory);
        if (n200Device) {
            startStreamThread("LynxiStreamThread", () -> CmdUti.startStream(encodedStreamUrl, rtspAddress, streamFormat));
            return Result.ok();
        }

        // N2110 设备如果不是H264，使用ffmpeg代理
        boolean n2110NoEqualH264 = "N2110".equals(deviceCategory) && !equalH264;
        if (n2110NoEqualH264) {
            startStreamThread("LynxiStreamThread", () -> CmdUti.startStream(encodedStreamUrl, rtspAddress, streamFormat));
            return Result.ok();
        }

        // N150 设备如果不是H264，使用ffmpeg代理
        boolean n150NoEqualH264 = "N150".equals(deviceCategory) && !equalH264;
        if (n150NoEqualH264) {
            startStreamThread("N150H265StreamThread", () -> CmdUti.startStream(encodedStreamUrl, rtspAddress, streamFormat));
            return Result.ok();
        }

        if (equalH264) {
            // 直接代理，webrtc可直接播放
            params.put("vhost", "__defaultVhost__");
            params.put("app", "live");
            params.put("stream", streamId);
            // ZLMediaKit expects the source URL as a URL value in the JSON body.
            // Encoding it here makes the schema become "rtsp%3A%2F%2F...".
            params.put("url", streamUrl);
            // params.put("auto_close", true);
        } else {
            // 不在特殊设备中，使用ffmpeg代理,支持H265
            url = getMediaServerHttpUrl() + StreamMediaConst.FFMPEG_STREAM_PROXY;
            setupFfmpegProxyParams(params, streamUrl, streamId);
        }

        try {
            String responseStr = HttpClientUtil.requestPostJson(url, params);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int code = responseObj.getIntValue("code");
            if (code != 0) return Result.error(responseObj.getString("msg"));
            // 返回的是流的唯一标识 { "key" : ""}
            return Result.OK(responseObj.getJSONObject("data"));
        } catch (Exception e) {
            log.error("开启视频流失败，请确认流媒体服务是否正常");
            return Result.error("开启视频流失败，请确认流媒体服务是否正常");
        }
    }

    private void setupFfmpegProxyParams(JSONObject params, String streamUrl, String streamId) {
        params.put("src_url", streamUrl);
        params.put("dst_url", "rtsp://127.0.0.1/live/" + streamId);
        params.put("timeout_ms", 10 * 1000);
        params.put("enable_hls", false);
        params.put("enable_mp4", false);
    }

    private Result<?> waitForStreamRegistration(String streamId) {
        long deadline = System.currentTimeMillis() + STREAM_START_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Result<?> mediaList = getStreamServerMediaList();
            if (containsStream(mediaList, streamId)) {
                JSONObject result = new JSONObject();
                result.put("success", true);
                return Result.OK(result);
            }
            try {
                Thread.sleep(STREAM_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                CmdUti.stopRockchipStream(streamId);
                return Result.error("等待视频流启动时被中断, 相机id: " + streamId);
            }
        }
        CmdUti.stopRockchipStream(streamId);
        return Result.error("RKMPP 转码启动超时, 相机id: " + streamId);
    }

    private boolean containsStream(Result<?> mediaList, String streamId) {
        if (mediaList == null || ObjectUtil.notEqual(mediaList.getCode(), CommonConstant.SC_OK_200)) return false;
        JSONArray jsonArray = JSON.parseArray(JSON.toJSONString(mediaList.getResult()));
        if (jsonArray == null) return false;
        for (int i = 0; i < jsonArray.size(); i++) {
            if (streamId.equals(jsonArray.getJSONObject(i).getString("stream"))) return true;
        }
        return false;
    }

    // 获取流媒体服务上的流列表
    private Result<?> getStreamServerMediaList() {
        String url = getMediaServerHttpUrl() + StreamMediaConst.GET_MEDIA_LIST_URI;
        JSONObject params = new JSONObject();
        params.put("secret", mediaServerConfig.getSecret());
        try {
            String responseStr = HttpClientUtil.requestPostJson(url, params);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int code = responseObj.getIntValue("code");
            if (code != 0) return Result.error(responseObj.getString("msg"));
            return Result.OK(responseObj.getJSONArray("data"));
        } catch (Exception e) {
            log.error("获取流列表失败，请确认流媒体是否正常");
            return Result.error("获取流列表失败，请确认流媒体是否正常");
        }
    }

    // 获取流媒体服务器请求的Base Url
    private String getMediaServerHttpUrl() {
        return "http://" + mediaServerConfig.getIpAddress() + ":" + mediaServerConfig.getRestApiPort();
    }

    // 根据流id去查询stream push address
    private String getStreamPushAddress(String cameraId) {
        CameraManager cameraManager = getCameraInfo(cameraId);
        if (cameraManager != null) return cameraManager.getStreamAddress();
        return null;
    }

    private String getStreamFormat(String cameraId) {
        CameraManager cameraManager = getCameraInfo(cameraId);
        if (cameraManager != null) return cameraManager.getStreamFormat();
        return null;
    }

    private CameraManager getCameraInfo(String cameraId) {
        ICameraManagerService cameraManagerService = SpringContextUtils.getBean(ICameraManagerService.class);
        return cameraManagerService.getById(cameraId);
    }

    // 封装线程启动逻辑，增加线程名和异常捕获
    private void startStreamThread(String threadName, Runnable task) {
        new Thread(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("视频流代理线程异常，线程名：{}", threadName, e);
            }
        }, threadName).start();
    }

}
