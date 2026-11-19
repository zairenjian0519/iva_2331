package org.jeecg.modules.alarmStatistics.entity;

import lombok.Data;

@Data
public class MainCountVo {
    //1：提醒，2：一般，3：警告，4：严重 0：正常
    //统计页面不统计为0的
    private Integer allCount;  //全部
//    private Integer normalCount; //0：正常
    private Integer remindCount; //1：提醒
    private Integer commonCount; //2：一般
    private Integer warningCount;//3：警告
    private Integer seriousCount;//4：严重
}
