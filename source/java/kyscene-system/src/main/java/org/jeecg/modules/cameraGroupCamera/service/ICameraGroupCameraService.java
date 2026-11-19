package org.jeecg.modules.cameraGroupCamera.service;

import org.jeecg.modules.cameraGroupCamera.entity.CameraGroupCamera;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @Description: 轮询组相机表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
public interface ICameraGroupCameraService extends IService<CameraGroupCamera> {

    List<CameraGroupCamera> getCameraByGroupId(String groupId);

    void removeByGroupId(String id);

    String getCameraIds(String groupId);

    String getAlgIds(String groupId);

    List<CameraGroupCamera> getByDeviceId(String devId);
}
