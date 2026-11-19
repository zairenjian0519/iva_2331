/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.mqtt.IotMqttConfigProperties;
import org.jeecg.modules.iot.thermal.entity.ThermalDeviceInfo;
import org.jeecg.modules.iot.thermal.entity.ThermalMqttMsg;
import org.jeecg.modules.iot.thermal.entity.ThermalRealtimeData;
import org.jeecg.modules.iot.thermal.mapper.ThermalDataMapper;
import org.jeecg.modules.iot.thermal.module.ItemQueryParams;
import org.jeecg.modules.iot.thermal.module.TemperQueryResult;
import org.jeecg.modules.iot.thermal.module.ThermalCamera;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/11/19
 */
@Slf4j
@Service
public class ThermalRealDataServiceImpl extends ServiceImpl<ThermalDataMapper, ThermalRealtimeData> implements ThermalRealDataService {

    @Autowired
    private ThermalService thermalService;

    @Autowired
    private IotMqttConfigProperties iotMqttConfigProperties;

    @Override
    public IPage<ThermalRealtimeData> listPage(IPage<ThermalRealtimeData> page) {
        return this.page(page);
    }

    @Override
    public void queryRealTimeData(String deviceId) {
        ThermalDeviceInfo deviceInfo = thermalService.getById(deviceId);
        if (deviceInfo == null)
            throw new JeecgBootException("设备不存在");

        TemperQueryResult temperQueryResult = executeQuery(deviceInfo);
        if (temperQueryResult == null) return;

        // 保存到数据库
        ThermalRealtimeData data = new ThermalRealtimeData();
        data.setTemperatureMax(temperQueryResult.getTemperMax());
        data.setTemperatureMin(temperQueryResult.getTemperMin());
        data.setTemperatureAver(temperQueryResult.getTemperAver());
        data.setTemperatureMid(temperQueryResult.getTemperMid());
        data.setTemperatureStd(temperQueryResult.getTemperStd());
        data.setDeviceId(deviceInfo.getId());
        this.save(data);

    }

    private TemperQueryResult executeQuery(ThermalDeviceInfo deviceInfo) {
        ItemQueryParams params = new ItemQueryParams();
        params.setChannel(deviceInfo.getThermalChannel());
        params.setPresetId(deviceInfo.getPresetId());
        params.setRuleId(deviceInfo.getRuleId());
        params.setMeterType(deviceInfo.getMeterType());
        params.setIpAddress(deviceInfo.getIpAddress());
        return ThermalCamera.queryTemperature(params);
    }

    @Override
    public ThermalRealtimeData getLastRecord(String deviceId) {
        return this.lambdaQuery()
                .eq(ThermalRealtimeData::getDeviceId, deviceId)
                .orderByDesc(ThermalRealtimeData::getCreateTime)
                .last("limit 1")
                .one();
    }

    @Override
    public void collectAllDeviceData() {
        List<ThermalDeviceInfo> deviceInfos = thermalService.list();
        log.info("开始轮询采集，共 {} 个设备", deviceInfos.size());
        for (ThermalDeviceInfo deviceInfo : deviceInfos) {
            log.info("开始轮询采集设备：{}, IP地址：{}", deviceInfo.getDeviceName(), deviceInfo.getIpAddress());
            TemperQueryResult temperQueryResult = executeQuery(deviceInfo);
            if (temperQueryResult == null) continue;

            ThermalRealtimeData data = new ThermalRealtimeData();
            data.setTemperatureMax(temperQueryResult.getTemperMax());
            data.setTemperatureMin(temperQueryResult.getTemperMin());
            data.setTemperatureAver(temperQueryResult.getTemperAver());
            data.setTemperatureMid(temperQueryResult.getTemperMid());
            data.setTemperatureStd(temperQueryResult.getTemperStd());
            data.setDeviceId(deviceInfo.getId());
            this.save(data);

            //mqtt推送
            ThreadUtil.execAsync(() -> {

                // 组装mqtt msg data 部分
                ThermalMqttMsg mqttMsg = new ThermalMqttMsg();
                mqttMsg.setId(deviceInfo.getId());
                mqttMsg.setDeviceName(deviceInfo.getDeviceName());
                mqttMsg.setDeviceCode(deviceInfo.getDeviceCode());
                mqttMsg.setIpAddress(deviceInfo.getIpAddress());
                mqttMsg.setLocation(AlarmHandleUtil.getGroupNames(deviceInfo.getLocation()));
                mqttMsg.setTemperatureMax(temperQueryResult.getTemperMax());
                mqttMsg.setTemperatureMin(temperQueryResult.getTemperMin());
                mqttMsg.setTemperatureAver(temperQueryResult.getTemperAver());
                mqttMsg.setCreateTime(data.getCreateTime());

                // 组装mqtt msg
                IotMsgEntity<ThermalMqttMsg> iotMsgEntity = new IotMsgEntity<>();
                iotMsgEntity.setType(DeviceEnum.TH_SENSOR_REALTIME_DATA.getCode());
                iotMsgEntity.setData(mqttMsg);
                iotMsgEntity.setDeviceSn(SysParameterUtil.getDeviceSn());
                try {
                    MqttClientConnect mqttClient = MqttClientService.getInstance().getMqttClient();
                    if (mqttClient == null) {
                        log.error("未配置推送的mqtt信息");
                        return;
                    }
                    mqttClient.pub(iotMqttConfigProperties.getPubTopic(), JSON.toJSONString(iotMsgEntity));
                    log.info("设备-[{}]，IP-[{}] 推送温度数据成功", deviceInfo.getDeviceName(), deviceInfo.getIpAddress());
                } catch (MqttException e) {
                    log.error("设备-[{}]，IP-[{}] 推送温湿度数据失败：{}", deviceInfo.getDeviceName(), deviceInfo.getIpAddress(), e.getMessage(), e);
                }
            });
        }
    }

    // 清理任务 30 分钟执行一次
    @Scheduled(cron = "0 0/30 * * * ?")
    public void clearThermalHistoryData() {
        log.info("开始清理温度历史数据, 只保留最近5000条记录...");

        // 按时间倒序获取数据，只保留最近5000条数据
        List<ThermalRealtimeData> latest5000 = this.list(
                new LambdaQueryWrapper<ThermalRealtimeData>()
                        .orderByDesc(ThermalRealtimeData::getCreateTime)
                        .last("LIMIT 5000")
        );

        if (latest5000.isEmpty()) {
            log.info("暂无温度数据，无需清理");
            return;
        }

        Date createTime = latest5000.get(latest5000.size() - 1).getCreateTime();
        // this.remove(new LambdaQueryWrapper<THRealtimeData>().lt(THRealtimeData::getCreateTime, createTime));
        int deleted = this.baseMapper.delete(new LambdaQueryWrapper<ThermalRealtimeData>().lt(ThermalRealtimeData::getCreateTime, createTime));
        log.info("已清理 {} 条历史数据", deleted);
    }
}
