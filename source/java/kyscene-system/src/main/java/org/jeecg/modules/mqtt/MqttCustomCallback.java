/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.face.remoteSync.GlobalSyncStateCache;
import org.jeecg.modules.face.remoteSync.RemoteSyncFaceData;

/**
 * @author： DecoderX
 * @date： 2025/5/21
 */
@Slf4j
public class MqttCustomCallback implements MqttCallback, MqttCallbackExtended {

    private String mqttClientId;

    public MqttCustomCallback(String mqttClientId) {
        this.mqttClientId = mqttClientId;
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            log.info("MQTT 重连: {}", serverURI);
            MqttClientService.getInstance().reconnectCustomClient(serverURI);
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        log.warn("MQTT断开连接：{}", throwable.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        try {
            String msg = StrUtil.str(mqttMessage.getPayload(), "utf-8");
            log.info("mqtt 收到消息：{}, {}", topic, msg);
            JSONObject jsonObject = JSON.parseObject(msg);
            String type = jsonObject.getString("type");
            RemoteSyncFaceData remoteSyncFaceData = SpringContextUtils.getBean(RemoteSyncFaceData.class);
            GlobalSyncStateCache globalSyncStateCache = SpringContextUtils.getBean(GlobalSyncStateCache.class);
            if (globalSyncStateCache.getSyncState().isGlobalState()) {
                log.info("正在同步数据，忽略本次消息");
                return;
            }
            if ("ALL".equals(type)) {
                remoteSyncFaceData.syncAllFace();
            } else if ("DIFF".equals(type)) {
                remoteSyncFaceData.syncDiffFace();
            }
        } catch (Exception e) {
            log.error("mqtt 收到消息异常：{}", e.getMessage());
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
