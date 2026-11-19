/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.remoteSync;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/5/22
 */
@Data
@Component
@ConfigurationProperties(prefix = "iot.api")
public class ApiProperties {

    /**
     * appKey
     */
    private String appKey;

    /**
     * appSecret
     */
    private String appSecret;

    /**
     * tokenUrl
     */
    private String tokenUrl;

    /**
     * differentUrl
     */
    private String differentUrl;

    /**
     * allUrl
     */
    private String allUrl;
}
