/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.cameraManager.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.model.CommandResult;
import org.jeecg.modules.system.util.CmdUti;

/**
 * @author： DecoderX
 * @date： 2025/9/1
 */
@Slf4j
public class CameraStatusCheckerNew {

    private static final String CHECK_SHELL_PATH = "/home/super/web/camera_check";

    public static JSONObject check(String rtspUrl, String cameraId) {
        try {
            JSONObject params = new JSONObject();
            params.put("rtsp_url", rtspUrl);
            params.put("camera_id", cameraId);
            // params.put("skip_frames", 10);
            // params.put("skip_keyframes", 1);
            // params.put("max_attempts", 50);
            // params.put("only_keyframe", false);
            CommandResult commandResult = CmdUti.executeCmd2(new String[]{CHECK_SHELL_PATH, params.toJSONString()});
            return JSONObject.parseObject(commandResult.getOutput());
        }catch (Exception e) {
            log.error("检查相机状态异常：{}", e.getMessage());
        }

        return null;
    }
}
