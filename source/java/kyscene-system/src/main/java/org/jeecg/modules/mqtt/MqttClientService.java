package org.jeecg.modules.mqtt;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.utils.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.iot.mqtt.IotMqttCustomOptions;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Elmer on 2022/8/25.
 */
@Slf4j
public class MqttClientService {
    private MqttClientService() {
    }

    public static ConcurrentHashMap<String, MqttClientConnect> mqttClients = new ConcurrentHashMap<>();

    private static volatile MqttClientService mqttService = null;

    /**
     * 获取实例
     * 单例模式
     */
    public static MqttClientService getInstance() {
        //双重检查锁
        if (mqttService == null) {
            //同步锁，保证只有一个线程同时进入同步代码块
            synchronized (MqttClientService.class) {
                if (mqttService == null) {
                    mqttService = new MqttClientService();
                }
            }
        }
        return mqttService;
    }

    /**
     * 获取设备MQTT客户端
     **/
    public MqttClientConnect getMqttClient(String ip) throws MqttException {
        if (StrUtil.isEmpty(ip)) return null;
        MqttClientConnect client = mqttClients.get(ip);
        if (ObjectUtil.isEmpty(client)) {
            client = new MqttClientConnect();
            client.setMqttClientId(ip);
            String host = "tcp://" + ip + ":1883";
            client.setMqttClient(host, UUID.randomUUID().toString(), "user", "pwd", true, null);
            subTopic(client);
            mqttClients.put(ip, client);
        }
        return client;
    }

    /**
     * 获取mqtt客户端
     * 开放参数：ip，userName，passWord，cleanSession，mqttCallback
     */
    public MqttClientConnect getMqttClient() throws MqttException {
        IotMqttCustomOptions mqttCustomOptions = SpringContextUtils.getBean(IotMqttCustomOptions.class);
        String ip = mqttCustomOptions.getHost();
        if (StrUtil.isEmpty(ip)) return null;
        MqttClientConnect client = mqttClients.get(ip);
        if (ObjectUtil.isEmpty(client)) {
            client = new MqttClientConnect();
            String host = String.format("tcp://%s:%s", ip, mqttCustomOptions.getPort());
            log.info("MQTT连接：{}", host);
            client.setMqttClient(
                    host,
                    mqttCustomOptions.getClientId(),
                    mqttCustomOptions.getUserName().trim(),
                    mqttCustomOptions.getPassWord().trim(),
                    mqttCustomOptions.isCleanSession(),
                    mqttCustomOptions.getMqttCallback());
            mqttClients.put(ip, client);
        }
        return client;
    }

    /**
     * 自定义MQTT重连
     * 主要是重连后需要重新订阅
     */
    public void reconnectCustomClient(String ip) {
        try {
            if (StringUtils.isEmpty(ip)) return;
            ip = ip.split(":")[1].substring(2);
            MqttClientConnect client = mqttClients.get(ip);
            if (client != null) {
                MqttCustomOptions mqttCustomOptions = SpringContextUtils.getBean(MqttCustomOptions.class);
                client.sub(mqttCustomOptions.getSubTopic());
            }
        } catch (Exception e) {
            log.error("MQTT {} 重连异常 {}", ip, e.getMessage());
        }
    }

    /**
     * MQTT重连后需要重新订阅
     **/
    public void reconnectMqttClient(String ip) {
        try {
            if (StringUtils.isEmpty(ip)) {
                return;
            }
            ip = ip.split(":")[1].substring(2);
            MqttClientConnect client = mqttClients.get(ip);
            if (client != null) {
                subTopic(client);
            }
        } catch (Exception e) {
            log.error("MQTT {} 重连异常 {}", ip, e.getMessage());
        }
    }

    /**
     * 关闭MQTT客户端
     **/
    public void closeMqttClient(String ip) throws MqttException {
        if (StringUtils.isEmpty(ip)) {
            return;
        }
        MqttClientConnect client = mqttClients.get(ip);
        if (client != null) {
            client.close();
            log.info("关闭MQTT客户端 : " + ip);
            mqttClients.remove(ip);
        }
    }

    /**
     * MQTT订阅主题
     **/
    private void subTopic(MqttClientConnect clientConnect) throws MqttException {
        clientConnect.sub("alarm_result", 0); // 算法发送的告警结果
        clientConnect.sub("web_conftp", 0); // 获取ftp连接信息
        clientConnect.sub("web_getcamerastatus", 0); // 获取设备的所有相机状态
        clientConnect.sub("web_getdeviceinfo", 0); // 获取设备运行信息
        clientConnect.sub("web_getvideostream", 0); // 相机视频流地址
        clientConnect.sub("camera_warning", 0); // 获取相机告警，收到该消息需要通知用户检查相机配置
        clientConnect.sub("web_aiver", 0); // 获取版本list
        clientConnect.sub("web_pic", 0); //获取采集图片压缩包
        clientConnect.sub("web_machineid", 0); //获取采集图片压缩包
        clientConnect.sub("web_authinfo", 0); //获取采集图片压缩包
        clientConnect.sub("algorithm_adapter", 0); // 算法移植适配
    }


    public void getMqttClientByDeviceId(String deviceId) {

    }
}