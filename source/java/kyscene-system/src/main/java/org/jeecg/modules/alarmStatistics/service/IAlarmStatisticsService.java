package org.jeecg.modules.alarmStatistics.service;

import org.jeecg.modules.alarmStatistics.entity.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @Description: 统计分析表
 * @Author: jeecg-boot
 * @Date:   2022-08-12
 * @Version: V1.0
 */
public interface IAlarmStatisticsService extends IService<AlarmStatistics> {
    List<CountVo> getCameraCountList(List<String> cameraIds,List<String> categorys,String startTime,String endTime);
    List<CountVo> getTypeCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime);
    List<HourCountVo> getHourCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime);
    List<DateCountVo> getDateCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime);
    List<CountVo> getLocationCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime);

    int getAllCount(List<String> cameraIds, List<String> categorys, String startTime, String endTime);

    MainCountVo getMainCount(List<String> cameraIds, List<String> categorys, String startTime, String endTime);
}
