package org.jeecg.modules.cameraManager.event;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import org.jeecg.modules.cameraManager.entity.CameraManager;

import java.util.List;

/**
 * 事件选项
 */
@Data
public class CameraEventOptions {

    /**
     * 设备ip
     */
    private String deviceIp = "127.0.0.1";

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 相机原始状态数组
     */
    private List<CameraManager> cameraOriginalList;
}
