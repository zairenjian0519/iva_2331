package org.jeecg.modules.face.entity;

import lombok.Data;

@Data
public class FaceQueryParams {
    /**
     * 搜索人脸库id
     */
    private String groupId;

    /**
     * 搜索人脸名称
     */
    private String faceName;

    /**
     * 搜索人脸手机号
     */
    private String facePhone;

    /**
     * 搜索人脸身份证
     */
    private String faceIdCard;
}
