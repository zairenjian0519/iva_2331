package org.jeecg.modules.alarmStatistics.entity;

import lombok.Data;

@Data
public class DateCountVo {
    //日期
    private String date;
    //告警数量
    private Integer value;
    //严重
    private Integer seriousValue;
    //告警
    private Integer warningValue;
    //一般
    private Integer commonValue;
    //提醒
    private Integer remindValue;

}
