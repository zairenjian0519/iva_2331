/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.gpio;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author： DecoderX
 * @date： 2025/10/13
 */
public interface GpioReaderService extends IService<GpioReaderEntity> {

    IPage<GpioReaderEntity> getPageData(IPage<GpioReaderEntity> page);

    void saveData(GpioReaderEntity gpioReaderEntity);

    void editData(GpioReaderEntity gpioReaderEntity);

    void deleteData(String id);

    GpioReaderEntity getByPinNumber(String pinNumber);
}
