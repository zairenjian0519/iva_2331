package org.jeecg.modules.thirdApi.retry;

import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.thirdApi.retry.service.AlarmRetryService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 定时重试推送失败的数据
 */
public class AlarmRetryTask implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        AlarmRetryService alarmRetryService = SpringContextUtils.getBean(AlarmRetryService.class);
        alarmRetryService.retryPushData();
    }
}
