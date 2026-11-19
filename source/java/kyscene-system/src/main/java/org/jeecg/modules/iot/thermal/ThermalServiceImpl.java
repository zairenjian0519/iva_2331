/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.mqtt.IotMqttCustomOptions;
import org.jeecg.modules.iot.thermal.entity.ThermalDeviceInfo;
import org.jeecg.modules.iot.thermal.mapper.ThermalMapper;
import org.jeecg.modules.iot.thermal.module.CameraLoginParams;
import org.jeecg.modules.iot.thermal.module.ThermalCamera;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/11/18
 */
@Slf4j
@Service
public class ThermalServiceImpl extends ServiceImpl<ThermalMapper, ThermalDeviceInfo> implements ThermalService {

    @Autowired
    private IotMqttCustomOptions iotMqttCustomOptions;

    @Override
    public IPage<ThermalDeviceInfo> queryPageList(IPage<ThermalDeviceInfo> page) {
        return this.page(page);
    }

    @Override
    public void addData(ThermalDeviceInfo thermalDeviceInfo) {
        checkParams(thermalDeviceInfo);

        // 名称不能重复
        long count1 = this.count(Wrappers.lambdaQuery(ThermalDeviceInfo.class).eq(ThermalDeviceInfo::getDeviceName, thermalDeviceInfo.getDeviceName()));
        if (count1 > 0) {
            throw new JeecgBootException("设备名称已存在");
        }

        // 编号不能重复
        long count2 = this.count(Wrappers.lambdaQuery(ThermalDeviceInfo.class).eq(ThermalDeviceInfo::getDeviceCode, thermalDeviceInfo.getDeviceCode()));
        if (count2 > 0) {
            throw new JeecgBootException("设备编号已存在");
        }

        // 设备IP地址不能重复
        long count3 = this.count(Wrappers.lambdaQuery(ThermalDeviceInfo.class).eq(ThermalDeviceInfo::getIpAddress, thermalDeviceInfo.getIpAddress()));
        if (count3 > 0) {
            throw new JeecgBootException("设备IP地址已存在");
        }
        this.save(thermalDeviceInfo);
        syncDeviceList();
        // 启用线程去初始化登录信息
        ThreadUtil.execAsync(() -> initCameraSdk(thermalDeviceInfo.getId()));
    }

    @Override
    public void updateData(ThermalDeviceInfo thermalDeviceInfo) {
        checkParams(thermalDeviceInfo);

        // 名称不能重复
        long count1 = this.count(Wrappers.lambdaQuery(ThermalDeviceInfo.class)
                .eq(ThermalDeviceInfo::getDeviceName, thermalDeviceInfo.getDeviceName())
                .ne(ThermalDeviceInfo::getId, thermalDeviceInfo.getId()));
        if (count1 > 0) {
            throw new JeecgBootException("设备名称已存在");
        }

        // 编号不能重复
        long count2 = this.count(Wrappers.lambdaQuery(ThermalDeviceInfo.class)
                .eq(ThermalDeviceInfo::getDeviceCode, thermalDeviceInfo.getDeviceCode())
                .ne(ThermalDeviceInfo::getId, thermalDeviceInfo.getId()));
        if (count2 > 0) {
            throw new JeecgBootException("设备编号已存在");
        }

        // 设备IP地址不能重复
        long count3 = this.count(Wrappers.lambdaQuery(ThermalDeviceInfo.class)
                .eq(ThermalDeviceInfo::getIpAddress, thermalDeviceInfo.getIpAddress())
                .ne(ThermalDeviceInfo::getId, thermalDeviceInfo.getId()));
        if (count3 > 0) {
            throw new JeecgBootException("设备IP地址已存在");
        }

        this.updateById(thermalDeviceInfo);
        syncDeviceList();

        // 启用线程去初始化登录信息
        ThreadUtil.execAsync(() -> initCameraSdk(thermalDeviceInfo.getId()));
    }

    private void checkParams(ThermalDeviceInfo thermalDeviceInfo) {
        if (StrUtil.isBlank(thermalDeviceInfo.getDeviceName())) {
            throw new JeecgBootException("请填写设备名称");
        }

        // 设备编号不能为空; 编号只支持数字和字母和下划线的组合；长度不超过50
        if (StrUtil.isBlank(thermalDeviceInfo.getDeviceCode())) {
            throw new JeecgBootException("请填写设备编号");
        }

        String deviceCode = thermalDeviceInfo.getDeviceCode();
        if (!deviceCode.matches("^[a-zA-Z][a-zA-Z0-9_-]{1,50}$")) {
            throw new JeecgBootException("设备编号只支持数字、字母和下划线的组合，且长度不超过50个字符");
        }

        if (StrUtil.isBlank(thermalDeviceInfo.getIpAddress())) {
            throw new JeecgBootException("请填写设备IP地址");
        }

        if (ObjectUtil.isNull(thermalDeviceInfo.getPort())) {
            throw new JeecgBootException("请填写设备端口号");
        }

        if (StrUtil.isBlank(thermalDeviceInfo.getUsername())) {
            throw new JeecgBootException("请填写设备用户名");
        }

        if (StrUtil.isBlank(thermalDeviceInfo.getPassword())) {
            throw new JeecgBootException("请填写设备密码");
        }

        if (ObjectUtil.isNull(thermalDeviceInfo.getMeterType())) {
            throw new JeecgBootException("请选择测点类型");
        }

        if (ObjectUtil.isNull(thermalDeviceInfo.getPresetId())) {
            throw new JeecgBootException("请填写预置点编号");
        }

        if (ObjectUtil.isNull(thermalDeviceInfo.getRuleId())) {
            throw new JeecgBootException("请填写规则编号");
        }
    }

    @Override
    public void deleteRow(String id) {
        if (StrUtil.isBlank(id)) return;
        ThermalDeviceInfo deviceInfo = this.getById(id);
        if (deviceInfo == null) {
            throw new JeecgBootException("设备不存在");
        }
        this.removeById(id);
        syncDeviceList();
        ThermalCamera.logout(deviceInfo.getIpAddress());
    }

    public void initCameraSdk(String deviceId) {
        ThermalDeviceInfo deviceInfo = this.getById(deviceId);
        if (deviceInfo == null) {
            log.info("初始化热成像相机失败，设备不存在");
            return;
        }
        log.info("初始化热成像相机开始，设备IP：{}", deviceInfo.getIpAddress());
        initLogin(deviceInfo);
    }

    private void initLogin(ThermalDeviceInfo deviceInfo) {
        CameraLoginParams cameraLoginParams = new CameraLoginParams();
        cameraLoginParams.setIp(deviceInfo.getIpAddress());
        cameraLoginParams.setPort(deviceInfo.getPort());
        cameraLoginParams.setUsername(deviceInfo.getUsername());
        cameraLoginParams.setPassword(deviceInfo.getPassword());
        cameraLoginParams.setReLogin(false);
        boolean login = ThermalCamera.login(cameraLoginParams);
        if (login) {
            deviceInfo.setStatus(1);
            this.updateById(deviceInfo);
        } else {
            deviceInfo.setStatus(0);
            this.updateById(deviceInfo);
        }
    }

    @Async
    @Override
    public void initAllCameraLogin() {
        log.info("初始化所有热成像相机登录信息...");
        List<ThermalDeviceInfo> deviceInfos = this.list();
        if (deviceInfos == null) {
            log.info("没有需要初始化的热成像相机, 查询结果为空");
            return;
        }
        for (ThermalDeviceInfo deviceInfo : deviceInfos) {
            initLogin(deviceInfo);
        }
    }

    // 主要用户设备状态改变：掉线，重连
    @Override
    public void updateStatusByIp(String pchDVRIP, int i) {
        ThermalDeviceInfo deviceInfo = this.getOne(Wrappers.lambdaQuery(ThermalDeviceInfo.class)
                .eq(StrUtil.isNotBlank(pchDVRIP), ThermalDeviceInfo::getIpAddress, pchDVRIP), false);
        if (deviceInfo != null) {
            deviceInfo.setStatus(i);
            this.updateById(deviceInfo);
            // 在同步一次设备信息
            syncDeviceList();
        }
    }

    // 同步设备列表
    public void syncDeviceList() {
        List<ThermalDeviceInfo> thermalDeviceInfoList = this.list();
        if (thermalDeviceInfoList == null || thermalDeviceInfoList.isEmpty()) return;

        // 把设备列表中的分组名称进行转换
        for (ThermalDeviceInfo deviceInfo : thermalDeviceInfoList) {
            deviceInfo.setLocation(AlarmHandleUtil.getGroupNames(deviceInfo.getLocation()));
        }

        IotMsgEntity<List<ThermalDeviceInfo>> iotMsgEntity = new IotMsgEntity<>();
        iotMsgEntity.setType(DeviceEnum.TH_SENSOR_DEVICE_LIST.getCode());
        iotMsgEntity.setData(thermalDeviceInfoList);
        iotMsgEntity.setDeviceSn(SysParameterUtil.getDeviceSn());
        try {
            MqttClientService.getInstance().getMqttClient().pub(iotMqttCustomOptions.getPubTopic(), JSON.toJSONString(iotMsgEntity));
        } catch (MqttException e) {
            log.error("同步温湿度设备列表失败：{}", e.getMessage(), e);
        }
    }
}
