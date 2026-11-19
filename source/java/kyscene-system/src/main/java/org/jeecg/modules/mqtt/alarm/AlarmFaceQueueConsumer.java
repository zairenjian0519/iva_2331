/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.mqtt.handler.AlarmFaceHandler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author： DecoderX
 * @date： 2025/12/3
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AlarmFaceQueueConsumer {

    private final AlarmFaceQueue alarmFaceQueue;

    private final AlarmFaceHandler alarmFaceHandler;

    @PostConstruct
    public void startConsumer() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                List<AlarmDetail> batch = alarmFaceQueue.drainEvents(32); // 一次拉32条
                if (!batch.isEmpty()) {
                    // 批量写入数据库
                    alarmFaceHandler.handleBatchData(batch);
                }
            } catch (Exception e) {
                log.error("AlarmFace 批量写入数据库失败", e);
            }

        }, 0, 3, TimeUnit.SECONDS); // 每3秒处理一次
    }
}
