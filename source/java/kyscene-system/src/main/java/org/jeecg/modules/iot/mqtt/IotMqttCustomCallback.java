/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.mqtt;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @author： DecoderX
 * @date： 2025/10/13
 */
@Slf4j
public class IotMqttCustomCallback implements MqttCallback, MqttCallbackExtended {

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            log.info("MQTT 重连: {}", serverURI);

        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        log.warn("MQTT断开连接：{}", throwable.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String msg = StrUtil.str(mqttMessage.getPayload(), "utf-8");
        log.info("MQTT 收到消息：{}, {}", topic, msg);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
