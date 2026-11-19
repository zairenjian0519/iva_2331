/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@Data
public class ThermalMqttMsg {
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
     * IP地址
     */
    private String ipAddress;

    /**
     * 最高温度
     */
    private String temperatureMax;

    /**
     * 温度最小值
     */
    private String temperatureMin;

    /**
     * 温度平均值
     */
    private String temperatureAver;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
