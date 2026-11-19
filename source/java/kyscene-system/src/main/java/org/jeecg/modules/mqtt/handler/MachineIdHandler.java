package org.jeecg.modules.mqtt.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MachineIdHandler {

    private final IDeviceManagementService deviceManagementService;

    public MachineIdHandler(IDeviceManagementService deviceManagementService){
        this.deviceManagementService = deviceManagementService;
    }

    public void handleMessage(String msg, String mqttClientId){
        try {
            JSONObject machine = JSON.parseObject(msg);
            DeviceManagement dev = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda().eq(DeviceManagement::getDeviceIp, mqttClientId));
            if (null == dev) return;
            dev.setMachineId(machine.getString("machine_id"));
            deviceManagementService.updateById(dev);
            log.info("已获取到设备" + dev.getDeviceName() + "的机器码，存入数据库！");
        } catch (Exception e) {
            log.error("设备机器码处理失败：{}", e.getLocalizedMessage());
        }
    }
}
