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
 * 向前端发送实时报警信息
 */
@Slf4j
@Component
@ServerEndpoint("/alarmToWebSocket/{userId}")
public class AlarmToWebSocket {
    private static final CopyOnWriteArraySet<AlarmToWebSocket> webSockets = new CopyOnWriteArraySet<>();
    private Session session;
    private String userId;

    @OnOpen//处理建立连接的逻辑
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        try {
            this.session = session;
            this.userId = userId;
            webSockets.add(this);
            log.info(" AlarmToWebSocket 连接成功 : {}", userId);
        } catch (Exception e) {
            log.error("建立 AlarmToWebSocket 连接失败，userId - {}", userId);
        }
    }

    public String getUserId() {
        return session.getId();
    }

    @OnClose//关闭
    public void onClose() {
        try {
            log.info(" AlarmToWebSocket 关闭，userID : {}", this.userId);
            webSockets.remove(this);
        } catch (Exception e) {
            log.error("关闭 AlarmToWebSocket 失败，userId - {}", this.userId);
        }
    }

    @OnMessage//处理收到的消息
    public void OnMessage(String message) {
        log.info(" AlarmToWebSocket 收到来自userId - {} 的消息: {}", this.userId, message);
    }

    //向当前用户发送信息
    public void sendMessageToUser(String tempMessage) {
        webSockets.forEach(ws -> {
            try {
                log.info(" AlarmToWebSocket 发送消息: {} 给客户端: {}", tempMessage, ws.userId);
                ws.session.getBasicRemote().sendText(tempMessage); //修复后台报错
            } catch (IOException e) {
                log.error(" AlarmToWebSocket 发送消息错误 IOException : {}", e.getMessage());
            }
        });

    }
}
