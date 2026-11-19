/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
public interface THRealDataService extends IService<THRealtimeData> {

    void saveData(THRealtimeData thRealtimeData);

    void handleData(Integer slaveAddr, Double temperature, Double humidity);

    THRealtimeData getLastData(String id);

    IPage<THRealtimeData> listPage(String id, Page<THRealtimeData> objectPage);

    void collectAllDeviceData();

    void checkDeviceStatus();
}
