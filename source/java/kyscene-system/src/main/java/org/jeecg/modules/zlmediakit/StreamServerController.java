package org.jeecg.modules.zlmediakit;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.thirdApi.CameraStreamHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * zlmediakit 接口调用
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/stream/server")
public class StreamServerController {

    private final CameraStreamHandler cameraStreamHandler;

    /**
     * 添加流代理
     * 添加成功后播放地址：rtmp://盒子ip/live/cameraId
     */
    @PostMapping("/addStreamProxy")
    public Result<?> addStreamProxy(@RequestBody JSONObject requestParams) {
        Result<?> result = cameraStreamHandler.startCameraStream(requestParams);
        if (result.isSuccess()) {
            JSONObject data = new JSONObject();
            data.put("success", true);
            return Result.OK(data);
        }
        return result;
    }

}