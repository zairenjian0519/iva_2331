/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/5/21
 */
@Data
@Component
@ConfigurationProperties(prefix = "cjzk.mqtt")
public class MqttConfigProperties {

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
    private String topic;

    /**
     * mqtt pub topic
     */
    private String pubTopic;
}
