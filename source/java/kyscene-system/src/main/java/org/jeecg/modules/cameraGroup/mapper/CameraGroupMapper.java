package org.jeecg.modules.cameraGroup.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.cameraGroup.entity.CameraGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 相机组
 * @Author: jeecg-boot
 * @Date:   2023-02-14
 * @Version: V1.0
 */
public interface CameraGroupMapper extends BaseMapper<CameraGroup> {
    IPage<CameraGroup> pageList(IPage page, @Param("cameraGroup") CameraGroup cameraGroup);

    List<String>  getDeviceIpByGroupId(@Param("id") String id);
}
