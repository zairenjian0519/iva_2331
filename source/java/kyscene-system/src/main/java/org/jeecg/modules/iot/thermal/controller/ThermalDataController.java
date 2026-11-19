/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.iot.thermal.ThermalRealDataService;
import org.jeecg.modules.iot.thermal.entity.ThermalRealtimeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author： DecoderX
 * @date： 2025/11/19
 */
@RestController
@RequestMapping("/thermalData")
public class ThermalDataController {

    @Autowired
    private ThermalRealDataService thermalRealDataService;

    @GetMapping("/realData")
    public Result<?> realData(@RequestParam String deviceId) {
        thermalRealDataService.queryRealTimeData(deviceId);
        return Result.OK();
    }

    @GetMapping("/lastRecord")
    public Result<?> lastRecord(@RequestParam String deviceId) {
        return Result.OK(thermalRealDataService.getLastRecord(deviceId));
    }

    @GetMapping("/page")
    public Result<?> page(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                         @RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize) {
        IPage<ThermalRealtimeData> page =  new Page<>(pageNo, pageSize);
        return Result.OK(thermalRealDataService.listPage(page));
    }

    @GetMapping("/delete")
    public Result<?> delete(@RequestParam String id) {
        thermalRealDataService.removeById(id);
        return Result.OK();
    }
}
