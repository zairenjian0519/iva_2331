package org.jeecg.modules.cameraGroupCamera.service.impl;

import org.jeecg.modules.cameraGroupCamera.entity.CameraGroupCamera;
import org.jeecg.modules.cameraGroupCamera.mapper.CameraGroupCameraMapper;
import org.jeecg.modules.cameraGroupCamera.service.ICameraGroupCameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
 * @Description: 轮询组相机表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
@Service
public class CameraGroupCameraServiceImpl extends ServiceImpl<CameraGroupCameraMapper, CameraGroupCamera> implements ICameraGroupCameraService {
    @Autowired
    CameraGroupCameraMapper cameraGroupCameraMapper;

    @Override
    public List<CameraGroupCamera> getCameraByGroupId(String groupId) {
        return cameraGroupCameraMapper.getCameraByGroupId(groupId);
    }

    @Override
    public void removeByGroupId(String id) {
        cameraGroupCameraMapper.removeByGroupId(id);
    }

    @Override
    public String getCameraIds(String groupId) {
        return cameraGroupCameraMapper.getCameraIds(groupId);
    }

    @Override
    public String getAlgIds(String groupId) {
        return cameraGroupCameraMapper.getAlgIds(groupId);
    }

    @Override
    public List<CameraGroupCamera> getByDeviceId(String devId) {
        return cameraGroupCameraMapper.getByDeviceId(devId);
    }
}
