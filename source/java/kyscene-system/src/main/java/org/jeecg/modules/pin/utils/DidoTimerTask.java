package org.jeecg.modules.pin.utils;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;

import java.io.IOException;
import java.util.TimerTask;

// 复位
@Slf4j
public class DidoTimerTask extends TimerTask {

    private final String cameraId;

    private final String pinNumber;

    private final String algorithmCode;

    private final int timeSegment;

    public DidoTimerTask(String cameraId, String pinNumber, String algorithmCode, int timeSegment) {
        this.cameraId = cameraId;
        this.pinNumber = pinNumber;
        this.algorithmCode = algorithmCode;
        this.timeSegment = timeSegment;
    }

    @Override
    public void run() {
        IAlarmDetailService alarmDetailService = SpringContextUtils.getBean(IAlarmDetailService.class);
        int count = alarmDetailService.getCountByCameraIdAndAlgCode(cameraId, algorithmCode, timeSegment);
        if (count == 0) {
            // 复位
            try {
                GpioControl.getInstance(pinNumber, "out").setValue("0");
                this.cancel();
            } catch (IOException e) {
                log.error("gpio 复位写入失败：{}", pinNumber);
                this.cancel();
            }
        }
    }
}
