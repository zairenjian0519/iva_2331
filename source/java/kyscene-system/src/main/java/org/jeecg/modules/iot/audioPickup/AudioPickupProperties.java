/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/7/1
 */
@Data
@Component
@ConfigurationProperties(prefix = "iot.audio")
public class AudioPickupProperties {

    private String path;

    private Integer clearLimit;
}