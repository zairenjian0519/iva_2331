/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.alarmFace.AlarmFaceDo;
import org.jeecg.modules.alarmFace.AlarmFaceService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/4/8
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AlarmFaceHandler {

    private final AlarmFaceService alarmFaceService;

    @Async
    public void handleData(AlarmDetail alarmDetail) {
        AlarmFaceDo alarmFaceDo = getAlarmFaceDo(alarmDetail);
        if (alarmFaceDo == null) return;
        alarmFaceService.saveAlarmFace(alarmFaceDo);
    }

    public void handleBatchData(List<AlarmDetail> batch) {
        if (batch == null || batch.isEmpty()) return;
        List<AlarmFaceDo> alarmFaceDos = batch.stream().map(this::getAlarmFaceDo).toList();
        alarmFaceService.saveBatch(alarmFaceDos);
    }

    private AlarmFaceDo getAlarmFaceDo(AlarmDetail alarmDetail) {
        if (alarmDetail == null) return null;
        AlarmFaceDo alarmFaceDo = new AlarmFaceDo();
        alarmFaceDo.setAlarmType(alarmDetail.getAlarmType());
        alarmFaceDo.setAlarmTime(alarmDetail.getAlarmTime());
        alarmFaceDo.setAlarmId(alarmDetail.getAlarmId());
        alarmFaceDo.setCameraId(alarmDetail.getCameraId());
        alarmFaceDo.setCameraName(alarmDetail.getCameraName());
        alarmFaceDo.setPicPath(alarmDetail.getPicPath());
        alarmFaceDo.setThumbnailPath(alarmDetail.getThumbnailPath());
        alarmFaceDo.setCreateTime(alarmDetail.getCreateTime());
        alarmFaceDo.setIsRotate(alarmDetail.getIsRotate());
        return alarmFaceDo;
    }
}
