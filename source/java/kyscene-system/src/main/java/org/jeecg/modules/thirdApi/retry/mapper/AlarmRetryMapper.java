package org.jeecg.modules.thirdApi.retry.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.thirdApi.retry.AlarmRetryData;

import java.util.List;

@Mapper
public interface AlarmRetryMapper extends BaseMapper<AlarmRetryData> {

    @Select("select * from alarm_retry_data order by alarm_time asc limit 50")
    List<AlarmRetryData> selectAlarmRetryList();

    /**
     * 清空表
     */
    @Update("truncate table alarm_retry_data")
    void truncateTable();
}
