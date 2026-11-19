package org.jeecg.modules.serial.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.serial.config.SerialPortUtil;
import org.jeecg.modules.serial.entity.SerialSettingsVo;
import org.jeecg.modules.serial.entity.SoundPillarParam;
import org.jeecg.modules.serial.service.SerialPortService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/serial")
public class SerialController {

    @Autowired
    private SerialPortService serialPortService;

    /**
     * 获取端口列表
     */
    @GetMapping("/getSerialPortList")
    public Result<?> getSerialPortList() {
        return Result.OK(SerialPortUtil.getSerialPortList());
    }

    /**
     * 获取算法列表
     */
    @GetMapping("/getAlgorithmList")
    public Result<?> getAlgorithmList() {
        List<KyCategory> algorithmList = serialPortService.getAlgorithmList();
        return Result.OK(algorithmList);
    }

    @PostMapping("/testVoice")
    public Result<?> testVoice(@RequestBody SoundPillarParam soundPillarParam) {
        serialPortService.playVoice(soundPillarParam);
        return Result.OK();
    }

    @PostMapping("/closeVoice")
    public Result<?> closeVoice(@RequestBody JSONObject params) {
        String serialName = params.getString("serialName");
        String closeCode = params.getString("closeCode");
        serialPortService.closeVoice(serialName, closeCode);
        return Result.OK();
    }

    @PostMapping("/saveSettings")
    public Result<?> saveSettings(@RequestBody SerialSettingsVo serialSettingsVo) {
        serialPortService.saveOrUpdateSettings(serialSettingsVo);
        return Result.OK();
    }

    @PostMapping("/getSettingList")
    public Result<IPage<SerialSettingsVo>> getSettingList(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return serialPortService.getSettingList(pageNo, pageSize);
    }

    @GetMapping("/deleteSetting")
    public Result<?> deleteSetting(@RequestParam String id){
        serialPortService.deleteSetting(id);
        return Result.ok();
    }
}
