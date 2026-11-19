/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.heart;

import lombok.Data;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/10/27
 */
@Data
public class DeviceHeartVo {
    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备ip
     */
    private String ipAddress;

    /**
     * 设备位置
     */
    private String location;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 设备状态 0:离线 1:在线
     */
    private Integer status;

    /**
     * 时间戳
     */
    private Date timestamp;
}
