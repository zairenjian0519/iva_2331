/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.deviceManagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/5/27
 */
@Data
@Component
@ConfigurationProperties(prefix = "upload-update")
public class UploadUpdateProperties {

    /**
     * 盒子ip地址
     */
    private String path;

    /**
     * web前端路径
     */
    private String webFrontPath;

    /**
     * web后端路径
     */
    private String webBackendPath;

    /**
     * 算法路径
     */
    private String algorithmPath;

    /**
     * 更新算法插件路径
     */
    private String algorithmPluginPath;

    /**
     * 封面图路径
     */
    private String thumbnailPath;
}
