/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.iot.thermal.entity.ThermalRealtimeData;

/**
 * @author： DecoderX
 * @date： 2025/11/19
 */
@Mapper
public interface ThermalDataMapper extends BaseMapper<ThermalRealtimeData> {
}
