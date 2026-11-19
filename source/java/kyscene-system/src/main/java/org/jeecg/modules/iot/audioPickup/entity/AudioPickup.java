/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/6/30
 */
@Data
@TableName("audio_pickup")
public class AudioPickup {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 位置
     */
    private String location;

    /**
     * 拾音器名称
     */
    private String deviceName;

    /**
     * 拾音器编号
     */
    private String deviceCode;


    /**
     * 拾音器rtsp地址
     */
    private String rtspUrl;

    /**
     * 拾音器备注
     */
    private String remark;

    /**
     * 设备状态
     * 1-在线 0 - 离线
     */
    private Integer status;

    /**
     * 采集状态
     * 1-正在录制 0 - 停止录制
     */
    private Integer collectStatus;

    /**
     * 录制分片时长(秒)
     */
    private Integer recordSegmentSeconds;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * ip 地址
     */
    private String ipAddress;
}