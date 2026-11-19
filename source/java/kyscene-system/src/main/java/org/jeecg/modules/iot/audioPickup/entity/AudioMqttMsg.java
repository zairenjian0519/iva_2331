/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/10/27
 */
@Data
public class AudioMqttMsg {

    /**
     * 拾音器id
     */
    private String id;

    /**
     * 名称
     */
    private String deviceName;

    /**
     * 编码
     */
    private String deviceCode;

    /**
     * 位置
     */
    private String location;

    /**
     * 备注
     */
    private String remark;

    /**
     * rtsp地址
     */
    private String rtspUrl;

    /**
     * 录制分片时长(秒)
     */
    private Integer recordSegmentSeconds;

    /**
     * 音频保存路径
     */
    private String audioPath;

    /**
     * 在线地址
     */
    private String onlineAddress;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * ip地址 - 盒子ip
     */
    private String ipAddress;

}
