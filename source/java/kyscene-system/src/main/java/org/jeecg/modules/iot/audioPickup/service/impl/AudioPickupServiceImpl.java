/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.audioPickup.AudioPickupConst;
import org.jeecg.modules.iot.audioPickup.AudioPickupProperties;
import org.jeecg.modules.iot.audioPickup.RtspAudioRecorderService;
import org.jeecg.modules.iot.audioPickup.entity.AudioPickup;
import org.jeecg.modules.iot.audioPickup.mapper.AudioPickupMapper;
import org.jeecg.modules.iot.audioPickup.service.IAudioPickupService;
import org.jeecg.modules.iot.mqtt.IotMqttCustomOptions;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.util.CustomUtil;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/6/30
 */

@Service
public class AudioPickupServiceImpl extends ServiceImpl<AudioPickupMapper, AudioPickup> implements IAudioPickupService {

    @Autowired
    private AudioPickupProperties audioPickupProperties;

    @Autowired
    private RtspAudioRecorderService rtspAudioRecorderService;

    @Autowired
    private IotMqttCustomOptions iotMqttCustomOptions;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public IPage<AudioPickup> getAudioPickupList(Integer pageNo, Integer pageSize) {
        Page<AudioPickup> page = new Page<>(pageNo, pageSize);
        return this.page(page, Wrappers.lambdaQuery(AudioPickup.class).orderByDesc(AudioPickup::getCreateTime));
    }

    @Override
    public void saveData(AudioPickup audioPickup) {
        // 判断名称或者rtsp地址是否重复
        String pickupName = audioPickup.getDeviceName();
        if (StrUtil.isBlank(pickupName)) {
            throw new JeecgBootException("名称不能为空");
        }

        String rtspUrl = audioPickup.getRtspUrl();
        if (StrUtil.isBlank(rtspUrl)) {
            throw new JeecgBootException("rtsp地址不能为空");
        }

        String deviceCode = audioPickup.getDeviceCode();
        if (StrUtil.isBlank(deviceCode)) {
            throw new JeecgBootException("设备编码不能为空");
        }

        long nameCount = this.count(Wrappers.lambdaQuery(AudioPickup.class).eq(AudioPickup::getDeviceName, pickupName));
        if (nameCount > 0) {
            throw new JeecgBootException("名称重复");
        }

        long rtspCount = this.count(Wrappers.lambdaQuery(AudioPickup.class).eq(AudioPickup::getRtspUrl, rtspUrl));
        if (rtspCount > 0) {
            throw new JeecgBootException("rtsp地址重复");
        }

        long count = this.count(Wrappers.lambdaQuery(AudioPickup.class).eq(AudioPickup::getDeviceCode, deviceCode));
        if (count > 0) {
            throw new JeecgBootException("设备编码重复");
        }

        // 从rtsp地址中获取ip地址 rtsp://user:password@192.168.3.108/h264/ch1/main/av_stream0
        String ip = CustomUtil.getIpFromRtsp(rtspUrl);
        audioPickup.setIpAddress(ip);

        // 通过ip判断设备是否在线
        audioPickup.setStatus(CustomUtil.isOnlineFromIp(ip));

        // 采集状态初始为 0
        audioPickup.setCollectStatus(0);
        this.save(audioPickup);
        cacheAudioPickup();
        syncAudioPickupToCloudPlatform();
    }

    @Override
    public void updateData(AudioPickup audioPickup) {
        String pickupName = audioPickup.getDeviceName();
        if (StrUtil.isNotBlank(pickupName)) {
            long nameCount = this.count(Wrappers.lambdaQuery(AudioPickup.class)
                    .eq(AudioPickup::getDeviceName, pickupName)
                    .ne(AudioPickup::getId, audioPickup.getId()));
            if (nameCount > 0) {
                throw new JeecgBootException("音频采集点名称已存在");
            }
        }
        String rtspUrl = audioPickup.getRtspUrl();
        if (StrUtil.isNotBlank(rtspUrl)) {
            long rtspCount = this.count(Wrappers.lambdaQuery(AudioPickup.class)
                    .eq(AudioPickup::getRtspUrl, rtspUrl)
                    .ne(AudioPickup::getId, audioPickup.getId()));
            if (rtspCount > 0) {
                throw new JeecgBootException("音频采集点RtspUrl已存在");
            }
        }

        String deviceCode = audioPickup.getDeviceCode();
        if (StrUtil.isNotBlank(deviceCode)) {
            long count = this.count(Wrappers.lambdaQuery(AudioPickup.class)
                    .eq(AudioPickup::getDeviceCode, deviceCode)
                    .ne(AudioPickup::getId, audioPickup.getId()));
            if (count > 0) {
                throw new JeecgBootException("设备编码已存在");
            }
        }
        String ip = CustomUtil.getIpFromRtsp(rtspUrl);
        audioPickup.setIpAddress(ip);
        audioPickup.setStatus(CustomUtil.isOnlineFromIp(ip));

        this.updateById(audioPickup);
        cacheAudioPickup();
        syncAudioPickupToCloudPlatform();
    }

    @Override
    public Result<?> deleteData(String id) {
        this.removeById(id);
        cacheAudioPickup();
        syncAudioPickupToCloudPlatform();
        // 删除后需要停止录制
        return rtspAudioRecorderService.stopRecording(id);
    }

    @Override
    public Result<?> startCollecting(String id) {
        AudioPickup audioPickup = this.getById(id);
        String storePath = audioPickupProperties.getPath() + "audio" + id;
        Result<?> result = rtspAudioRecorderService.startRecording(audioPickup.getRtspUrl(),
                audioPickup.getRecordSegmentSeconds(), storePath, id);
        if (result.isSuccess()) {
            AudioPickup audioPickupUpdate = new AudioPickup();
            audioPickupUpdate.setId(id);
            audioPickupUpdate.setCollectStatus(1);
            this.updateById(audioPickupUpdate);
        }
        return result;
    }

    @Override
    public Result<?> stopCollecting(String id) {
        Result<?> stopped = rtspAudioRecorderService.stopRecording(id);
        if (stopped.isSuccess()) {
            AudioPickup audioPickupUpdate = new AudioPickup();
            audioPickupUpdate.setId(id);
            audioPickupUpdate.setCollectStatus(0);
            this.updateById(audioPickupUpdate);
        }
        return stopped;
    }

    @Override
    public JSONArray getAudioFileList(String id) {
        // AudioPickup audioPickup = this.getById(id);
        String storePath = audioPickupProperties.getPath() + "audio" + id;
        File file = new File(storePath);
        if (!file.exists()) {
            return new JSONArray();
        }
        File[] files = file.listFiles();
        if (files == null) {
            return new JSONArray();
        }
        JSONArray fileList = new JSONArray();
        for (File f : files) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("fileName", f.getName());
            // 获取文件大小,转为MB, 保留两位小数
            // jsonObject.put("fileSize", f.length() / 1024 / 1024);
            jsonObject.put("fileSize", String.format("%.2f", f.length() / 1024.0 / 1024.0));
            jsonObject.put("fileModifiedTime", f.lastModified());
            jsonObject.put("filePath", f.getAbsolutePath());
            fileList.add(jsonObject);
        }
        return fileList;
    }

    @Override
    public Result<?> deleteByPath(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return Result.error("文件不存在");
        }
        if (file.isDirectory()) {
            return Result.error("不能删除文件夹");
        }
        if (!file.delete()) {
            return Result.error("删除失败");
        }
        return Result.OK("删除成功");
    }

    @Override
    public void stopAll() {
        rtspAudioRecorderService.shutdownAll();
    }

    // 缓存音频采集点列表
    private void cacheAudioPickup() {
        List<AudioPickup> audioPickups = this.list();
        if (ObjectUtil.isNotEmpty(audioPickups)) {
            redisUtil.set(AudioPickupConst.Audio_Pickup_Device_Cache_Key, JSON.toJSONString(audioPickups));
        }
    }

    // 通过mqtt同步音频采集器到云平台
    private void syncAudioPickupToCloudPlatform() {
        List<AudioPickup> audioPickups = this.list();
        if (ObjectUtil.isNotEmpty(audioPickups)) {

            // 格式化location 字段
            for (AudioPickup audioPickup : audioPickups) {
                audioPickup.setLocation(AlarmHandleUtil.getGroupNames(audioPickup.getLocation()));

            }
            IotMsgEntity<List<AudioPickup>> iotMsgEntity = new IotMsgEntity<>();
            iotMsgEntity.setData(audioPickups);
            iotMsgEntity.setType(DeviceEnum.AUDIO_PICKUP_DEVICE_LIST.getCode());
            iotMsgEntity.setDeviceSn(SysParameterUtil.getDeviceSn());
            try {
                MqttClientService.getInstance().getMqttClient().pub(iotMqttCustomOptions.getPubTopic(), JSON.toJSONString(iotMsgEntity));
            } catch (MqttException e) {
                log.error("同步音频采集器到云平台失败", e);
            }
        }
    }
}