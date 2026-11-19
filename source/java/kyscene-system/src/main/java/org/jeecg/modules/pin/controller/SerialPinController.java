package org.jeecg.modules.pin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.pin.entity.SerialPinDO;
import org.jeecg.modules.pin.service.SerialPinService;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/serialPin")
public class SerialPinController {

    private final SerialPinService serialPinService;

    /**
     * 获取表格数据
     */
    @PostMapping("/getPinSettingTable")
    public Result<IPage<SerialPinDO>> getPageData(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                  @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(serialPinService.getTableData(pageNo, pageSize));
    }

    /**
     * 新增GPO端点
     */
    @PostMapping("/addPinSetting")
    public Result<?> addPinSetting(@RequestBody SerialPinDO serialPinDO) {
        serialPinService.addData(serialPinDO);
        return Result.OK();
    }

    @PostMapping("/editPinSetting")
    public Result<?> editPinSetting(@RequestBody SerialPinDO serialPinDO) {
        serialPinService.updatePinSetting(serialPinDO);
        return Result.OK();
    }

    @GetMapping("/deletePinSetting")
    public Result<?> deletePinSetting(@RequestParam String id) {
        serialPinService.removeById(id);
        return Result.OK();
    }

    @GetMapping("/list")
    public Result<?> getList() {
        return Result.OK(serialPinService.getList());
    }

    @GetMapping("/testPin")
    public Result<?> testPin(@RequestParam String id) {
        serialPinService.testPin(id);
        return Result.ok();
    }
}
