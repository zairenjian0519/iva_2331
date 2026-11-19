/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.algAdapter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/7/16
 */
@Data
@Component
@ConfigurationProperties(prefix = "algorithm-adapter")
public class ConfigProperties {

    /**
     * 算法适配主文件上传保存目录
     */
    private String mainUploadPath;

    /**
     * 算法适配主文件解压的目标目录
     */
    private String mainExecutePath;

    /**
     * 模型文件上保存的目录
     */
    private String modelUploadPath;

    /**
     * 模型文件工作目录
     */
    private String modelExecutePath;
}
