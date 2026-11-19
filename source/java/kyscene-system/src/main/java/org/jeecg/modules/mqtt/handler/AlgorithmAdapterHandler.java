/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.devwebsocket.AlgAdapterMsgWebSocket;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/7/22
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AlgorithmAdapterHandler {

    private final AlgAdapterMsgWebSocket algAdapterMsgWebSocket;

    public void handle(String message) {
        log.info("收到算法调试消息：{}", message);
        if (StrUtil.isNotBlank(message)) {
            algAdapterMsgWebSocket.sendMessageToUser(parseMsg(message));
        }
    }

    private String parseMsg(String msg) {
        JSONObject json = new JSONObject();
        json.put("msg", msg);
        return json.toJSONString();
    }
}