/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.thirdApi.pojo;

import lombok.Data;

/**
 * @author： DecoderX
 * @date： 2025/5/12
 */
@Data
public class AlarmPerson {

    /**
     * id
     */
    private String personId;

    /**
     * 证件号码
     */
    private String idCard;

    /**
     * 姓名
     */
    private String name;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 人脸组名称
     */
    private String faceGroupName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 人脸照片
     */
    private String photoBase64;
}
