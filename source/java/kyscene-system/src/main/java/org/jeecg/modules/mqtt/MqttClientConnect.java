package org.jeecg.modules.mqtt;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Objects;

/**
 * Created by Elmer on 2022/8/25.
 * mqtt连接
 */
@Getter
public class MqttClientConnect {

    public MqttClientConnect() {
    }

    private MqttClient mqttClient;

    @Setter
    private String mqttClientId;

    /**
     * 设置mqtt客户端连接
     *
     * @param host         服务地址
     * @param clientId     客户端id
     * @param userName     用户名称
     * @param passWord     用户密码
     * @param cleanSession 是否清空会话
     * @param mqttCallback mqtt回调
     * @throws MqttException mqtt异常
     */
    public void setMqttClient(String host, String clientId, String userName, String passWord, boolean cleanSession,
                              MqttCallback mqttCallback) throws MqttException {
        MqttConnectOptions options = mqttConnectOptions(host, clientId, userName, passWord, cleanSession);

        mqttClient.setCallback(Objects.requireNonNullElseGet(mqttCallback, () -> new MqttClientCallback(mqttClientId)));
        mqttClient.connect(options);
    }

    /**
     * MQTT 连接参数
     **/
    private MqttConnectOptions mqttConnectOptions(String host, String clientId, String userName, String passWord,
                                                  boolean cleanSession) throws MqttException {
        mqttClient = new MqttClient(host, clientId, new MemoryPersistence());
        mqttClient.setTimeToWait(1000);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(userName);
        options.setPassword(passWord.toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(cleanSession);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(120);
        return options;
    }

    /**
     * 发布消息 qos:1
     **/
    public void pub(String topic, String msg) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(msg.getBytes());
        MqttTopic mqttTopic = mqttClient.getTopic(topic);
        MqttDeliveryToken token = mqttTopic.publish(mqttMessage);
        token.waitForCompletion();
    }

    /**
     * 发布消息
     **/
    public void pub(String topic, String msg, int qos) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(qos);
        mqttMessage.setPayload(msg.getBytes());
        MqttTopic mqttTopic = mqttClient.getTopic(topic);
        MqttDeliveryToken token = mqttTopic.publish(mqttMessage);
        token.waitForCompletion();
    }

    /**
     * 订阅主题 qos默认1
     **/
    public void sub(String[] topic) throws MqttException {
        mqttClient.subscribe(topic);
    }

    public void sub(String topic) throws MqttException {
        mqttClient.subscribe(topic);
    }

    /**
     * 订阅主题
     *
     * @param qos 0:至少0次 1:至少1次 2:刚好1次
     **/
    public void sub(String topic, int qos) throws MqttException {
        mqttClient.subscribe(topic, qos);
    }

    /**
     * 关闭连接
     **/
    public void close() throws MqttException {
        mqttClient.disconnect();
        mqttClient.close();
    }

}
