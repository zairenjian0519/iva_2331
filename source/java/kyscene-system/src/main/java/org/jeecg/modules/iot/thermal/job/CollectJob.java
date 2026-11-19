/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal.job;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.iot.thermal.ThermalRealDataService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author： DecoderX
 * @date： 2025/11/19
 */
@Slf4j
public class CollectJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("开始采集温度数据...");
        ThermalRealDataService realDataService = SpringContextUtils.getBean(ThermalRealDataService.class);
        realDataService.collectAllDeviceData();
    }
}
