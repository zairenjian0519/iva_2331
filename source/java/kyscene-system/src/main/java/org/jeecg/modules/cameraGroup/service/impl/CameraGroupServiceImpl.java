package org.jeecg.modules.cameraGroup.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.cameraGroup.entity.CameraGroup;
import org.jeecg.modules.cameraGroup.mapper.CameraGroupMapper;
import org.jeecg.modules.cameraGroup.service.ICameraGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
 * @Description: 相机组
 * @Author: jeecg-boot
 * @Date:   2023-02-14
 * @Version: V1.0
 */
@Service
public class CameraGroupServiceImpl extends ServiceImpl<CameraGroupMapper, CameraGroup> implements ICameraGroupService {
    @Autowired
    private CameraGroupMapper cameraGroupMapper;

    @Override
    public IPage<CameraGroup> pageList(IPage page, CameraGroup cameraGroup) {
        return cameraGroupMapper.pageList(page,cameraGroup);
    }

    @Override
    public List<String> getDeviceIpByGroupId(String id) {
        return cameraGroupMapper.getDeviceIpByGroupId(id);
    }
}
