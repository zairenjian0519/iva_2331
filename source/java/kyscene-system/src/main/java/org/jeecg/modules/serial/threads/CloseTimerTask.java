package org.jeecg.modules.serial.threads;

import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.serial.service.SerialPortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

public class CloseTimerTask extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(CloseTimerTask.class);

    private final String serialName;

    private final String closeCode;

    private final SerialPortService serialPortService;

    public CloseTimerTask(String serialName, String closeCode) {
        this.serialName = serialName;
        this.closeCode = closeCode;
        serialPortService = SpringContextUtils.getBean(SerialPortService.class);
    }

    @Override
    public void run() {
        try {
            serialPortService.closeVoice(serialName, closeCode);
        } catch (Exception e) {
            log.error("串口 {}，执行关闭指令 {} 错误", serialName, closeCode);
            this.cancel();
        }

    }
}
