package org.jeecg.modules.serial.threads;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.serial.config.SerialPortUtil;
import org.jeecg.modules.serial.entity.SoundPillarParam;
import org.jeecg.modules.serial.service.SerialPortService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduleExecutor {

    @Autowired
    private SerialPortService serialPortService;

    /**
     * 3秒一次轮询音柱播放队列
     */
    @Scheduled(fixedDelay = 3000)
    public void playVoiceExecutor(){
        try {
            SoundPillarParam soundPillarParam = SerialPortUtil.VOICE_PLAY_QUEUE.poll();
            if (soundPillarParam == null) return;
            serialPortService.playVoice(soundPillarParam);
            log.info("执行3秒一次定时任务");
        }catch (Exception e){
            log.error("音柱播放失败：{}", e.getLocalizedMessage());
        }


    }
}
