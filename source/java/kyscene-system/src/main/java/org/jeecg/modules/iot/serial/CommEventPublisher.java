/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@RequiredArgsConstructor
@Component
public class CommEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    // 静态实例
    private static ApplicationEventPublisher staticPublisher;

    @PostConstruct
    public void init() {
        staticPublisher = applicationEventPublisher;
    }

    public static void publishCommEvent(JSONObject data) {
        CommEvent commEvent = new CommEvent(staticPublisher, data);
        staticPublisher.publishEvent(commEvent);
    }
}
