/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot;

import lombok.Data;

/**
 * @author： DecoderX
 * @date： 2025/10/13
 */
@Data
public class IotMsgEntity<T> {

    private String type;

    private String deviceSn;

    private T data;
}
