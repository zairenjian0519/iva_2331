/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.elock;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.iot.gpio.GpioReaderEntity;
import org.jeecg.modules.iot.gpio.GpioReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 电磁锁
 *
 * @author： DecoderX
 * @date： 2025/10/13
 */
@RestController
@RequestMapping("/elock")
public class ElectricLockController {

    @Autowired
    private GpioReaderService electricLockService;

    /**
     * 获取所有数据
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public Result<?> page(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<GpioReaderEntity> page = electricLockService.getPageData(new Page<>(pageNo, pageSize));
        return Result.OK(page);
    }

    /**
     * 新增数据
     *
     * @param gpioReaderEntity
     * @return
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody GpioReaderEntity gpioReaderEntity) {
        electricLockService.saveData(gpioReaderEntity);
        return Result.OK();
    }

    /**
     * 修改数据
     *
     * @param gpioReaderEntity
     * @return
     */
    @PostMapping("/edit")
    public Result<?> edit(@RequestBody GpioReaderEntity gpioReaderEntity) {
        electricLockService.editData(gpioReaderEntity);
        return Result.OK();
    }

    /**
     * 删除数据
     *
     * @param id
     * @return
     */
    @GetMapping("/delete")
    public Result<?> delete(String id) {
        electricLockService.deleteData(id);
        return Result.OK();
    }

    /**
     * 获取数据
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public Result<?> get(String id) {
        return Result.OK(electricLockService.getById(id));
    }

    /**
     * 实时数据查看
     *
     * @return
     */
    @GetMapping("/realTimeData")
    public Result<?> realTimeData(String id) {
        return Result.OK(electricLockService.getById(id));
    }
}
