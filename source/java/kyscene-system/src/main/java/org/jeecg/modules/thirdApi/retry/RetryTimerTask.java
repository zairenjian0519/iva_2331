package org.jeecg.modules.thirdApi.retry;

import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.AlarmPushHandler;
import org.jeecg.modules.thirdApi.pojo.AlarmPushEntity;
import org.jeecg.modules.thirdApi.retry.service.AlarmRetryService;

import java.util.TimerTask;

/**
 * 数据推送任务线程
 */
@Slf4j
public class RetryTimerTask extends TimerTask {

    private final AlarmRetryData alarmRetryData;

    private final AlarmPushHandler alarmPushHandler;

    private final AlarmRetryService alarmRetryService;


    public RetryTimerTask(AlarmRetryData alarmRetryData) {
        this.alarmRetryData = alarmRetryData;
        alarmPushHandler = SpringContextUtils.getBean(AlarmPushHandler.class);
        alarmRetryService = SpringContextUtils.getBean(AlarmRetryService.class);
    }

    @Override
    public void run() {
        try {
            AlarmPushEntity alarmPushEntity = new AlarmPushEntity();
            BeanUtil.copyProperties(alarmRetryData, alarmPushEntity);
            String picPath = alarmRetryData.getAlarmPictureUrl();
            String base64_head = "data:image/jpg;base64,";
            String imgBase64 = AlarmHandleUtil.getAlarmImgBase64(picPath);
            if (!base64_head.equals(imgBase64)) {
                alarmPushEntity.setAlarmPictureBase64(imgBase64);
                alarmPushEntity.setDeviceSn(SysParameterUtil.getDeviceSn());
                alarmPushHandler.pushData(alarmPushEntity);
            }else { // 删除retry记录，因为图片可能被清理了
                alarmRetryService.removeById(alarmRetryData.getId());
            }
        }catch (Exception e){
            this.cancel();
            log.error("告警消息重推异常");
        }

    }
}
