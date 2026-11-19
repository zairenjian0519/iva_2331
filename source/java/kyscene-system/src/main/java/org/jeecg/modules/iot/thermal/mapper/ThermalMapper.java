package org.jeecg.modules.iot.thermal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.iot.thermal.entity.ThermalDeviceInfo;

/**
 * @author： DecoderX
 * @date： 2025/11/18
 */
@Mapper
public interface ThermalMapper extends BaseMapper<ThermalDeviceInfo> {
}
