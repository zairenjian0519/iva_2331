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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.gpio.mapper.GpioReaderMapper;
import org.jeecg.modules.iot.mqtt.IotMqttConfigProperties;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/10/13
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GpioReaderServiceImpl extends ServiceImpl<GpioReaderMapper, GpioReaderEntity> implements GpioReaderService {

    private final IotMqttConfigProperties iotMqttConfigProperties;

    private final RedisUtil redisUtil;

    @Override
    public IPage<GpioReaderEntity> getPageData(IPage<GpioReaderEntity> page) {
        return this.page(page);
    }

    @Override
    public void saveData(GpioReaderEntity gpioReaderEntity) {
        checkParams(gpioReaderEntity);

        // 名称不能重复
        long count1 = this.count(Wrappers.lambdaQuery(GpioReaderEntity.class).eq(GpioReaderEntity::getDeviceName, gpioReaderEntity.getDeviceName()));
        if (count1 > 0) {
            throw new JeecgBootException("名称不能重复");
        }

        // 编号不能重复
        long count2 = this.count(Wrappers.lambdaQuery(GpioReaderEntity.class).eq(GpioReaderEntity::getDeviceCode, gpioReaderEntity.getDeviceCode()));
        if (count2 > 0) {
            throw new JeecgBootException("编号不能重复");
        }

        gpioReaderEntity.setStatus(1);
        this.save(gpioReaderEntity);
        cacheDeviceInfo();
        syncDevices();
    }

    @Override
    public void editData(GpioReaderEntity gpioReaderEntity) {
        checkParams(gpioReaderEntity);
        GpioReaderEntity old = this.getById(gpioReaderEntity.getId());
        if (old == null) {
            throw new JeecgBootException("数据不存在");
        }

        // 名称不能重复
        long count1 = this.count(Wrappers.lambdaQuery(GpioReaderEntity.class)
                .eq(GpioReaderEntity::getDeviceName, gpioReaderEntity.getDeviceName())
                .ne(GpioReaderEntity::getId, gpioReaderEntity.getId()));
        if (count1 > 0) {
            throw new JeecgBootException("名称不能重复");
        }

        // 编号不能重复
        long count2 = this.count(Wrappers.lambdaQuery(GpioReaderEntity.class)
                .eq(GpioReaderEntity::getDeviceCode, gpioReaderEntity.getDeviceCode())
                .ne(GpioReaderEntity::getId, gpioReaderEntity.getId()));
        if (count2 > 0) {
            throw new JeecgBootException("编号不能重复");
        }

        gpioReaderEntity.setStatus(1);
        this.updateById(gpioReaderEntity);
        cacheDeviceInfo();
        syncDevices();
    }

    @Override
    public void deleteData(String id) {
        this.removeById(id);
        cacheDeviceInfo();
        syncDevices();
    }

    @Override
    public GpioReaderEntity getByPinNumber(String pinNumber) {
        // 参数校验
        if (StrUtil.isBlank(pinNumber)) {
            return null;
        }

        // 从缓存中获取设备信息
        Object cacheObj = redisUtil.get(GpioConst.Gpio_Device_Cache_Key);
        if (cacheObj != null) {
            try {
                List<GpioReaderEntity> caches = JSON.parseArray(cacheObj.toString(), GpioReaderEntity.class);
                return caches.stream()
                        .filter(cache -> pinNumber.equals(cache.getPinNumber()))
                        .findFirst()
                        .orElse(null);
            } catch (Exception e) {
                log.warn("解析GPIO设备缓存失败，将重新构建缓存", e);
            }
        }

        // 缓存不存在或解析失败，重建缓存
        cacheDeviceInfo();

        // 直接从数据库查询指定pinNumber的设备
        return this.getOne(Wrappers.lambdaQuery(GpioReaderEntity.class)
                .eq(GpioReaderEntity::getPinNumber, pinNumber), false);
    }

    private void checkParams(GpioReaderEntity gpioReaderEntity) {
        if (StrUtil.isBlank(gpioReaderEntity.getPinNumber())) {
            throw new JeecgBootException("设备连接针脚不能为空");
        }
        if (StrUtil.isBlank(gpioReaderEntity.getDeviceName())) {
            throw new JeecgBootException("请填写设备名称");
        }
        if (StrUtil.isBlank(gpioReaderEntity.getDeviceCode())) {
            throw new JeecgBootException("请填写设备编号");
        }
        String deviceCode = gpioReaderEntity.getDeviceCode();
        if (!deviceCode.matches("^[a-zA-Z0-9_]{1,50}$")) {
            throw new JeecgBootException("设备编号只支持数字、字母和下划线的组合，且长度不超过50个字符");
        }
    }

    private void cacheDeviceInfo() {
        List<GpioReaderEntity> list = this.list();
        if (ObjectUtil.isNotEmpty(list))
            redisUtil.set(GpioConst.Gpio_Device_Cache_Key, JSON.toJSONString(list));
    }

    private void syncDevices() {
        List<GpioReaderEntity> list = this.list();
        JSONObject businessIpInf = SysParameterUtil.getBusinessIpInf();
        String ip = "";
        if (ObjectUtil.isNotEmpty(businessIpInf)) {
            ip = businessIpInf.getString("bizIp");
        }
        if (ObjectUtil.isNotEmpty(list)) {
            for (GpioReaderEntity gpioReader : list) {
                gpioReader.setLocation(AlarmHandleUtil.getGroupNames(gpioReader.getLocation()));
                gpioReader.setIpAddress(ip);
            }
            // 通过mqtt发送设备配置信息
            try {
                MsgUtil.publishElectricLockDevInfo(iotMqttConfigProperties.getPubTopic(), DeviceEnum.ELECTRIC_LOCK_DEVICES.getCode(), list);
            } catch (MqttException e) {
                log.error("同步电磁锁设备信息失败", e);
            }
        }
    }
}
