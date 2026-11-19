package org.jeecg.modules.mqtt;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.devwebsocket.DeviceWebSocket;
import org.jeecg.modules.devwebsocket.ViewWebSocket;
import org.jeecg.modules.mqtt.handler.*;

@Slf4j
public class MqttClientCallback implements MqttCallback, MqttCallbackExtended {
    private final RedisUtil redisUtil;

    private final DeviceWebSocket webSockets;

    private final ViewWebSocket viewWebSockets;

    private final String mqttClientId;
    private final CameraStatusHandler cameraStatusHandler;

    private final AlarmMsgHandler alarmMsgHandler;

    private final CameraWarningHandler cameraWarningHandler;

    private final MachineIdHandler machineIdHandler;

    private final AuthInfoHandler authInfoHandler;

    private final AlgorithmAdapterHandler algorithmAdapterHandler;

    public MqttClientCallback(String mqttClientId) {
        this.mqttClientId = mqttClientId;
        redisUtil = SpringContextUtils.getBean(RedisUtil.class);
        webSockets = SpringContextUtils.getBean(DeviceWebSocket.class);
        viewWebSockets = SpringContextUtils.getBean(ViewWebSocket.class);
        cameraStatusHandler = SpringContextUtils.getBean(CameraStatusHandler.class);
        alarmMsgHandler = SpringContextUtils.getBean(AlarmMsgHandler.class);
        cameraWarningHandler = SpringContextUtils.getBean(CameraWarningHandler.class);
        machineIdHandler = SpringContextUtils.getBean(MachineIdHandler.class);
        authInfoHandler = SpringContextUtils.getBean(AuthInfoHandler.class);
        algorithmAdapterHandler = SpringContextUtils.getBean(AlgorithmAdapterHandler.class);
    }

    /**
     * MQTT 断开连接执行
     */
    @Override
    public void connectionLost(Throwable throwable) {
        log.warn("MQTT断开连接：{}", throwable.getMessage());
    }

    /**
     * MQTT 收到消息执行
     * 默认8KB缓冲区
     */
    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        try {
            String msg = StrUtil.str(mqttMessage.getPayload(), "utf-8");
            switch (topic) {
                case "web_getdeviceinfo": //设备运行信息
                    JSONObject jsonObject = JSON.parseObject(msg);
                    jsonObject.put("devIp", mqttClientId);
                    webSockets.pushMessage(JSON.toJSONString(jsonObject));
                    redisUtil.set(mqttClientId + "_deviceInfo", JSON.parseObject(msg));
                    break;
                case "web_getcamerastatus": //相机状态
                    cameraStatusHandler.handleMessage(msg, mqttClientId);
                    break;
                case "web_conftp":
                    redisUtil.set(mqttClientId + "_deviceFtp", JSON.parseObject(msg));
                    break;
                case "web_getvideostream":
                    viewWebSockets.pushMessage(msg);
                    redisUtil.set(mqttClientId + "_videoStream", msg);
                    break;
                case "alarm_result":
                    alarmMsgHandler.handleMessage(msg);
                    break;
                case "camera_warning":
                    cameraWarningHandler.handleMessage(msg, mqttClientId);
                    break;
                case "web_aiver":
                    redisUtil.set(mqttClientId + "_deviceAIver", JSON.parseObject(msg));
                    break;
                case "web_pic":
                    redisUtil.set(mqttClientId + "_webPicZip", msg);
                    break;
                case "web_machineid":
                    machineIdHandler.handleMessage(msg, mqttClientId);
                    break;
                case "web_authinfo":
                    authInfoHandler.handleMessage(msg, mqttClientId);
                    break;
                case "algorithm_adapter":
                    algorithmAdapterHandler.handle(msg);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("处理mqtt消息报错：{}", e.getLocalizedMessage());
        }
    }

    /**
     * MQTT 发布成功后执行
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    /**
     * MQTT 重连
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            log.warn("MQTT_重连:" + serverURI);
            MqttClientService.getInstance().reconnectMqttClient(serverURI);
        }
    }
}
