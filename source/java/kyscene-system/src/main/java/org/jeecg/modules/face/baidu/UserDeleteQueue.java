/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author： DecoderX
 * @date： 2025/8/7
 */
@Slf4j
@Component
public class UserDeleteQueue {
    // 控制最大缓存数量
    private final BlockingQueue<Map<String, String>> queue = new LinkedBlockingQueue<>(500) {
        @Override
        public boolean offer(@NotNull Map<String, String> event) {
            if (remainingCapacity() <= 0) {
                Map<String, String> oldest = poll();
                if (oldest != null) {
                    log.debug("UserDeleteQueue is full, remove oldest event: {}", oldest);
                }
            }
            return super.offer(event);
        }
    };

    // 非阻塞入队
    public void addEvent(Map<String, String> event) {
        queue.offer(event);
    }

    public List<Map<String, String>> drainEvents(int maxElements) {
        List<Map<String, String>> list = new ArrayList<>();
        queue.drainTo(list, maxElements); // 批量拉取
        return list;
    }

    public Map<String, String> get() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }
}
