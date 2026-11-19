package org.jeecg.modules.devwebsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Elmer on 2022/9/2.
 */
@Component
@Slf4j
@ServerEndpoint("/devWebsocket/{userId}")
public class DeviceWebSocket {
    private static final CopyOnWriteArraySet<DeviceWebSocket> webSockets = new CopyOnWriteArraySet<>();
    private Session session;
    private String userId;

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        this.session = session;
        this.userId = userId;
        webSockets.add(this);
        log.info(" DeviceWebSocket 连接成功 : {}", userId);
    }

    @OnClose
    public void onClose() {
        try {
            log.info(" DeviceWebSocket 关闭，userID : {}", this.userId);
            webSockets.remove(this);
        } catch (Exception e) {
            log.error("关闭 DeviceWebSocket 失败，userId - {}", this.userId);
        }
    }

    @OnMessage
    public void OnMessage(String message) {
        log.info(" DeviceWebSocket 收到来自userId - {} 的消息: {}", this.userId, message);
    }

    public void pushMessage(String message) {
        webSockets.forEach(ws -> {
            try {
                log.info(" DeviceWebSocket 发送消息: {} 给客户端: {}", message, ws.userId);
                ws.session.getBasicRemote().sendText(message); //修复后台报错
            } catch (IOException e) {
                log.error("发送消息错误IOException : {}", e.getMessage());
            }
        });

    }
}