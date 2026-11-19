package org.jeecg.modules.deviceManagement.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 设备管理
 * @Author: jeecg-boot
 * @Date:   2022-08-23
 * @Version: V1.0
 */
public interface DeviceManagementMapper extends BaseMapper<DeviceManagement> {

    int getUsedAlgNum(String id);

    int getUsedCameraNum(String id);

    DeviceManagement getByCameraId(String cameraId);

    void removeAuditByDeviceId(String devId);

    void removeAlarmByDeviceId(String devId);

    DeviceManagement getDeviceIp(String id);

    void updateIdByIp(String ip,String id);

    void updateIpById(String id, String ip);
}
