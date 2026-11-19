package org.jeecg.modules.cameraGroup.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.cameraGroup.entity.CameraGroup;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @Description: 相机组
 * @Author: jeecg-boot
 * @Date:   2023-02-14
 * @Version: V1.0
 */
public interface ICameraGroupService extends IService<CameraGroup> {

    IPage<CameraGroup> pageList(IPage page, CameraGroup cameraGroup);

    List<String>  getDeviceIpByGroupId(String id);
}
