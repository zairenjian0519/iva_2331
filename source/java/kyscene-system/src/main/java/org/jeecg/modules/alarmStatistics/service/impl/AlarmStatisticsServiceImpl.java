package org.jeecg.modules.alarmStatistics.service.impl;

import org.jeecg.modules.alarmStatistics.entity.*;
import org.jeecg.modules.alarmStatistics.mapper.AlarmStatisticsMapper;
import org.jeecg.modules.alarmStatistics.service.IAlarmStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;


/**
 * @Description: 统计分析表
 * @Author: jeecg-boot
 * @Date:   2022-08-12
 * @Version: V1.0
 */
@Service
public class AlarmStatisticsServiceImpl extends ServiceImpl<AlarmStatisticsMapper, AlarmStatistics> implements IAlarmStatisticsService {
    @Autowired
    AlarmStatisticsMapper alarmStatisticsMapper;
    @Override
    public List<CountVo> getCameraCountList(List<String> cameraIds,List<String> categorys,String startTime,String endTime){
        return alarmStatisticsMapper.cameraCountList(cameraIds,categorys,startTime,endTime);
    }
    @Override
    public List<CountVo> getTypeCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime){
        return alarmStatisticsMapper.typeCountList(cameraIds,categorys,startTime,endTime);
    }
    @Override
    public List<HourCountVo> getHourCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime){
        return alarmStatisticsMapper.hourCountList(cameraIds,categorys,startTime,endTime);
    }
    @Override
    public List<DateCountVo> getDateCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime){
        return alarmStatisticsMapper.dateCountList(cameraIds,categorys,startTime,endTime);
    }

    @Override
    public List<CountVo> getLocationCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime) {
        return alarmStatisticsMapper.locationCountList(cameraIds,categorys,startTime,endTime);
    }


    @Override
    public int getAllCount(List<String> cameraIds, List<String> categorys, String startTime, String endTime) {
        return alarmStatisticsMapper.allCount(cameraIds,categorys,startTime,endTime);
    }


    @Override
    public MainCountVo getMainCount(List<String> cameraIds, List<String> categorys, String startTime, String endTime) {
        return alarmStatisticsMapper.mainCount(cameraIds,categorys,startTime,endTime);
    }
}
