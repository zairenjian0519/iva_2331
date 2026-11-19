/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.elock;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/10/20
 */
@Data
public class ElectricLockRealMqttMsg {

    /**
     * 设备id
     */
    private String id;

    /**
     * 设备位置
     */
    private String location;

    /**
     * 设备IP
     */
    private String ipAddress;

    /**
     * 锁名称
     */
    private String deviceName;

    /**
     * 锁编号
     */
    private String deviceCode;

    /**
     * 锁描述
     */
    private String remark;

    /**
     * GPIO pin
     */
    private String pinNumber;

    /**
     * 实时数据
     */
    private String realtimeData;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

}
