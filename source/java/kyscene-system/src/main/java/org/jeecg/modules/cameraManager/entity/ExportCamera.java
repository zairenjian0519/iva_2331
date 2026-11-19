package org.jeecg.modules.cameraManager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ExportCamera extends CameraManager {
    private String locationName;
    private String factoryName;
    private String deviceName;
}
