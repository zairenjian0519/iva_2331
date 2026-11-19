/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.wireless;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/8/19
 */
@Data
@Component
@ConfigurationProperties(prefix = "sound-device")
public class SoundPillarProperties {

    private String appId;

    private String appCode;

    private String defaultAccount;

    private String defaultPassword;
}
