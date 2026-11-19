/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.audioPickup.entity.AudioPickup;
import org.jeecg.modules.iot.audioPickup.service.IAudioPickupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 开机检查是否需要启动拾音器采集
 *
 * @author： DecoderX
 * @date： 2025/7/1
 */

@Slf4j
@Component
public class StartUpCommandline implements CommandLineRunner {

    @Autowired
    private IAudioPickupService audioPickupService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始启动音频采集服务...");
        Object o = redisUtil.get(DeviceEnum.AUDIO_PICKUP_DEVICE_LIST.getCode());
        if (o == null) {
            log.info("音频设备列表为空，请先添加设备");
            return;
        }
        List<AudioPickup> audioPickups = JSON.parseArray(o.toString(), AudioPickup.class);
        audioPickups.forEach(audioPickup -> {
            if (audioPickup.getStatus() == 1) {
                log.info("启动设备：{}", audioPickup.getDeviceName());
                audioPickupService.startCollecting(audioPickup.getId());
            }
        });
        log.info("音频采集服务启动完成！");
    }
}