/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.mqtt.IotMqttConfigProperties;
import org.jeecg.modules.iot.serial.mapper.THRealDataMapper;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@Slf4j
@Service
public class THRealDataServiceImpl extends ServiceImpl<THRealDataMapper, THRealtimeData> implements THRealDataService {

    @Autowired
    private Serial485ReaderService serial485ReaderService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IotMqttConfigProperties iotMqttConfigProperties;

    @Autowired
    private Serial485Service serial485Service;

    @Override
    public IPage<THRealtimeData> listPage(String id, Page<THRealtimeData> objectPage) {
        Serial485Reader reader = serial485ReaderService.getById(id);
        if (reader == null) {
            return new Page<>();
        }
        return this.page(objectPage, Wrappers.lambdaQuery(THRealtimeData.class).eq(THRealtimeData::getSlaveAddress, reader.getSlaveAddress()));
    }

    @Override
    public void saveData(THRealtimeData thRealtimeData) {
        this.save(thRealtimeData);
    }

    @Override
    public THRealtimeData getLastData(String id) {
        Serial485Reader reader = serial485ReaderService.getById(id);

        // 直接从redis中获取
        Object o = redisUtil.get("th_sensor_" + reader.getSlaveAddress());
        if (o == null) {
            return null;
        }
        return JSON.parseObject((String) o, THRealtimeData.class);
    }

    @Override
    public void handleData(Integer slaveAddress, Double temperature, Double humidity) {
        // 插入数据库
        THRealtimeData thRealTimeData = new THRealtimeData();
        thRealTimeData.setSlaveAddress(slaveAddress);
        thRealTimeData.setTemperature(temperature);
        thRealTimeData.setHumidity(humidity);
        thRealTimeData.setCreateTime(new Date());
        this.save(thRealTimeData);

        // 缓存一份数据到redis，用于后面心跳判断设备是否在线
        redisUtil.set("th_sensor_" + slaveAddress, JSON.toJSONString(thRealTimeData));

        // 通过mqtt推送数据
        try {
            MqttClientConnect mqttClient = MqttClientService.getInstance().getMqttClient();
            if (mqttClient == null) {
                log.error("未配置温湿度推送的mqtt信息");
                return;
            }

            Serial485Reader serial485Reader = serial485ReaderService.getBySlaveAddress(slaveAddress);
            if (serial485Reader == null) {
                log.error("未找到从机地址 {} 对应的设备信息", slaveAddress);
                return;
            }

            THMqttMsgData thMqttMsgData = new THMqttMsgData();
            thMqttMsgData.setId(serial485Reader.getId());
            thMqttMsgData.setLocation(AlarmHandleUtil.getGroupNames(serial485Reader.getLocation()));
            thMqttMsgData.setDeviceName(serial485Reader.getDeviceName());
            thMqttMsgData.setDeviceCode(serial485Reader.getDeviceCode());
            thMqttMsgData.setSlaveAddress(slaveAddress);
            thMqttMsgData.setTemperature(temperature);
            thMqttMsgData.setHumidity(humidity);
            thMqttMsgData.setCreateTime(new Date());

            // 获取业务IP
            JSONObject businessIpInf = SysParameterUtil.getBusinessIpInf();
            if (ObjectUtil.isNotEmpty(businessIpInf)) {
                thMqttMsgData.setIpAddress(businessIpInf.getString("bizIp"));
            } else {
                thMqttMsgData.setIpAddress("");
            }

            IotMsgEntity<THMqttMsgData> iotMsgEntity = new IotMsgEntity<>();
            iotMsgEntity.setType(DeviceEnum.TH_SENSOR_REALTIME_DATA.getCode());
            iotMsgEntity.setData(thMqttMsgData);
            iotMsgEntity.setDeviceSn(SysParameterUtil.getDeviceSn());
            mqttClient.pub(iotMqttConfigProperties.getPubTopic(), JSON.toJSONString(iotMsgEntity));
            log.info("推送温湿度数据成功");
        } catch (MqttException e) {
            log.error("推送温湿度数据失败：{}", e.getMessage(), e);
        }
    }

    @Override
    public void collectAllDeviceData() {
        // List<Serial485Reader> serial485Readers = serial485ReaderService.list();
        Object o = redisUtil.get(DeviceEnum.TH_SENSOR_DEVICE_LIST.getCode());
        if (o == null) return;
        List<Serial485Reader> serial485Readers = JSON.parseArray(o.toString(), Serial485Reader.class);
        log.info("开始轮询采集，共 {} 个设备", serial485Readers.size());

        for (Serial485Reader serial485Reader : serial485Readers) {
            try {
                serial485Service.sendCommand(serial485Reader.getPollingCommand());

                // 每个设备间隔 1 秒发送
                Thread.sleep(1000);
            } catch (IOException e) {
                log.error("设备 [{}] 发送采集指令失败", serial485Reader.getDeviceName(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("采集任务被中断");
                break;
            }
        }

        log.info("本轮采集任务完成");
    }

    @Override
    public void checkDeviceStatus() {
        List<Serial485Reader> serial485Readers = serial485ReaderService.list();
        long currentTime = System.currentTimeMillis();
        long offlineThreshold = 2 * 60 * 60 * 1000; // 2小时
        // 可配置的离线阈值，默认2小时
        SysParameter sysParameter = SysParameterUtil.getSysParameter("TH_SENSOR_OFFLINE_THRESHOLD");
        if (ObjectUtil.isNotNull(sysParameter) && StrUtil.isNotBlank(sysParameter.getParamVal())) {
            offlineThreshold = Long.parseLong(sysParameter.getParamVal()) * 60 * 1000;
        }

        for (Serial485Reader serial485Reader : serial485Readers) {
            Object o = redisUtil.get("th_sensor_" + serial485Reader.getSlaveAddress());
            Integer currentStatus = serial485Reader.getStatus();
            Integer newStatus = 0; // 默认离线

            // 设备离线情况1: Redis中无数据
            if (o == null) {
                log.info("温湿度设备 {} 离线 - 无缓存数据", serial485Reader.getDeviceName());
            } else {
                try {
                    THRealtimeData thRealTimeData = JSON.parseObject((String) o, THRealtimeData.class);

                    // 设备离线情况2: 数据过期(超过阈值时间)
                    if (thRealTimeData.getCreateTime().getTime() + offlineThreshold >= currentTime) {
                        // 设备在线
                        log.info("温湿度设备 {} 在线", serial485Reader.getDeviceName());
                        newStatus = 1;
                    } else {
                        log.info("温湿度设备 {} 离线 - 数据过期", serial485Reader.getDeviceName());
                    }
                } catch (Exception e) {
                    log.error("检查设备 {} 状态时发生错误", serial485Reader.getDeviceName(), e);
                }
            }
            // 只有状态发生变化时才更新数据库
            if (!newStatus.equals(currentStatus)) {
                serial485Reader.setStatus(newStatus);
                try {
                    serial485ReaderService.updateById(serial485Reader);
                    log.debug("设备 {} 状态已更新为: {}", serial485Reader.getDeviceName(), newStatus);
                } catch (Exception e) {
                    log.error("更新设备 {} 状态失败", serial485Reader.getDeviceName(), e);
                }
            }
        }
    }

    // 清理任务 30 分钟执行一次
    @Scheduled(cron = "0 0/30 * * * ?")
    public void clearTHHistory() {
        log.info("开始清理温湿度历史数据...");

        // 按时间倒序获取数据，只保留最近5000条数据
        List<THRealtimeData> latest5000 = this.list(
                new LambdaQueryWrapper<THRealtimeData>()
                        .orderByDesc(THRealtimeData::getCreateTime)
                        .last("LIMIT 5000")
        );

        if (latest5000.isEmpty()) {
            log.info("暂无温湿度数据，无需清理");
            return;
        }

        Date createTime = latest5000.get(latest5000.size() - 1).getCreateTime();
        // this.remove(new LambdaQueryWrapper<THRealtimeData>().lt(THRealtimeData::getCreateTime, createTime));
        int deleted = this.baseMapper.delete(new LambdaQueryWrapper<THRealtimeData>().lt(THRealtimeData::getCreateTime, createTime));
        log.info("已清理 {} 条历史数据", deleted);
    }
}
