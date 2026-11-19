package org.jeecg.modules.devwebsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.CopyOnWriteArraySet;


@Component
@Slf4j
@ServerEndpoint("/streamWebsocket/{userId}")
public class ViewWebSocket {
    private static final CopyOnWriteArraySet<org.jeecg.modules.devwebsocket.ViewWebSocket> webSockets = new CopyOnWriteArraySet<>();
    private Session session;
    private String userId;

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        this.session = session;
        this.userId = userId;
        webSockets.add(this);
        log.info("ViewWebSocket 连接成功 : {}", userId);
    }

    @OnClose
    public void onClose() {
        try {
            log.info("ViewWebSocket 关闭，userID : {}", this.userId);
            webSockets.remove(this);
        } catch (Exception e) {
            log.error("关闭 ViewWebSocket 失败，userId - {}", this.userId);
        }
    }

    @OnMessage
    public void OnMessage(String message) {
        log.info("ViewWebSocket 收到来自userId - {} 的消息: {}", this.userId, message);
    }

    public void pushMessage(String message) {
        try {
            webSockets.forEach(ws -> {
                log.info(" ViewWebSocket 发送消息: {} 给客户端: {}", message, ws.userId);
                ws.session.getAsyncRemote().sendText(message);
            });
        } catch (Exception e) {
            log.error("ViewWebSocket 发送消息错误IOException : {}", e.getMessage());
        }
    }
}