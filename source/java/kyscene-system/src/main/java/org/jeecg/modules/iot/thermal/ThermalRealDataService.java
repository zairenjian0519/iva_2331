package org.jeecg.modules.iot.thermal;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.iot.thermal.entity.ThermalRealtimeData;
import org.jeecg.modules.iot.thermal.module.TemperQueryResult;

/**
 * @author： DecoderX
 * @date： 2025/11/19
 */
public interface ThermalRealDataService extends IService<ThermalRealtimeData> {

    void queryRealTimeData(String deviceId);

    IPage<ThermalRealtimeData> listPage(IPage<ThermalRealtimeData> page);

    ThermalRealtimeData getLastRecord(String deviceId);

    void collectAllDeviceData();
}
