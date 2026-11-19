package org.jeecg.modules.serial.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.serial.entity.SoundPillar485;
import org.jeecg.modules.serial.mapper.xml.SoundPillarVoiceMapper;
import org.jeecg.modules.serial.service.SoundPillarVoiceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SoundPillarVoiceServiceImpl extends ServiceImpl<SoundPillarVoiceMapper, SoundPillar485> implements SoundPillarVoiceService {


    @Override
    public List<SoundPillar485> getSoundPillarVoices() {
        return list();
    }
}
