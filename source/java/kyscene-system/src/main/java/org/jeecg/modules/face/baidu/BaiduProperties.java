/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/8/6
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "baidu")
public class BaiduProperties {

    private String apiKey;

    private String secretKey;
}
