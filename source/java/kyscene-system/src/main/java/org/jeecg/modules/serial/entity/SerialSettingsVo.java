package org.jeecg.modules.serial.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SerialSettingsVo extends SoundPillarParam{

    /**
     * 串口表id
     */
    private String serialPortId;

    /**
     * 音频表id
     */
    private Integer voiceFileId;

    /**
     * 串口音频算法表关联id
     */
    private String serialPortVoiceId;

    /**
     * 串口状态
     */
    private Integer serialPortStatus;
}
