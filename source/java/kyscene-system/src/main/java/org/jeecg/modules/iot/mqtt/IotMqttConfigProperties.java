/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.mqtt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/10/13
 */
@Data
@Component
@ConfigurationProperties(prefix = "iot.mqtt")
public class IotMqttConfigProperties {
    /**
     * mqtt地址
     */
    private String host;

    /**
     * mqtt端口
     */
    private int port;

    /**
     * mqtt用户名
     */
    private String userName;

    /**
     * mqtt密码
     */
    private String passWord;

    /**
     * mqtt订阅的topic
     */
    private String subTopic;

    /**
     * mqtt推送topic
     */
    private String pubTopic;
}
