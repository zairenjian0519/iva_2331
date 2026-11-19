package org.jeecg.modules.deviceManagement.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.mapper.DeviceManagementMapper;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.iot.mqtt.IotMqttCustomOptions;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.thirdApi.pojo.DeviceInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 设备管理
 * @Author: jeecg-boot
 * @Date: 2022-08-23
 * @Version: V1.0
 */
@Service
@Slf4j
public class DeviceManagementServiceImpl extends ServiceImpl<DeviceManagementMapper, DeviceManagement> implements IDeviceManagementService {

    @Autowired
    DeviceManagementMapper managementMapper;

    @Override
    public int getUsedAlgNum(String id) {
        return managementMapper.getUsedAlgNum(id);
    }

    @Override
    public int getUsedCameraNum(String id) {
        return managementMapper.getUsedCameraNum(id);
    }

    @Override
    public DeviceManagement getByCameraId(String cameraId) {
        return managementMapper.getByCameraId(cameraId);
    }

    @Override
    public void initMqtt() {
        try {
            List<DeviceManagement> deviceManagements = managementMapper.selectList(null);
            if (ObjectUtil.isEmpty(deviceManagements)) {
                log.info("未查询到超脑设备，取消初始化设备MQTT");
                return;
            }
            for (DeviceManagement dev : deviceManagements) {
                String devIp = dev.getDeviceIp();
                if (StrUtil.isNotBlank(devIp)) {
                    MqttClientService.getInstance().getMqttClient(dev.getDeviceIp());
                    log.info("初始化设备MQTT成功, 设备IP：{}", devIp);
                }

            }
        } catch (Exception e) {
            log.error("初始化设备MQTT异常：{}", e.getLocalizedMessage());
        }
    }

    @Override
    public void initCustomMqtt() {
        log.info("初始化拓明MQTT客户端");
        IotMqttCustomOptions mqttCustomOptions = SpringContextUtils.getBean(IotMqttCustomOptions.class);
        mqttCustomOptions.initMqttCustomOptions();
        try {
            MqttClientConnect mqttClient = MqttClientService.getInstance().getMqttClient();
            String subTopic = mqttCustomOptions.getSubTopic();
            if (StrUtil.isNotBlank(subTopic)) {
                mqttClient.sub(subTopic);
            }
            log.info("初始化拓明MQTT客户端成功: ClientId: {}, IP: {}, SubTopic: {}",
                    mqttClient.getMqttClient().getClientId(), mqttCustomOptions.getHost(), mqttCustomOptions.getSubTopic());
            // 发送一次心跳
            // DeviceHeart.publishHeartbeat(mqttClient);
        } catch (MqttException e) {
            log.error("初始化拓明MQTT客户端失败:{}", e.getMessage(), e);
        }
    }

    @Override
    public void removeAlarmByDeviceId(String devId) {
        managementMapper.removeAuditByDeviceId(devId);
        managementMapper.removeAlarmByDeviceId(devId);
    }

    @Override
    public DeviceManagement getByAlgorithmId(String id) {
        return managementMapper.getDeviceIp(id);
    }

    @Override
    public void updateIdByIp(String ip, String id) {
        managementMapper.updateIdByIp(ip, id);
    }


    @Override
    public DeviceInfoVo getDeviceInfo(String deviceIp) {
        DeviceManagement deviceManagement = this.getOne(Wrappers.lambdaQuery(DeviceManagement.class)
                .eq(DeviceManagement::getDeviceIp, deviceIp), false);
        if (ObjectUtil.isEmpty(deviceManagement)) return null;
        // 初始化返回数据
        DeviceInfoVo deviceInfoVo = new DeviceInfoVo();
        deviceInfoVo.setId(deviceManagement.getId());
        deviceInfoVo.setDeviceName(deviceManagement.getDeviceName());
        deviceInfoVo.setDeviceIp(deviceManagement.getDeviceIp());
        deviceInfoVo.setCategory(deviceManagement.getCategory());
        deviceInfoVo.setCreateBy(deviceManagement.getCreateBy());
        deviceInfoVo.setCreateTime(deviceManagement.getCreateTime());
        // 从缓存读取deviceInfo
        RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
        JSONObject cachedDevInfo = (JSONObject) redisUtil.get(deviceIp + "_deviceInfo");
        if (null != cachedDevInfo){
            try {
                MqttClientConnect clientConnect = MqttClientService.getInstance().getMqttClient(deviceIp);
                // 离线
                deviceInfoVo.setStatus("0");
                // 在线
                if (null != clientConnect && clientConnect.getMqttClient().isConnected())
                    deviceInfoVo.setStatus("1");
            } catch (MqttException e) {
                deviceInfoVo.setStatus("0");
                log.info(e.getMessage());
            }
        }
        // 更新设备状态
        DeviceManagement updateTempData = new DeviceManagement();
        updateTempData.setStatus(deviceInfoVo.getStatus());
        updateTempData.setId(deviceInfoVo.getId());
        this.updateById(updateTempData);

        return deviceInfoVo;
    }

    @Override
    public Result<?> updateDeviceStatus(String id, String status) {
        DeviceManagement deviceManagement = this.getById(id);
        if (ObjectUtil.isEmpty(deviceManagement)) {
            return Result.error("设备不存在");
        }
        deviceManagement.setStatus(status);
        this.updateById(deviceManagement);
        return Result.ok();
    }

    @Override
    public void updateIpById(String id, String ip) {
        managementMapper.updateIpById(id, ip);
    }
}
