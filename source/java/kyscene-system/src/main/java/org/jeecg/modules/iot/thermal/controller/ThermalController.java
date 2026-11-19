/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.iot.thermal.ThermalService;
import org.jeecg.modules.iot.thermal.common.Res;
import org.jeecg.modules.iot.thermal.entity.ThermalDeviceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/11/18
 */
@RestController
@RequestMapping("/thermal")
public class ThermalController {

    @Autowired
    private ThermalService thermalService;

    /**
     * 获取测点类型列表
     */
    @GetMapping("/getMeterTypeList")
    public Result<?> getMeterTypeList() {
        return Result.OK(List.of(Res.string().getMeterTypeList()));
    }

    /**
     * 分页查询
     */
    @GetMapping("/listPage")
    public Result<?> list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize) {
        IPage<ThermalDeviceInfo> page =  new Page<>(pageNo, pageSize);
        return Result.OK(thermalService.queryPageList(page));
    }
    /**
     * 新增设备
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody ThermalDeviceInfo thermalDeviceInfo) {
        thermalService.addData(thermalDeviceInfo);
        return Result.OK();
    }

    /**
     * 编辑设备信息
     */
    @PostMapping("/edit")
    public Result<?> edit(@RequestBody ThermalDeviceInfo thermalDeviceInfo) {
        thermalService.updateData(thermalDeviceInfo);
        return Result.OK();
    }

    /**
     * 删除设备
     */
    @GetMapping("/delete")
    public Result<?> delete(@RequestParam String id) {
        thermalService.deleteRow(id);
        return Result.OK();
    }
}
