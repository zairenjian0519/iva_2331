package org.jeecg.modules.cameraManager.entity;

import lombok.Data;

import java.util.List;

@Data
public class CameraTreeVo {

    List<LocationTreeVo> locationTree;

    List<DeviceTreeVo> deviceTree;
    int allCameraCount;
}
