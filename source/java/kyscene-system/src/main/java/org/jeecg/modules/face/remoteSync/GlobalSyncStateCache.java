/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.remoteSync;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 缓存全局同步状态
 * @author： DecoderX
 * @date： 2025/9/4
 */
@Data
@Component
public class GlobalSyncStateCache {

    private SyncState syncState;

    public GlobalSyncStateCache() {
        SyncState temp = new SyncState();
        temp.setGlobalState(false);
        temp.setUserDeleteQueueEmpty(true);
        temp.setFaceUploadQueueEmpty(true);
        this.syncState = temp;
    }
}
