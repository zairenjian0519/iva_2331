package org.jeecg.modules.alarmDetail.entity;

import lombok.Data;

/**
 * Created by Elmer on 2022/9/22.
 */
@Data
public class AlarmDetailMqtt {

    /**
     * 告警信息ID
     */
    private String warningID;
    /**
     * 相机ID
     */
    private String cameraID;
    /**
     * 相机名称
     */
    private String cameraName;
    /**
     * 监控职责描述(场景定位,如:1号卸载点,球磨机等)
     */
    private String monitorDuty;
    /**
     * 支持的告警类型列表(0:未知,1:安全帽,2:口罩,3:吸烟,4:打电话,5:烟火,6:睡岗,7:脱岗,8:人脸识别)
     */
    private String warningType;

    /**
     * 告警级别
     */
    private String warningLevel;
    /**
     * 告警时间
     */
    private String warningTime;
    /**
     * 告警图片地址
     */
    private String warningImagePath;
    /**
     * 告警图片标识信息
     */
    private String detectObjs;


}
