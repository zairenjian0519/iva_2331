/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.gpio;

import com.alibaba.fastjson.JSON;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.elock.ElectricLockRealMqttMsg;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.system.util.SysParameterUtil;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/10/20
 */
public class MsgUtil {

    /**
     * 发送 MQTT 消息 - 电磁锁
     */
    public static void pushElectricLockMessage(String topic, String msgType, ElectricLockRealMqttMsg data) throws MqttException {
        IotMsgEntity<ElectricLockRealMqttMsg> message = new IotMsgEntity<>();
        message.setData(data);
        message.setDeviceSn(SysParameterUtil.getDeviceSn());
        message.setType(msgType);
        MqttClientService.getInstance().getMqttClient().pub(topic, JSON.toJSONString(message));
    }

    /**
     * 发送 MQTT 锁设备信息列表
     */
    public static void publishElectricLockDevInfo(String pubTopic, String msgType, List<GpioReaderEntity> list) throws MqttException{
        IotMsgEntity<List<GpioReaderEntity>> message = new IotMsgEntity<>();
        message.setData(list);
        message.setDeviceSn(SysParameterUtil.getDeviceSn());
        message.setType(msgType);
        MqttClientService.getInstance().getMqttClient().pub(pubTopic, JSON.toJSONString(message));
    }
}
