package org.jeecg.modules.serial.controller;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.serial.service.SoundPillarVoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 音柱音频controller
 */
@RestController
@RequestMapping("/soundPillar")
public class SoundPillarVoiceController {

    @Autowired
    private SoundPillarVoiceService soundPillarVoiceService;


    @GetMapping("/listVoices")
    public Result<?> getSoundPillarVoices(){
        return Result.OK(soundPillarVoiceService.getSoundPillarVoices());
    }
}
