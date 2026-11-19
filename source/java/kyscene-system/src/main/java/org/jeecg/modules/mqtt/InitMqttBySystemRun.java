package org.jeecg.modules.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Created by Elmer on 2022/10/20.
 */
@Slf4j
@Component
public class InitMqttBySystemRun implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private IDeviceManagementService deviceManagementService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ThreadUtil.execAsync(() -> {
            log.info("初始化设备MQTT");
            deviceManagementService.initMqtt();
            deviceManagementService.initCustomMqtt();
        });
    }
}
