/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.heart;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.audioPickup.entity.AudioPickup;
import org.jeecg.modules.iot.audioPickup.service.IAudioPickupService;
import org.jeecg.modules.iot.gpio.GpioReaderEntity;
import org.jeecg.modules.iot.gpio.GpioReaderService;
import org.jeecg.modules.iot.thermal.ThermalService;
import org.jeecg.modules.iot.thermal.entity.ThermalDeviceInfo;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author： DecoderX
 * @date： 2025/10/28
 */
@Service
public class DeviceHeartService {

    @Autowired
    private GpioReaderService readerService;

    @Autowired
    private ThermalService thermalService;

    @Autowired
    private IAudioPickupService audioPickupService;

    @Autowired
    private ICameraManagerService cameraManagerService;

    public Map<String, Object> getDeviceHeartInfo() {
        return Map.of(
                DeviceEnum.ELECTRIC_LOCK_HEART.getCode(), getElectricLockInfo(),
                DeviceEnum.TH_SENSOR_HEART.getCode(), getThSensorInfo(),
                DeviceEnum.AUDIO_PICKUP_HEART.getCode(), getAudioPickupInfo(),
                DeviceEnum.CAMERA_DEVICE_HEART.getCode(), getCameraInfo()
        );
    }

    // 获取所有摄像头信息
    private List<CameraDeviceHeartVo> getCameraInfo() {
        List<CameraManager> cameraManagers = cameraManagerService.list();
        return cameraManagers.stream().map(item -> {
            CameraDeviceHeartVo vo = new CameraDeviceHeartVo();
            vo.setDeviceId(item.getId());
            vo.setDeviceName(item.getCameraName());
            vo.setDeviceCode(item.getCameraNumber());
            vo.setLocation(AlarmHandleUtil.getGroupNames(item.getLocation()));
            vo.setTimestamp(item.getCreateTime());
            vo.setIpAddress(item.getIp());

            if (item.getCameraStatus().equals("1")) { // 在线
                vo.setOfflineReason("");
                vo.setStatus(1);
            } else {
                vo.setStatus(0);
                vo.setOfflineReason(item.getOfflineReason());
            }
            return vo;
        }).toList();
    }


    // 获取所有音频拾取器信息
    private List<AudioDeviceHeartVo> getAudioPickupInfo() {
        List<AudioPickup> list = audioPickupService.list();
        return list.stream().map(item -> {
            AudioDeviceHeartVo vo = new AudioDeviceHeartVo();
            vo.setDeviceId(item.getId());
            vo.setDeviceName(item.getDeviceName());
            vo.setDeviceCode(item.getDeviceCode());
            vo.setStatus(item.getStatus());
            vo.setLocation(AlarmHandleUtil.getGroupNames(item.getLocation()));
            vo.setTimestamp(item.getUpdateTime());
            vo.setIpAddress(item.getIpAddress());
            vo.setCollectStatus(item.getCollectStatus());
            return vo;
        }).toList();
    }

    // 获取所有温湿度传感器信息
    private List<DeviceHeartVo> getThSensorInfo() {
        List<ThermalDeviceInfo> list = thermalService.list();
        return list.stream().map(item -> {
            DeviceHeartVo vo = new DeviceHeartVo();
            vo.setDeviceId(item.getId());
            vo.setDeviceName(item.getDeviceName());
            vo.setDeviceCode(item.getDeviceCode());
            vo.setStatus(item.getStatus());
            vo.setLocation(AlarmHandleUtil.getGroupNames(item.getLocation()));
            vo.setTimestamp(item.getUpdateTime());
            vo.setIpAddress(item.getIpAddress());
            return vo;
        }).toList();
    }

    // 获取所有电磁锁信息
    private List<DeviceHeartVo> getElectricLockInfo() {
        JSONObject businessIpInf = SysParameterUtil.getBusinessIpInf();
        String ip;
        if (ObjectUtil.isNotEmpty(businessIpInf)) {
            ip = businessIpInf.getString("bizIp");
        } else {
            ip = "";
        }
        List<GpioReaderEntity> list = readerService.list();
        return list.stream().map(item -> {
            DeviceHeartVo vo = new DeviceHeartVo();
            vo.setDeviceId(item.getId());
            vo.setDeviceName(item.getDeviceName());
            vo.setDeviceCode(item.getDeviceCode());
            vo.setStatus(item.getStatus());
            vo.setLocation(AlarmHandleUtil.getGroupNames(item.getLocation()));
            vo.setTimestamp(item.getUpdateTime());
            vo.setIpAddress(ip);
            return vo;
        }).toList();
    }
}
