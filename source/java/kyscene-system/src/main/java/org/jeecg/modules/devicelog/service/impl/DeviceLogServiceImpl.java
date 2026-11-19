package org.jeecg.modules.devicelog.service.impl;

import org.jeecg.modules.devicelog.entity.DeviceLog;
import org.jeecg.modules.devicelog.mapper.DeviceLogMapper;
import org.jeecg.modules.devicelog.service.IDeviceLogService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 设备升级日志
 * @Author: jeecg-boot
 * @Date:   2022-09-06
 * @Version: V1.0
 */
@Service
public class DeviceLogServiceImpl extends ServiceImpl<DeviceLogMapper, DeviceLog> implements IDeviceLogService {

}
