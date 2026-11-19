/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.deviceManagement.config;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.configjson.Const;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/7/29
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class InitDeviceInfo implements CommandLineRunner {

    private final IDeviceManagementService deviceManagementService;

    private final ICameraManagerService cameraManagerService;

    private final RedisUtil redisUtil;

    @Override
    public void run(String... args) throws Exception {
        List<DeviceManagement> devices = deviceManagementService.list();
        if (!devices.isEmpty()) {
            DeviceManagement dev = devices.get(0);
            // 初始化设备信息
            redisUtil.set(Const.DEVICE_LOCAL_INFO_KEY, dev);
            log.info("初始化设备信息成功：{}", dev);

            // 初始化设备下面的相机列表
            List<CameraManager> cameraList = cameraManagerService.getListByDeviceId(dev.getId());
            if (!cameraList.isEmpty()) {
                redisUtil.set(Const.DEVICE_LOCAL_CAMERA_LIST_KEY, JSON.toJSONString(cameraList));
            }
            log.info("初始化设备相机列表成功，数量：{}", cameraList.size());
        }
    }
}
