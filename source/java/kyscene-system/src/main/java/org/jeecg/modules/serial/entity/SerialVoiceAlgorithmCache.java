package org.jeecg.modules.serial.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class SerialVoiceAlgorithmCache implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 串口名
     */
    private String serialName;

    /**
     * 音频播放指令
     */
    private String openCode;

    /**
     * 算法类型
     */
    private String algorithm;
}
