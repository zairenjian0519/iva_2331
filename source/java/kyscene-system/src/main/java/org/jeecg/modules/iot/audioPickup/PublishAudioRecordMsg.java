/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.util.MinioUtil;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.audioPickup.entity.AudioMqttMsg;
import org.jeecg.modules.iot.audioPickup.entity.AudioPickup;
import org.jeecg.modules.iot.audioPickup.service.IAudioPickupService;
import org.jeecg.modules.iot.mqtt.IotMqttCustomOptions;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/8/5
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PublishAudioRecordMsg {

    private final IotMqttCustomOptions iotMqttCustomOptions;

    private final IAudioPickupService audioPickupService;

    @Async
    public void publishMsg(String id, String audioFilePath) {
        AudioPickup audioPickup = audioPickupService.getById(id);
        if (audioPickup == null) {
            log.error("音频采集任务不存在");
            return;
        }

        File zip = null;
        try {
            // 根据path, 打包为zip
            zip = ZipUtil.zip(audioFilePath);
            if (zip == null) {
                log.error("音频打包异常 zip 文件为 null");
                return;
            }
            // 上传到minio，/yyyy/MM/dd/fileName.zip
            String today = DateUtil.today();
            String yyyyMMdd = today.replaceAll("-", File.separator);
            String relativePath = yyyyMMdd + File.separator + FileUtil.getName(zip); // 相对于minio的根目录
            String httpFileUrl = MinioUtil.upload(FileUtil.getInputStream(zip), relativePath );
            // 组装mqtt消息体
            IotMsgEntity<AudioMqttMsg> iotMsgEntity = getIotMsgEntity(id, audioFilePath, httpFileUrl, audioPickup);
            if (iotMsgEntity == null) return;
            // 发送mqtt消息
            MqttClientService.getInstance().getMqttClient().pub(iotMqttCustomOptions.getPubTopic(), JSON.toJSONString(iotMsgEntity));
            log.info("mqtt发送成功，音频保存路径：{}, minio 地址：{}", audioFilePath, httpFileUrl);
        } catch (UtilException e) {
            log.error("音频打包异常：{}", e.getMessage(), e);
        } catch (MqttException e) {
            log.error("mqtt发送异常：{}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("音频上传异常：{}", e.getMessage(), e);
        } finally {
            FileUtil.del(zip); // 删除音频zip文件
        }
    }

    private IotMsgEntity<AudioMqttMsg> getIotMsgEntity(String id, String audioFilePath, String httpFileUrl,
                                                       AudioPickup audioPickup) {
        JSONObject bizParam = SysParameterUtil.getBusinessIpInf();
        if (bizParam == null) {
            log.error("业务IP配置异常");
            return null;
        }
        AudioMqttMsg audioMqttMsg = new AudioMqttMsg();
        audioMqttMsg.setId(id);
        audioMqttMsg.setLocation(AlarmHandleUtil.getGroupNames(audioPickup.getLocation()));
        audioMqttMsg.setDeviceName(audioPickup.getDeviceName());
        audioMqttMsg.setDeviceCode(audioPickup.getDeviceCode());
        audioMqttMsg.setRemark(audioPickup.getRemark());
        audioMqttMsg.setRtspUrl(audioPickup.getRtspUrl());
        audioMqttMsg.setRecordSegmentSeconds(audioPickup.getRecordSegmentSeconds());
        audioMqttMsg.setAudioPath(audioFilePath);
        audioMqttMsg.setIpAddress(audioPickup.getIpAddress());
        audioMqttMsg.setCreateTime(new Date());

        // audioFilePath = /home/super/audio_pickup/audio1939603519901331458/audio_20251027_142542.wav
        // audioMqttMsg.setOnlineAddress("http://" + bizParam.getString("bizIp") + ":" + bizParam.getInteger("bizPort") + "/alarmImg" + audioFilePath);
        audioMqttMsg.setOnlineAddress(httpFileUrl);
        IotMsgEntity<AudioMqttMsg> iotMsgEntity = new IotMsgEntity<>();
        iotMsgEntity.setType(DeviceEnum.AUDIO_PICKUP_REALTIME_DATA.getCode());
        iotMsgEntity.setData(audioMqttMsg);
        iotMsgEntity.setDeviceSn(SysParameterUtil.getDeviceSn());
        return iotMsgEntity;
    }
}