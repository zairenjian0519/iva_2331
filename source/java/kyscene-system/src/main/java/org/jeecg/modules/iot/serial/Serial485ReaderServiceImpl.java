/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.mqtt.IotMqttCustomOptions;
import org.jeecg.modules.iot.serial.mapper.Serial485ReaderMapper;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * @author： DecoderX
 * @date： 2025/10/21
 */
@Slf4j
@Service
public class Serial485ReaderServiceImpl extends ServiceImpl<Serial485ReaderMapper, Serial485Reader> implements Serial485ReaderService {

    @Autowired
    private Serial485Service serial485Service;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IotMqttCustomOptions iotMqttCustomOptions;

    @Override
    public IPage<Serial485Reader> queryPageList(IPage<Serial485Reader> page) {
        return this.page(page);
    }

    @Override
    public void addData(Serial485Reader serial485Reader) {
        checkParams(serial485Reader);

        // 名称不能重复
        long count1 = this.count(Wrappers.lambdaQuery(Serial485Reader.class).eq(Serial485Reader::getDeviceName, serial485Reader.getDeviceName()));
        if (count1 > 0) {
            throw new JeecgBootException("设备名称已存在");
        }

        // 编号不能重复
        long count2 = this.count(Wrappers.lambdaQuery(Serial485Reader.class).eq(Serial485Reader::getDeviceCode, serial485Reader.getDeviceCode()));
        if (count2 > 0) {
            throw new JeecgBootException("设备编号已存在");
        }
        serial485Reader.setStatus(0);
        this.save(serial485Reader);
        cacheDeviceList();
        syncDeviceList();
    }

    @Override
    public void updateData(Serial485Reader serial485Reader) {
        checkParams(serial485Reader);

        // 名称不能重复
        long count1 = this.count(Wrappers.lambdaQuery(Serial485Reader.class)
                .eq(Serial485Reader::getDeviceName, serial485Reader.getDeviceName())
                .ne(Serial485Reader::getId, serial485Reader.getId()));
        if (count1 > 0) {
            throw new JeecgBootException("设备名称已存在");
        }

        // 编号不能重复
        long count2 = this.count(Wrappers.lambdaQuery(Serial485Reader.class)
                .eq(Serial485Reader::getDeviceCode, serial485Reader.getDeviceCode())
                .ne(Serial485Reader::getId, serial485Reader.getId()));
        if (count2 > 0) {
            throw new JeecgBootException("设备编号已存在");
        }

        this.updateById(serial485Reader);
        cacheDeviceList();
        syncDeviceList();
    }

    @Override
    public void deleteData(String id) {
        this.removeById(id);
        cacheDeviceList();
        syncDeviceList();
    }

    @Override
    public Serial485Reader getBySlaveAddress(Integer slaveAddress) {
        return this.getOne(Wrappers.lambdaQuery(Serial485Reader.class).eq(Serial485Reader::getSlaveAddress, slaveAddress), false);
    }

    @Override
    public Serial485Setting getCommInfo() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter("TH_SENSOR");
        if (sysParameter != null && sysParameter.getParamVal() != null) {
            return JSON.toJavaObject(JSON.parseObject(sysParameter.getParamVal()), Serial485Setting.class);
        }
        return null;
    }

    @Override
    public void saveToSystemParam(Serial485Setting serial485Setting) {
        // 检查当前串口是否已打开
        SysParameter parameter = SysParameterUtil.getSysParameter("TH_SENSOR");
        if (parameter != null && parameter.getParamVal() != null) {
            Serial485Setting setting = JSON.toJavaObject(JSON.parseObject(parameter.getParamVal()), Serial485Setting.class);
            if (setting != null && setting.getStatus() == 1) {
                throw new JeecgBootException("请先断开当前已打开的串口：" + setting.getCommPortName());
            }
        }

        SysParameter sysParameter = new SysParameter();
        sysParameter.setParamName("温湿度传感器");
        sysParameter.setParamCode("TH_SENSOR");
        sysParameter.setDescription("温度、湿度传感器配置");

        serial485Setting.setStatus(0);
        try {
            serial485Service.openCommPort(serial485Setting);
            serial485Setting.setStatus(1);
            sysParameter.setParamVal(JSON.toJSONString(serial485Setting));
            SysParameterUtil.addOrUpdateSysParameter(sysParameter);
        } catch (UnsupportedCommOperationException e) {
            throw new JeecgBootException("串口参数设置不支持");
        } catch (TooManyListenersException e) {
            throw new JeecgBootException("添加串口监听器失败");
        } catch (NoSuchPortException e) {
            throw new JeecgBootException("串口 " + serial485Setting.getCommPortName() + " 不存在");
        } catch (PortInUseException e) {
            throw new JeecgBootException("串口 " + serial485Setting.getCommPortName() + " 被占用");
        }
    }

    @Override
    public void disconnectCommPort() {
        serial485Service.closeCommPort();
        SysParameter sysParameter = SysParameterUtil.getSysParameter("TH_SENSOR");
        if (sysParameter != null && sysParameter.getParamVal() != null) {
            Serial485Setting setting = JSON.toJavaObject(JSON.parseObject(sysParameter.getParamVal()), Serial485Setting.class);
            if (setting != null && setting.getStatus() == 1) {
                setting.setStatus(0);
                sysParameter.setParamVal(JSON.toJSONString(setting));
                SysParameterUtil.addOrUpdateSysParameter(sysParameter);
            }
        }
    }

    @Override
    public void sendCommand(String id) {
        SerialPort serialPort = serial485Service.getSerialPort();
        if (serialPort == null) {
            throw new JeecgBootException("请先连接串口");
        }
        Serial485Reader reader = this.getById(id);
        if (reader == null) {
            throw new JeecgBootException("传感器不存在");
        }
        String command = reader.getPollingCommand();
        if (StrUtil.isBlank(command)) {
            throw new JeecgBootException("请填写轮询指令");
        }

        // 发送指令
        try {
            serial485Service.sendCommand(command);
        } catch (IOException e) {
            throw new JeecgBootException("发送指令失败");
        }
    }

    private void checkParams(Serial485Reader serial485Reader) {
        if (StrUtil.isBlank(serial485Reader.getDeviceName())) {
            throw new JeecgBootException("请填写设备名称");
        }

        // 设备编号不能为空; 编号只支持数字和字母和下划线的组合；长度不超过50
        if (StrUtil.isBlank(serial485Reader.getDeviceCode())) {
            throw new JeecgBootException("请填写设备编号");
        }

        String deviceCode = serial485Reader.getDeviceCode();
        if (!deviceCode.matches("^[a-zA-Z0-9_]{1,50}$")) {
            throw new JeecgBootException("设备编号只支持数字、字母和下划线的组合，且长度不超过50个字符");
        }
    }

    @PreDestroy
    public void destroy() {
        disconnectCommPort();
    }

    // 缓存设备列表
    private void cacheDeviceList() {
        List<Serial485Reader> serial485Readers = this.list();
        if (serial485Readers == null || serial485Readers.isEmpty()) return;
        redisUtil.set(DeviceEnum.TH_SENSOR_DEVICE_LIST.getCode(), JSON.toJSONString(serial485Readers));
    }

    // 同步设备列表
    public void syncDeviceList() {
        List<Serial485Reader> serial485Readers = this.list();
        if (serial485Readers == null || serial485Readers.isEmpty()) return;

        JSONObject businessIpInf = SysParameterUtil.getBusinessIpInf();
        String ip = "";
        if (ObjectUtil.isNotEmpty(businessIpInf)) {
            ip = businessIpInf.getString("bizIp");
        }
        for (Serial485Reader serial485Reader : serial485Readers) {
            serial485Reader.setLocation(AlarmHandleUtil.getGroupNames(serial485Reader.getLocation()));
            serial485Reader.setIpAddress(ip);
        }
        
        IotMsgEntity<List<Serial485Reader>> iotMsgEntity = new IotMsgEntity<>();
        iotMsgEntity.setType(DeviceEnum.TH_SENSOR_DEVICE_LIST.getCode());
        iotMsgEntity.setData(serial485Readers);
        iotMsgEntity.setDeviceSn(SysParameterUtil.getDeviceSn());
        try {
            MqttClientService.getInstance().getMqttClient().pub(iotMqttCustomOptions.getPubTopic(), JSON.toJSONString(iotMsgEntity));
        } catch (MqttException e) {
            log.error("同步温湿度设备列表失败：{}", e.getMessage(), e);
        }
    }
}
