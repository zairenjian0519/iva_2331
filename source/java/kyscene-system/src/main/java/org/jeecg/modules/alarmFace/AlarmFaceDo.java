/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.alarmFace;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/4/8
 */

@Data
@TableName("alarm_face")
public class AlarmFaceDo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 告警时间
     */
    private Date alarmTime;

    /**
     * 相机id
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

    /**
     * 告警图片
     */
    private String thumbnailPath;

    /**
     * 告警图片
     */
    private String picPath;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 告警id
     */
    private String alarmId;

    /**
     * 是否轮询
     */
    private Integer isRotate;
}
