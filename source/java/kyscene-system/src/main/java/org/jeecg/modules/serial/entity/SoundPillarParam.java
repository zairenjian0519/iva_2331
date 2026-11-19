package org.jeecg.modules.serial.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 参数
 */
@Data
public class SoundPillarParam implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 音柱名称
     */
    private String soundName;

    /**
     * 串口名
     */
    private String serialName;

    /**
     * 波特率
     */
    private Integer baudRate;

    /**
     * 数据位
     */
    private Integer dataBits;

    /**
     * 停止位
     */
    private Integer stopBits;

    /**
     * 校验位
     */
    private Integer parityBit;

    /**
     * 音频文件对应的485指令
     */
    private String voiceFile;

    /**
     * 报警声音对应的算法
     */
    private Integer algorithm;
}
