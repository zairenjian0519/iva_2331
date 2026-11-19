/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.api.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/10/21
 */
@RequestMapping("/serial485Reader")
@RestController
public class Serial485ReaderController {

    @Autowired
    private Serial485ReaderService serial485ReaderService;

    @Autowired
    private Serial485Service serial485Service;

    @GetMapping("/queryPage")
    public Result<IPage<Serial485Reader>> queryPageList(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(serial485ReaderService.queryPageList(new Page<>(pageNo, pageSize)));
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody Serial485Reader serial485Reader) {
        serial485ReaderService.addData(serial485Reader);
        return Result.OK();
    }

    @PostMapping("/edit")
    public Result<?> edit(@RequestBody Serial485Reader serial485Reader) {
        serial485ReaderService.updateData(serial485Reader);
        return Result.OK();
    }

    @GetMapping("/delete")
    public Result<?> delete(@RequestParam String id) {
        serial485ReaderService.deleteData(id);
        return Result.OK();
    }

    @GetMapping("/getCommInfo")
    public Result<Serial485Setting> getCommInfo() {
        return Result.OK(serial485ReaderService.getCommInfo());
    }

    /**
     * 连接串口
     */
    @PostMapping("/saveCommAndOpen")
    public Result<?> saveCommPortSetting(@RequestBody Serial485Setting serial485Setting) {
        serial485ReaderService.saveToSystemParam(serial485Setting);
        return Result.OK();
    }

    @GetMapping("/getCommPorts")
    public Result<List<String>> getCommPorts() {
        return Result.OK(serial485Service.listAvailableSerialPorts());
    }

    @GetMapping("/closeCommPort")
    public Result<?> closeCommPort() {
        serial485ReaderService.disconnectCommPort();
        return Result.OK();
    }

    @GetMapping("/sendCommand")
    public Result<?> sendCommand(@RequestParam String id) {
        serial485ReaderService.sendCommand(id);
        return Result.OK();
    }
}
