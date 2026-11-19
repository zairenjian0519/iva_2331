/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.elock;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.iot.gpio.GpioRealtimeDataService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 读取电磁锁状态，定时任务
 *
 * @author： DecoderX
 * @date： 2025/10/13
 */
@Slf4j
public class ElectricReadJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("开始读取电磁锁状态...");
        GpioRealtimeDataService electricLockService = SpringContextUtils.getBean(GpioRealtimeDataService.class);
        electricLockService.readAllDeviceData();
    }
}
