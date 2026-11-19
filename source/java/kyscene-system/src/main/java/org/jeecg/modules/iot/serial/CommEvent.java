/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@Getter
public class CommEvent extends ApplicationEvent {

    private final JSONObject data;

    public CommEvent(Object source, JSONObject payload) {
        super(source);
        this.data = payload;
    }

}
