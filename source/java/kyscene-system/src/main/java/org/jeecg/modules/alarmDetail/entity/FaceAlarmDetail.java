/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.alarmDetail.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/3/25
 */

@Data
public class FaceAlarmDetail {

    /**
     * id
     */
    private String id;

    /**
     * 告警ID
     */
    private String alarmId;

    /**
     * 告警时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date alarmTime;

    /**
     * 相机ID
     */
    private String cameraId;

    /**
     * 相机名称
     */
    private String cameraName;

    /**
     * 告警类型
     */
    private String alarmType;

    private String alarmTypeDict;

    /**
     * 缩略图地址
     */
    private String thumbnailPath;

    /**
     * 原图地址
     */
    private String picPath;


    /**
     * 算法备注
     */
    private String algorithmRemark;

    //新增相机编码字段
    private String cameraNumber;

    /**
     * 目标数量
     */
    private Integer targetCount;

    /**
     * 轮询和实时标志
     */
    private Integer isRotate;

    /**
     * 矢量ID
     */
    private String vectorId;

    /**
     * 人脸姓名
     */
    private String faceName;

    /**
     * 人脸手机号
     */
    private String facePhone;

    /**
     * 人脸身份证号
     */
    private String faceIdCard;

    /**
     * 人脸图片新名称
     */
    private String imageNewName;

    /**
     * 人脸图片路径
     */
    private String imagePath;

    /**
     * 备注
     */
    private String remark;

    /**
     * 人脸组ID
     */
    private String faceGroup;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组路径
     */
    private String groupPath;

}
