/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.heart;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.mqtt.IotMqttCustomOptions;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;

/**
 * @author： DecoderX
 * @date： 2025/10/28
 */
@Slf4j
public class DeviceHeartJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("开始发送设备心跳...");
        DeviceHeartService service = SpringContextUtils.getBean(DeviceHeartService.class);
        Map<String, Object> deviceHeartInfo = service.getDeviceHeartInfo();
        try {
            IotMqttCustomOptions options = SpringContextUtils.getBean(IotMqttCustomOptions.class);
            IotMsgEntity<Map<String, Object>> message = new IotMsgEntity<>();
            message.setData(deviceHeartInfo);
            message.setDeviceSn(SysParameterUtil.getDeviceSn());
            message.setType("device_heart");
            MqttClientService.getInstance().getMqttClient().pub(options.getPubTopic(), JSON.toJSONString(message));
            log.info("设备心跳发送成功...");
        } catch (MqttException e) {
            log.error("设备心跳发送失败 {}", e.getMessage(), e);
        }
    }
}
