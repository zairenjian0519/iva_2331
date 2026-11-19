package org.jeecg.modules.cameraManager.event;

import lombok.Data;

@Data
public class CameraEventMsg {
    // 相机id
    private String cameraId;

    // 相机地址
    private String streamAddress;
}
