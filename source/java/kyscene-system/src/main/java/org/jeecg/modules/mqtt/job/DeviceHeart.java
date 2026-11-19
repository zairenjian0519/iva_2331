/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.job;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.face.entity.FaceRemoteUpdateMsg;
import org.jeecg.modules.face.service.FaceRemoteUpdateMsgService;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.MqttConfigProperties;
import org.jeecg.modules.system.util.SysParameterUtil;

/**
 * @author： DecoderX
 * @date： 2025/5/22
 */
@Slf4j
public class DeviceHeart {

    public static void publishHeartbeat(MqttClientConnect mqttClient) throws MqttException {
        FaceRemoteUpdateMsgService remoteUpdateMsgService = SpringContextUtils.getBean(FaceRemoteUpdateMsgService.class);
        FaceRemoteUpdateMsg remoteUpdateMsg = remoteUpdateMsgService.getRecentData();
        JSONObject pubMsg = new JSONObject();
        pubMsg.put("feature", "");
        pubMsg.put("projectKey", SysParameterUtil.getProjectCode());
        if (remoteUpdateMsg != null) {
            pubMsg.put("feature", remoteUpdateMsg.getFeature());
        }
        MqttConfigProperties configProperties = SpringContextUtils.getBean(MqttConfigProperties.class);
        String logMsg = String.format("Topic: %s 发送心跳消息：%s", configProperties.getPubTopic(), pubMsg.toJSONString());
        log.info(logMsg);
        mqttClient.pub(configProperties.getPubTopic(), pubMsg.toJSONString());
    }

    public static void sendHeartbeat() {
        try {
            MqttClientConnect mqttClient = MqttClientService.getInstance().getMqttClient();
            if (mqttClient.getMqttClient().isConnected()) {
                publishHeartbeat(mqttClient);
            }
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }
}
