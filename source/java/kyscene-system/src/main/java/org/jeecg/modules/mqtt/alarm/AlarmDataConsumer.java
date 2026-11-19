/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.alarm;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;
import org.jeecg.modules.devwebsocket.AlarmToWebSocket;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author： DecoderX
 * @date： 2025/7/28
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AlarmDataConsumer {

    private final AlarmDataQueue alarmDataQueue;

    private final IAlarmDetailService alarmDetailService;

    private final AlarmToWebSocket alarmToWebSocket;

    @PostConstruct
    public void startConsumer() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                List<AlarmDetail> batch = alarmDataQueue.drainEvents(32); // 一次拉32条
                if (!batch.isEmpty()) {
                    boolean saved = alarmDetailService.saveBatch(batch); // 批量写入数据库
                    if (!saved) {
                        log.error("AlarmData 批量写入数据库失败，batchSize={}", batch.size());
                        return;
                    }
                    JSONObject message = new JSONObject();
                    message.put("event", "alarm_persisted");
                    message.put("count", batch.size());
                    alarmToWebSocket.sendMessageToUser(message.toJSONString());
                }
            } catch (Exception e) {
                log.error("AlarmData 批量写入数据库失败", e);
            }

        }, 0, 3, TimeUnit.SECONDS); // 每3秒处理一次
    }
}
