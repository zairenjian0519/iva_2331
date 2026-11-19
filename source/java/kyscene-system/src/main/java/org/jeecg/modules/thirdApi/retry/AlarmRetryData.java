package org.jeecg.modules.thirdApi.retry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("alarm_retry_data")
public class AlarmRetryData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报警记录id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 相机名称
     */
    private String cameraName;

    /**
     * 相机id
     */
    private String cameraId;

    /**
     * 相机IP
     */
    private String cameraIp;

    /**
     * 分组名称数组
     */
    private String groupNames;

    /**
     * 告警类型名称
     */
    private String alarmType;

    /**
     * 告警类型code
     */
    private String alarmTypeCode;

    /**
     * 告警等级名称
     */
    private String alarmLevel;

    /**
     * 告警等级code
     */
    private String alarmLevelCode;

    /**
     * 告警图片地址
     */
    private String alarmPictureUrl;

    /**
     * 告警图片Base64编码
     */
    @TableField(exist = false)
    private String alarmPictureBase64;

    /**
     * 超脑设备IP
     */
    private String deviceIp;

    /**
     * 告警时间
     */
    private Date alarmTime;
}
