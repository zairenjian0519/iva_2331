package org.jeecg.modules.dataPushHistory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.dataPushHistory.entity.DataPushHistory;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/6/5
 */
@Mapper
public interface PushHistoryMapper extends BaseMapper<DataPushHistory> {

    /**
     * 获取 10000条之后的数据
     */
    List<DataPushHistory> selectHistoryAfterNum(@Param("clearNum") Integer clearNum, @Param("offsetNum") Integer offsetNum);
}
