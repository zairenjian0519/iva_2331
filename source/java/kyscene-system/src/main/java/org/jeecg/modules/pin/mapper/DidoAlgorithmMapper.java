package org.jeecg.modules.pin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.pin.entity.DidoAlgorithmDO;

import java.util.List;
import java.util.Map;

@Mapper
public interface DidoAlgorithmMapper extends BaseMapper<DidoAlgorithmDO> {
    List<Map<String, Object>> queryDataToCache();
}
