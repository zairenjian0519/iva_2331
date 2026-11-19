/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.elock;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.iot.gpio.GpioRealtimeDataEntity;
import org.jeecg.modules.iot.gpio.GpioRealtimeDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author： DecoderX
 * @date： 2025/10/20
 */
@RequestMapping("/elock/realtime")
@RestController
public class ElectricLockRealtimeController {

    @Autowired
    private GpioRealtimeDataService gpioRealtimeDataService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          GpioRealtimeDataEntity gpioRealtimeDataEntity) {
        IPage<GpioRealtimeDataEntity> page = gpioRealtimeDataService.getPageData(new Page<>(pageNo, pageSize), gpioRealtimeDataEntity);
        return Result.OK(page);
    }

    @GetMapping("/data")
    public Result<?> getData(@RequestParam String id) {
        return Result.OK(gpioRealtimeDataService.getRealTimeData(id));
    }

    @GetMapping("/delete")
    public Result<?> delete(@RequestParam String id) {
        return Result.OK(gpioRealtimeDataService.removeById(id));
    }

}
