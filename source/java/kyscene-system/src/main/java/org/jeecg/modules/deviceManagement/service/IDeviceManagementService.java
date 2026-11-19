package org.jeecg.modules.deviceManagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.thirdApi.pojo.DeviceInfoVo;

/**
 * @Description: 设备管理
 * @Author: jeecg-boot
 * @Date:   2022-08-23
 * @Version: V1.0
 */
public interface IDeviceManagementService extends IService<DeviceManagement> {

    int getUsedAlgNum(String id);

    int getUsedCameraNum(String id);

    DeviceManagement getByCameraId(String cameraId);

    void initMqtt();

    void initCustomMqtt();

    void removeAlarmByDeviceId(String id);

    DeviceManagement getByAlgorithmId(String id);

    void updateIdByIp(String ip,String id);

    DeviceInfoVo getDeviceInfo(String deviceIp);

    Result<?> updateDeviceStatus (String id, String status);

    void updateIpById(String id, String ip);
}
