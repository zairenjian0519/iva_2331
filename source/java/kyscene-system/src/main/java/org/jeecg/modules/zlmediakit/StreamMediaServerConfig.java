package org.jeecg.modules.zlmediakit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 流媒体服务配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "media-server")
public class StreamMediaServerConfig {

    /**
     * 流媒体服务ip
     */
    private String ipAddress;

    /**
     * zlmediakit http port
     */
    private Integer restApiPort;

    /**
     * 密钥
     */
    private String secret;
}
