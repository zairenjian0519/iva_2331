package org.jeecg.modules.serial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("serial_port_voice")
public class SerialPortVoiceDo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一id
     */
    private String id;

    /**
     * 串口唯一id
     */
    private String serialPortId;

    /**
     * 告警声音唯一id
     */
    private Integer voiceFileId;

    /**
     * 算法类型
     */
    private Integer algorithm;

    /**
     * 音柱名
     */
    private String soundName;
}
