/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.alarm;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.pin.service.DidoAlgorithmService;
import org.jeecg.modules.wireless.AlarmToSoundPillarHandle;
import org.jeecg.modules.wireless.SoundPillarService;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/7/28
 */
@Slf4j
public class OtherConsumerTask implements Runnable {

    private final List<JSONObject> warnAlgList;

    private final SoundPillarService soundPillarService;

    private final AlarmToSoundPillarHandle alarmToSoundPillarHandle;

    private final DidoAlgorithmService didoAlgorithmService;

    // 私有构造方法
    public OtherConsumerTask(List<JSONObject> warnAlgList) {
        soundPillarService = SpringContextUtils.getBean(SoundPillarService.class);
        alarmToSoundPillarHandle = SpringContextUtils.getBean(AlarmToSoundPillarHandle.class);
        didoAlgorithmService = SpringContextUtils.getBean(DidoAlgorithmService.class);
        this.warnAlgList = warnAlgList;
    }

    @Override
    public void run() {
        if (soundPillarService.isEnableSoundPillar()) {
            log.info("4G音柱已启用，下发控制指令，当前算法：{}", warnAlgList);
            if (ObjectUtil.isNotEmpty(warnAlgList)) {
                alarmToSoundPillarHandle.handleSoundPillarPlay(warnAlgList);
            }
        }
        if (didoAlgorithmService.enableDido()) {
            log.info("dido已启用，下发控制指令，当前算法：{}", warnAlgList);
            if (ObjectUtil.isNotEmpty(warnAlgList))
                didoAlgorithmService.executeGpoCommand(warnAlgList);
        }
    }
}
