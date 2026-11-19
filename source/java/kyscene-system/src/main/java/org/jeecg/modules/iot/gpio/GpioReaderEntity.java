/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.gpio;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 读取dido连接的开关状态
 * @author： DecoderX
 * @date： 2025/10/14
 */
@Data
@TableName("serial_pin_reader")
public class GpioReaderEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * gpio编号
     */
    private String pinNumber;

    /**
     * 位置
     */
    private String location;

    /**
     * 锁名称
     */
    private String deviceName;

    /**
     * 锁编号
     */
    private String deviceCode;

    /**
     * 状态 0: 离线 1: 在线
     */
    private Integer status;

    /**
     * 锁描述
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * ip地址 - 盒子ip
     */
    @TableField(exist = false)
    private String ipAddress;
}
