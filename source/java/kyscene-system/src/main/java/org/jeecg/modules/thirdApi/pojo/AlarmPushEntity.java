package org.jeecg.modules.thirdApi.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class AlarmPushEntity {

    /**
     * 报警记录id
     */
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
     * 相机序列号
     */
    private String cameraSn;

    /**
     * 项目编号
     */
    private String projectKey;

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
    private String alarmPictureBase64;

    /**
     * 超脑设备IP
     */
    private String deviceIp;

    /**
     * 告警时间
     */
    private Date alarmTime;

    /**
     * 设备id/sn
     */
    private String deviceSn;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 告警人员信息
     */
    private AlarmPerson alarmPerson;

}
