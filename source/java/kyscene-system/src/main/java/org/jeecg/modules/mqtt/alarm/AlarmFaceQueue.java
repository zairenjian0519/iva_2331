/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.alarm;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
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
public class AlarmFaceQueue {

    // 控制最大缓存数量
    private final BlockingQueue<AlarmDetail> queue = new LinkedBlockingQueue<>(32) {
        @Override
        public boolean offer(@NotNull AlarmDetail event) {
            if (remainingCapacity() <= 0) {
                AlarmDetail oldest = poll();
                if (oldest != null) {
                    log.debug("AlarmFaceQueue is full, remove oldest event: {}", oldest.getCameraName());
                }
            }
            return super.offer(event);
        }
    };

    // 非阻塞入队
    public void addEvent(AlarmDetail event) {
        queue.offer(event);
    }

    public List<AlarmDetail> drainEvents(int maxElements) {
        List<AlarmDetail> list = new ArrayList<>();
        queue.drainTo(list, maxElements); // 批量拉取
        return list;
    }

    public int size() {
        return queue.size();
    }
}
