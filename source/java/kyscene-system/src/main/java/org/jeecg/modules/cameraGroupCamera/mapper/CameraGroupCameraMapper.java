package org.jeecg.modules.cameraGroupCamera.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.cameraGroupCamera.entity.CameraGroupCamera;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 轮询组相机表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
public interface CameraGroupCameraMapper extends BaseMapper<CameraGroupCamera> {

    List<CameraGroupCamera> getCameraByGroupId(String groupId);

    void removeByGroupId(String id);

    String getCameraIds(String groupId);

    String getAlgIds(String groupId);

    List<CameraGroupCamera> getByDeviceId(String devId);
}
