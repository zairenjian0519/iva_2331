package org.jeecg.modules.cameraManager.entity;

import lombok.Data;

import java.util.LinkedList;

@Data
public class LocationTreeVo {
    private String title;

    private String key;

    private String type;

    private String parentNode;

    private String parentKey;

    private Integer cameraCount;

    private Integer offLineCount;//设备下挂离线的相机数

    private Integer alarmCount;

    private String runStatus;

    private boolean haveAlarm;

    private String deviceIp;

    private Object slots;

    private String streamAddress;

    private String pic;

    private LinkedList<LocationTreeVo> children;

}
