/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.remoteSync;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author： DecoderX
 * @date： 2025/9/4
 */
@Getter
public class SyncStateEvent extends ApplicationEvent {

    private final SyncState syncState;

    public SyncStateEvent(Object source, SyncState syncState) {
        super(source);
        this.syncState = syncState;
    }
}
