/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import lombok.Data;

/**
 * @author： DecoderX
 * @date： 2025/10/22
 */
@Data
public class Serial485Setting {
    /**
     * 串口名称
     */
    private String commPortName;

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
    private Integer parity;

    /**
     * 状态: 0-未连接, 1-已连接
     */
    private Integer status;
}
