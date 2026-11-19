package org.jeecg.modules.wireless;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 定时同步音柱设备状态信息
 */
@Slf4j
public class SyncAudioDeviceTask implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("定时同步音柱设备信息开始...");
        SoundPillarService soundPillarService = SpringContextUtils.getBean(SoundPillarService.class);
        soundPillarService.syncDevices(null);
        log.info("定时同步音柱设备信息结束...");
    }
}