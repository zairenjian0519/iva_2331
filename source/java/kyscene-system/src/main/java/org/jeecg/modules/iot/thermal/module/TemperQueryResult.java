/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal.module;

import lombok.Data;

/**
 * @author： DecoderX
 * @date： 2025/11/18
 */
@Data
public class TemperQueryResult {

    /**
     * 测点类型
     */
    private String meterType;

    /**
     * 温度单位
     */
    private String temperUnit;

    /**
     * 平均温度
     */
    private String temperAver;

    /**
     * 最高温度
     */
    private String temperMax;

    /**
     * 最低温度
     */
    private String temperMin;

    /**
     * 中位数温度
     */
    private String temperMid;

    /**
     * 标准差温度
     */
    private String temperStd;

}
