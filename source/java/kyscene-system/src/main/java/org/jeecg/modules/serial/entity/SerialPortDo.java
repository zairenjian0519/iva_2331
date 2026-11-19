package org.jeecg.modules.serial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("serial_port")
public class SerialPortDo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private String id;

    /**
     * 串口号
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
     * 状态：0-离线 1-在线
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
