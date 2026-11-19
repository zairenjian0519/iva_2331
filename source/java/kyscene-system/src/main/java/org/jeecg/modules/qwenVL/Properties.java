/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.qwenVL;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/7/25
 */
@Data
@Component
@ConfigurationProperties(prefix = "multi-modal")
public class Properties {

    private String apiKey;

    private String scriptPath;

    private String templatePath;
}