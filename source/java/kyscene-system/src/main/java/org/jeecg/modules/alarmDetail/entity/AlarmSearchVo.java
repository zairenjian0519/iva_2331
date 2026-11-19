package org.jeecg.modules.alarmDetail.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmSearchVo extends AlarmDetail{

    private List<String> cameraNames;

    private List<String> categorys;

    private String field; //排序字段

    private String order; //排序规则

    private String startTime;

    private String endTime;
}
