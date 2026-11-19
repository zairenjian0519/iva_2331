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
public class ItemQueryParams {

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 通道
     */
    private int channel;

    /**
     * 预置点ID
     */
    private int presetId;

    /**
     * 规则ID
     */
    int ruleId;

    /**
     * 测点类型
     */
    int meterType;
}
