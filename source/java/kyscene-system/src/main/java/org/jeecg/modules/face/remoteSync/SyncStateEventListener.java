/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.remoteSync;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.devwebsocket.FaceSyncWebSocket;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/9/4
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SyncStateEventListener {

    private final FaceSyncWebSocket faceSyncWebSocket;

    private final GlobalSyncStateCache globalSyncStateCache;

    @Async
    @EventListener
    public void onApplicationEvent(SyncStateEvent syncStateEvent) {
        asyncState(syncStateEvent.getSyncState());
    }

    public void asyncState(SyncState syncState) {
        if (StrUtil.equals(syncState.getType(), "upload")) {
            globalSyncStateCache.getSyncState().setFaceUploadQueueEmpty(syncState.isFaceUploadQueueEmpty());
        } else if (StrUtil.equals(syncState.getType(), "delete")){
            globalSyncStateCache.getSyncState().setUserDeleteQueueEmpty(syncState.isUserDeleteQueueEmpty());
        } else {
            return;
        }

        // 检测同步状态, 只要有一个队列有数据，则同步状态为true
        globalSyncStateCache.getSyncState().setGlobalState(!globalSyncStateCache.getSyncState().isFaceUploadQueueEmpty() || !globalSyncStateCache.getSyncState().isUserDeleteQueueEmpty());

    }

    // 定时任务，每3秒执行一次
    @Scheduled(cron = "0/3 * * * * ?")
    public void syncState() {
        // 通知前端
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("SyncState", globalSyncStateCache.getSyncState().isGlobalState());
        faceSyncWebSocket.sendMessageToUser(jsonObject.toJSONString());
    }
}
