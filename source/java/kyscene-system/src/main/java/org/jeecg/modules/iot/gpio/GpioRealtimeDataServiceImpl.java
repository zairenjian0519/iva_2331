/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.gpio;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.elock.ElectricLockRealMqttMsg;
import org.jeecg.modules.iot.gpio.mapper.GpioRealtimeDataMapper;
import org.jeecg.modules.iot.mqtt.IotMqttConfigProperties;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.pin.utils.GpioControl;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/10/20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GpioRealtimeDataServiceImpl extends ServiceImpl<GpioRealtimeDataMapper, GpioRealtimeDataEntity> implements GpioRealtimeDataService {

    private final GpioReaderService readerService;

    private final IotMqttConfigProperties iotMqttConfigProperties;

    private final RedisUtil redisUtil;

    @Override
    public IPage<GpioRealtimeDataEntity> getPageData(Page<GpioRealtimeDataEntity> page, GpioRealtimeDataEntity gpioRealtimeDataEntity) {
        return this.page(page, Wrappers.lambdaQuery(GpioRealtimeDataEntity.class).eq(StrUtil.isNotBlank(gpioRealtimeDataEntity.getPinNumber()), GpioRealtimeDataEntity::getPinNumber, gpioRealtimeDataEntity.getPinNumber()));
    }

    @Override
    public void saveData(GpioRealtimeDataEntity gpioRealtimeDataEntity) {
        this.save(gpioRealtimeDataEntity);
    }

    @Override
    public String getRealTimeData(String id) {
        GpioReaderEntity readerEntity = readerService.getById(id);
        if (readerEntity != null) {
            try {
                return GpioControl.readValue(readerEntity.getPinNumber());
            } catch (IOException e) {
                throw new JeecgBootException("状态读取异常", e);
            }
        }
        return "";
    }

    @Override
    public void readData(String pinNumber) {
        GpioReaderEntity deviceInfo = readerService.getByPinNumber(pinNumber);
        if (deviceInfo == null) {
            log.error("读取io状态失败，未找到连接 {} 的设备", pinNumber);
            return;
        }
        try {
            String val = GpioControl.readValue(pinNumber);
            log.info("设备 GPIO {} 状态 {}", pinNumber, val);

            // 从换存中获取上次的数据，如果为空，则保存
            String key = GpioConst.Gpio_Value_Cache_Prefix + pinNumber;
            String lastVal = (String) redisUtil.get(key);

            // 如果缓存中没有数据或者数据发生变化，则更新缓存并发送消息
            if (StrUtil.isBlank(lastVal) || !lastVal.equals(val)) {
                redisUtil.set(key, val);

                // 保存到数据库
                GpioRealtimeDataEntity entity = new GpioRealtimeDataEntity();
                entity.setPinNumber(pinNumber);
                entity.setRealtimeData(val);
                entity.setCreateTime(new Date());
                this.saveData(entity);

                // 发送 MQTT 消息
                ElectricLockRealMqttMsg electricMqttMsg = new ElectricLockRealMqttMsg();
                electricMqttMsg.setId(deviceInfo.getId());
                electricMqttMsg.setLocation(AlarmHandleUtil.getGroupNames(deviceInfo.getLocation()));
                electricMqttMsg.setDeviceName(deviceInfo.getDeviceName());
                electricMqttMsg.setDeviceCode(deviceInfo.getDeviceCode());
                electricMqttMsg.setRemark(deviceInfo.getRemark());
                electricMqttMsg.setPinNumber(pinNumber);
                electricMqttMsg.setRealtimeData(val);
                electricMqttMsg.setCreateTime(entity.getCreateTime());
                JSONObject businessIpInf = SysParameterUtil.getBusinessIpInf();
                if (ObjectUtil.isNotEmpty(businessIpInf)) {
                    electricMqttMsg.setIpAddress(businessIpInf.getString("bizIp"));
                }
                MsgUtil.pushElectricLockMessage(iotMqttConfigProperties.getPubTopic(), DeviceEnum.ELECTRIC_LOCK_REALTIME_DATA.getCode(), electricMqttMsg);
            }
        } catch (IOException e) {
            log.error("读取 GPIO {} 失败", pinNumber);
        } catch (MqttException e) {
            log.error("发送 MQTT 消息失败, Topic: {}", iotMqttConfigProperties.getPubTopic());
        }
    }

    @Override
    public void readAllDeviceData() {
        try {
            Object cacheObj = redisUtil.get(GpioConst.Gpio_Device_Cache_Key);
            if (cacheObj == null) {
                log.debug("GPIO设备缓存为空，跳过批量读取");
                return;
            }

            List<GpioReaderEntity> devices = JSON.parseArray(cacheObj.toString(), GpioReaderEntity.class);
            if (devices.isEmpty()) {
                log.debug("GPIO设备列表为空，跳过批量读取");
                return;
            }

            log.info("开始批量读取 {} 个GPIO设备数据", devices.size());
            for (GpioReaderEntity device : devices) {
                try {
                    if (device != null && StrUtil.isNotBlank(device.getPinNumber())) {
                        readData(device.getPinNumber());
                    }
                } catch (Exception e) {
                    log.error("读取GPIO设备 {} 数据时发生错误", device.getPinNumber(), e);
                }
            }
            log.info("完成批量读取 {} 个GPIO设备数据", devices.size());
        } catch (Exception e) {
            log.error("批量读取GPIO设备数据失败", e);
        }
    }
}
