/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.alarm;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.alarmAudit.entity.AlarmAudit;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author： DecoderX
 * @date： 2025/7/28
 */
@Slf4j
@Component
public class AuditDataQueue {
    // 控制最大缓存数量
    private final BlockingQueue<AlarmAudit> queue = new LinkedBlockingQueue<>(32) {
        @Override
        public boolean offer(@NotNull AlarmAudit event) {
            if (remainingCapacity() <= 0) {
                AlarmAudit oldest = poll();
                if (oldest != null) {
                    log.debug("AlarmAuditDataQueue is full, remove oldest event: {}", oldest);
                }
            }
            return super.offer(event);
        }
    };

    // 非阻塞入队
    public void addEvent(AlarmAudit event) {
        queue.offer(event);
    }

    public List<AlarmAudit> drainEvents(int maxElements) {
        List<AlarmAudit> list = new ArrayList<>();
        queue.drainTo(list, maxElements); // 批量拉取
        return list;
    }

    public int size() {
        return queue.size();
    }
}
