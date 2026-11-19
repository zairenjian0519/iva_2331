/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup.controller;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.iot.audioPickup.RtspAudioRecorderService;
import org.jeecg.modules.iot.audioPickup.entity.AudioPickup;
import org.jeecg.modules.iot.audioPickup.service.IAudioPickupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 拾音器
 *
 * @author： DecoderX
 * @date： 2025/6/30
 */
@RestController
@RequestMapping("/audioRecoder")
public class AudioPickupController {

    @Autowired
    private RtspAudioRecorderService recorderService;

    @Autowired
    private IAudioPickupService audioPickupService;

    /**
     * 获取所有拾音器列表
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam Integer pageNo, @RequestParam Integer pageSize) {
        return Result.ok(audioPickupService.getAudioPickupList(pageNo, pageSize));
    }

    /**
     * 保存拾音器rtsp地址
     */
    @PostMapping("/save")
    public Result<?> save(@RequestBody AudioPickup audioPickup) {
        audioPickupService.saveData(audioPickup);
        return Result.OK("保存成功");
    }

    @PostMapping("/edit")
    public Result<?> edit(@RequestBody AudioPickup audioPickup) {
        audioPickupService.updateData(audioPickup);
        return Result.OK("修改成功");
    }

    @GetMapping("/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        return audioPickupService.deleteData(id);
    }

    @GetMapping("/start")
    public Result<?> start(@RequestParam String id) {
        return audioPickupService.startCollecting(id);
    }

    @GetMapping("/stop")
    public Result<?> stop(@RequestParam String id) {
        return audioPickupService.stopCollecting(id);
    }

    @GetMapping("/getAudioFileList")
    public Result<?> getAudioFileList(@RequestParam String id) {
        return Result.OK(audioPickupService.getAudioFileList(id));
    }

    @GetMapping("/deleteByPath")
    public Result<?> deleteByPath(@RequestParam String filePath) {
        return audioPickupService.deleteByPath(filePath);
    }

    @GetMapping("/stopAll")
    public Result<?> stopAll() {
        audioPickupService.stopAll();
        return Result.OK("操作成功");
    }
}