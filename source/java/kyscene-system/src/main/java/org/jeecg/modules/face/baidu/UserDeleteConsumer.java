/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.face.remoteSync.SyncState;
import org.jeecg.modules.face.remoteSync.SyncStateEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author： DecoderX
 * @date： 2025/8/7
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class UserDeleteConsumer {

    private final UserDeleteQueue userDeleteQueue;

    private final UserManagerService userManagerService;

    private final SyncStateEventPublisher syncStateEventPublisher;

    @PostConstruct
    public void startConsumer() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            // 检测队列是否为空, 发布同步状态事件
            SyncState syncState = new SyncState();
            syncState.setType("delete");
            syncState.setUserDeleteQueueEmpty(userDeleteQueue.size() == 0);
            syncStateEventPublisher.publishSyncStateEvent(syncState);

            Map<String, String> event = userDeleteQueue.get();
            try {
                if (event != null && !event.isEmpty()) {
                    log.info("开始删除云用户：{}", event);
                    Result<?> result = userManagerService.deleteUser(event.get("groupId"), event.get("userId"));
                    if (result.isSuccess()) {
                        log.info("删除云用户成功");
                    } else {
                        log.error("删除云用户失败：{}", result.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("人脸用户删除异常", e);
                // 放入队列重试删除，重试次数超过2次后放弃
                int retryCount = Integer.parseInt(event.getOrDefault("retryCount", "0"));
                if (retryCount < 2) {
                    event.put("retryCount", String.valueOf(retryCount + 1));
                    userDeleteQueue.addEvent(event);
                } else {
                    log.error("删除用户重试超过2次，放弃：{}", event);
                }
            }
        }, 0, 300, TimeUnit.MILLISECONDS); // 执行完单个任务后延迟300毫秒执行下个任务
    }
}
