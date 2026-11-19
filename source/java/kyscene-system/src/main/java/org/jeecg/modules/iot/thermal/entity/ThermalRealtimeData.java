/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@Data
@TableName("thermal_realtime_data")
public class ThermalRealtimeData {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 设备Id
     */
    private String deviceId;

    /**
     * 温度最大值
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
     * 温度中位数
     */
    private String temperatureMid;

    /**
     * 温度标准差
     */
    private String temperatureStd;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
