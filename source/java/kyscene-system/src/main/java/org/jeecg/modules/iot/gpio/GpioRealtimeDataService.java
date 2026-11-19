package org.jeecg.modules.iot.gpio;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author： DecoderX
 * @date： 2025/10/20
 */
public interface GpioRealtimeDataService extends IService<GpioRealtimeDataEntity> {

    void saveData(GpioRealtimeDataEntity gpioRealtimeDataEntity);

    void readData(String pinNumber);

    void readAllDeviceData();

    String getRealTimeData(String id);

    IPage<GpioRealtimeDataEntity> getPageData(Page<GpioRealtimeDataEntity> page, GpioRealtimeDataEntity gpioRealtimeDataEntity);
}
