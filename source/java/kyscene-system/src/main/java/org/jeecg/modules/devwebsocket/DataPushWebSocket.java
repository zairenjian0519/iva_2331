/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.devwebsocket;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.dataPushHistory.entity.DataPushHistory;
import org.jeecg.modules.dataPushHistory.service.IPushHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 数据推送websocket
 *
 * @author： DecoderX
 * @date： 2025/4/25
 */
@Slf4j
@Component
@ServerEndpoint("/dataPushWebSocket/{userId}")
public class DataPushWebSocket {
    private static final CopyOnWriteArraySet<DataPushWebSocket> webSockets = new CopyOnWriteArraySet<>();
    private Session session;
    private String userId;

    private IPushHistoryService pushHistoryService;

    @Autowired
    public void setPushHistoryService(IPushHistoryService pushHistoryService) {
        this.pushHistoryService = pushHistoryService;
    }

    @OnOpen//处理建立连接的逻辑
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        try {
            this.session = session;
            this.userId = userId;
            webSockets.add(this);
            log.info("DataPushWebSocket 连接成功，userId: {}", userId);
        } catch (Exception e) {
            log.error("建立 DataPushWebSocket 连接失败，userId：{}", userId);
        }
    }

    public String getUserId() {
        return session.getId();
    }

    //关闭
    @OnClose
    public void onClose() {
        try {
            log.info("DataPushWebSocket 关闭，userId: {} ", this.userId);
            webSockets.remove(this);
        } catch (Exception e) {
            log.error("关闭 DataPushWebSocket 失败，userId：{}", this.userId);
        }
    }

    //处理收到的消息
    @OnMessage
    public void OnMessage(String message) {
        log.info("DataPushWebSocket 收到来自 userId - {} 的消息: {}", this.userId, message);
    }

    //向当前用户发送信息
    public void sendMessageToUser(String tempMessage) {
        String finalTempMessage = formatDataPushLog(tempMessage);
        saveLog(tempMessage);
        webSockets.forEach(ws -> {
            try {
                log.info("DataPushWebSocket 发送消息: {} 给 userId - {}", finalTempMessage, ws.userId);
                ws.session.getBasicRemote().sendText(finalTempMessage); //修复后台报错
            } catch (IOException e) {
                log.error("DataPushWebSocket 发送消息错误 IOException : {}", e.getMessage());
            }
        });
    }

    // 格式化数据推送日志，格式：时间-日志
    private String formatDataPushLog(String message) {
        String time = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        String finalTempMessage = time + "  " + message;
        // 转json字符串，格式：{msg: xxx}
        JSONObject msg = new JSONObject();
        msg.put("msg", finalTempMessage);
        return msg.toJSONString();
    }

    /**
     * 保存日志
     * @param log String
     */
    private void saveLog(String log){
        DataPushHistory dataPushHistory = new DataPushHistory();
        dataPushHistory.setMessage(log);
        pushHistoryService.save(dataPushHistory);
    }
}