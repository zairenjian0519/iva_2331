package org.jeecg.modules.serial.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @since 2024-01-22
 */
@Data
@TableName("sound_pillar_voice")
public class SoundPillar485 implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id")
    private Integer id;

    /**
     * 音频名称
     */
    private String voiceName;

    /**
     * 打开串口的指令
     */
    private String openCode;

    /**
     * 打开串口返回的指令
     */
    private String openCodeResponse;

    /**
     * 关闭串口的指令
     */
    private String closeCode;

    /**
     * 关闭串口返回的指令
     */
    private String closeCodeResponse;


}
