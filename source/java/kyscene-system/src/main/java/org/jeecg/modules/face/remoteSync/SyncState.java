/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.remoteSync;

import lombok.Data;

/**
 * 单例类
 *
 * @author： DecoderX
 * @date： 2025/9/2
 */
@Data
public class SyncState {

    // upload or delete
    private String type;

    private boolean userDeleteQueueEmpty;

    private boolean faceUploadQueueEmpty;

    private boolean globalState;
}
