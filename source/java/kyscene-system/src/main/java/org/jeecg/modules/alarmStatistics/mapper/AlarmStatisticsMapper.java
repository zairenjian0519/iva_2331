package org.jeecg.modules.alarmStatistics.mapper;

import org.jeecg.modules.alarmStatistics.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * @Description: 统计分析表
 * @Author: jeecg-boot
 * @Date:   2022-08-12
 * @Version: V1.0
 */
public interface AlarmStatisticsMapper extends BaseMapper<AlarmStatistics> {
    List<CountVo> cameraCountList(List<String> cameraIds,List<String> categorys,String startTime,String endTime);
    List<CountVo> typeCountList(List<String> cameraIds,List<String> categorys,String startTime,String endTime);
    List<HourCountVo> hourCountList(List<String> cameraIds,List<String> categorys,String startTime,String endTime);
    List<DateCountVo> dateCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime);
    List<CountVo> locationCountList(List<String> cameraIds, List<String> categorys, String startTime, String endTime);

    int allCount(List<String> cameraIds, List<String> categorys, String startTime, String endTime);

    MainCountVo mainCount(List<String> cameraIds, List<String> categorys, String startTime, String endTime);
}
