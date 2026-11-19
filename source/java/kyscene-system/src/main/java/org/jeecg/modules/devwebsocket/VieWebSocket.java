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
 * 向前端发送视频播放是否正常的消息
 */
@Slf4j
@Component
@ServerEndpoint("/ViWebSocket/{userId}")
public class VieWebSocket {
    private static final CopyOnWriteArraySet<VieWebSocket> webSockets = new CopyOnWriteArraySet<>();
    private Session session;
    private String userId;

    @OnOpen//处理建立连接的逻辑
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        try {
            this.session = session;
            this.userId = userId;
            webSockets.add(this);
            log.info("ViWebSocket连接成功 : {}", userId);
        } catch (Exception e) {
            log.error("建立ViWebSocket连接失败，userId - {}", userId);
        }
    }

    public String getUserId() {
        return session.getId();
    }

    @OnClose//关闭
    public void onClose() {
        try {
            log.info("ViWebSocket关闭，userID : {}", this.userId);
            webSockets.remove(this);
        } catch (Exception e) {
            log.error("关闭ViWebSocket失败，userId - {}", this.userId);
        }
    }

    @OnMessage//处理收到的消息
    public void OnMessage(String message) {
        log.info("ViWebSocket收到来自userId - {} 的消息: {}", this.userId, message);
    }

    //向当前用户发送信息
    public void sendMessageToUser(String message) {
        webSockets.forEach(ws -> {
            try {
                log.info("ViWebSocket 发送消息: {} 给客户端: {}", message, ws.userId);
                ws.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("ViWebSocket 发送消息错误IOException : {}", e.getMessage());
            }
        });
    }
}