package org.jeecg.modules.alarmDetail.entity;

import lombok.Data;

import java.util.List;

@Data
public class AlarmDetailCountVo {
    private Integer allCount;
    private Integer todayCount;
    private Integer seriousCount;
    private List<AlarmDetail> todayList;
    private List<AlarmDetail> seriousList;
}
