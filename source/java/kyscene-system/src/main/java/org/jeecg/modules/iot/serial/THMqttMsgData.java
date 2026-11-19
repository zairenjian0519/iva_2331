/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@Data
public class THMqttMsgData {
    /**
     * 设备id
     */
    private String id;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备位置
     */
    private String location;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 从站地址
     */
    private Integer slaveAddress;

    /**
     * 温度
     */
    private Double temperature;

    /**
     * 湿度
     */
    private Double humidity;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * ip地址
     */
    private String ipAddress;
}
