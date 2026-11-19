/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/5/23
 */
@Data
@Component
@ConfigurationProperties(prefix = "face")
public class FaceConfigProperties {

    // 人脸照片基础路径
    private String baseFacePath;

    // 人脸组中人脸最大数量
    private long groupFaceCount;

    // npy文件路径
    private String npyFilePath;
}
