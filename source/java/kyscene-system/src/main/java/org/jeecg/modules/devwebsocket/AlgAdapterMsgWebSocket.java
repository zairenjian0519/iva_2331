
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

@Slf4j
@Component
@ServerEndpoint("/algorithm/adapter/{userId}") // 修复路径拼写错误
public class AlgAdapterMsgWebSocket {
    private static final CopyOnWriteArraySet<AlgAdapterMsgWebSocket> webSockets = new CopyOnWriteArraySet<>();
    private Session session;
    private String userId;

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        try {
            this.session = session;
            this.userId = userId;
            webSockets.add(this);
            log.info("AlgAdapterMsgWebSocket连接成功 : {}", userId);
        } catch (Exception e) {
            log.error("建立websocket连接失败，userId - {}", userId, e);
        }
    }

    @OnClose
    public void onClose() {
        try {
            log.info("AlgAdapterMsgWebSocket关闭，userID : {}", this.userId);
            webSockets.remove(this);
        } catch (Exception e) {
            log.error("关闭AlgAdapterMsgWebSocket失败，userId - {}", this.userId, e);
        }
    }

    @OnMessage
    public void OnMessage(String message) {
        log.info("AlgAdapterMsgWebSocket收到来自userId - {} 的消息: {}", this.userId, message);
    }

    // 向当前用户发送信息
    public void sendMessageToUser(String tempMessage) {
        if (tempMessage == null || tempMessage.isEmpty()) {
            log.warn("尝试发送空消息，已忽略");
            return;
        }

        webSockets.forEach(ws -> {
            try {
                log.info("AlgAdapterMsgWebSocket 发送消息: {} 给客户端: {}", tempMessage, ws.userId); // 修复日志中的 userId
                sendTextMessage(ws.session, tempMessage);
            } catch (IOException e) {
                log.error("发送消息错误IOException : {}", e.getMessage(), e);
            }
        });
    }

    // 封装发送逻辑，便于扩展
    private void sendTextMessage(Session session, String message) throws IOException {
        session.getBasicRemote().sendText(message);
    }
}