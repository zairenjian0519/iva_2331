/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.face.entity.FaceDetailDo;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author： DecoderX
 * @date： 2025/8/7
 */
@Slf4j
@Component
public class FaceUploadQueue {
    // 控制最大缓存数量
    private final BlockingQueue<FaceDetailDo> queue = new LinkedBlockingQueue<>(5000) {
        @Override
        public boolean offer(@NotNull FaceDetailDo event) {
            if (remainingCapacity() <= 0) {
                FaceDetailDo oldest = poll();
                if (oldest != null) {
                    log.debug("FaceUploadQueue is full, remove oldest event: {}", oldest.getVectorId());
                }
            }
            return super.offer(event);
        }
    };

    // 非阻塞入队
    public void addEvent(FaceDetailDo event) {
        queue.offer(event);
    }

    public List<FaceDetailDo> drainEvents(int maxElements) {
        List<FaceDetailDo> list = new ArrayList<>();
        queue.drainTo(list, maxElements); // 批量拉取
        return list;
    }

    public FaceDetailDo get() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }
}
