/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.face.entity.FaceDetailDo;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecg.modules.face.service.FaceGroupService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author： DecoderX
 * @date： 2025/8/7
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FaceDeleteConsumer {

    private final FaceDeleteQueue faceDeleteQueue;

    private final UserManagerService userManagerService;

    private final FaceGroupService faceGroupService;


    @PostConstruct
    public void startConsumer() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            FaceDetailDo event = faceDeleteQueue.get();
            try {
                if (event != null && StrUtil.isNotBlank(event.getId())) {
                    log.info("开始删除人脸：{}", event.getVectorId());
                    FaceGroupDo faceGroupDo = faceGroupService.getById(event.getFaceGroup());
                    Result<?> result = userManagerService.deleteFace(faceGroupDo.getGroupId(), event.getVectorId(), event.getFaceToken(), event.getLogId());
                    if (!result.isSuccess()) {
                        log.error("人脸删除失败：{}", result.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("删除人脸异常", e);
                // 放入队列重试删除，重试次数超过2次后放弃
                int retryCount = event.getDeleteRetryCount();
                if (retryCount < 2) {
                    event.setDeleteRetryCount(retryCount + 1);
                    faceDeleteQueue.addEvent(event);
                } else {
                    log.error("删除人脸重试超过2次，放弃：{}", event.getFaceName());
                }
            }
        }, 0, 300, TimeUnit.MILLISECONDS); // 执行完单个任务后延迟300毫秒执行下个任务
    }
}
