package org.jeecg.modules.serial.threads;

import org.jeecg.modules.serial.entity.SoundPillarParam;
import org.jeecg.modules.serial.service.SerialPortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutor extends Thread {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutor.class);

    private final SoundPillarParam soundPillarParam;

    private final SerialPortService serialPortService;

    public TaskExecutor(SoundPillarParam soundPillarParam, SerialPortService serialPortService) {
        log.info("音柱播放线程启动，当前报警算法：{}", soundPillarParam.getAlgorithm());
        this.soundPillarParam = soundPillarParam;
        this.serialPortService = serialPortService;
    }

    @Override
    public void run() {
        serialPortService.playSound(soundPillarParam);
    }
}
