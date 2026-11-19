package org.jeecg.modules.serial.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.serial.entity.SoundPillar485;

import java.util.List;

/**
 * 音柱音频接口
 */
public interface SoundPillarVoiceService extends IService<SoundPillar485> {

    List<SoundPillar485> getSoundPillarVoices();
}
