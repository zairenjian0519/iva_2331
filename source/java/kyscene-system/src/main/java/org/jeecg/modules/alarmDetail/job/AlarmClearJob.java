package org.jeecg.modules.alarmDetail.job;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;
import org.quartz.*;

/**
 * 定时清除告警任务
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Slf4j
public class AlarmClearJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            log.info(" --- 定时清除告警任务开始 --- ");
            IAlarmDetailService alarmDetailService = SpringContextUtils.getBean(IAlarmDetailService.class);
            alarmDetailService.clearAlarm();
            log.info(" --- 定时清除告警任务结束 --- ");
        } catch (Exception e) {
            log.error("定时清除告警任务失败: {}", e.getMessage());
        }
    }
}