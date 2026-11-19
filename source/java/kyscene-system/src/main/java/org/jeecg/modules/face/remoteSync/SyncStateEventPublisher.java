/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.remoteSync;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/9/4
 */
@RequiredArgsConstructor
@Component
public class SyncStateEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishSyncStateEvent(SyncState syncState) {
        SyncStateEvent syncStateEvent = new SyncStateEvent(this, syncState);
        applicationEventPublisher.publishEvent(syncStateEvent);
    }
}
