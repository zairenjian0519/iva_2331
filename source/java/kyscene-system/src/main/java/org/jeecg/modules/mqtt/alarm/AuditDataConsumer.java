/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.alarmAudit.entity.AlarmAudit;
import org.jeecg.modules.alarmAudit.service.IAlarmAuditService;
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
public class AuditDataConsumer {

    private final AuditDataQueue auditDataQueue;

    private final IAlarmAuditService alarmAuditService;

    @PostConstruct
    public void startConsumer() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                List<AlarmAudit> batch = auditDataQueue.drainEvents(32); // 一次拉32条
                if (!batch.isEmpty()) {
                    alarmAuditService.saveBatch(batch); // 批量写入数据库
                }
            }catch (Exception e) {
                log.error("Audit 批量写入数据库失败", e);
            }
        }, 0, 3, TimeUnit.SECONDS); // 每3秒处理一次
    }
}
