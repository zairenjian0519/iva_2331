/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.face.entity.FaceDetailDo;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecg.modules.face.remoteSync.SyncState;
import org.jeecg.modules.face.remoteSync.SyncStateEventPublisher;
import org.jeecg.modules.face.service.FaceDetailService;
import org.jeecg.modules.face.service.FaceGroupService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author： DecoderX
 * @date： 2025/8/7
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FaceUploadConsumer {

    private final FaceUploadQueue faceUploadQueue;

    private final UserManagerService userManagerService;

    private final FaceGroupService faceGroupService;

    private final FaceDetailService faceDetailService;

    private final SyncStateEventPublisher syncStateEventPublisher;

    @PostConstruct
    public void startConsumer() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            // 检测队列是否为空, 用于同步状态
            SyncState syncState = new SyncState();
            syncState.setType("upload");
            syncState.setFaceUploadQueueEmpty(faceUploadQueue.size() == 0);
            syncStateEventPublisher.publishSyncStateEvent(syncState);

            FaceDetailDo event = faceUploadQueue.get();
            try {
                if (event != null && StrUtil.isNotBlank(event.getId())) {
                    log.info("开始人脸上传：{}", event.getVectorId());
                    FaceGroupDo faceGroupDo = faceGroupService.getById(event.getFaceGroup());
                    String base64 = Base64.getEncoder().encodeToString(FileUtil.readBytes(event.getImagePath()));
                    Result<?> result = userManagerService.createUser(faceGroupDo.getGroupId(), event.getVectorId(), base64);
                    if (result.isSuccess()) {
                        Object object = result.getResult();
                        if (object != null) {
                            JSONObject jsonObject = (JSONObject) object;
                            FaceDetailDo faceDetailDo = new FaceDetailDo();
                            faceDetailDo.setId(event.getId());
                            faceDetailDo.setFaceToken(jsonObject.getString("face_token"));
                            faceDetailDo.setLogId(jsonObject.getString("log_id"));
                            faceDetailService.updateById(faceDetailDo);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("人脸上传异常", e);
                // 放入队列重试上传，重试次数超过2次后放弃
                int retryCount = event.getUploadRetryCount();
                if (retryCount < 2) {
                    event.setUploadRetryCount(retryCount + 1);
                    faceUploadQueue.addEvent(event);
                } else {
                    log.error("上传人脸重试超过2次，放弃：{}", event.getFaceName());
                }
            }
        }, 100, 300, TimeUnit.MILLISECONDS); // 执行完单个任务后延迟300毫秒执行下个任务
    }
}
