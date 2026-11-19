/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial.job;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.iot.serial.THRealDataService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author： DecoderX
 * @date： 2025/10/28
 */
@Slf4j
public class THDeviceStatusJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("开始检查温湿度设备状态...");
        THRealDataService thRealDataService = SpringContextUtils.getBean(THRealDataService.class);
        thRealDataService.checkDeviceStatus();
    }
}
