/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.mqtt;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/10/13
 */
@Slf4j
@Component
public class IotMqttCustomOptions {

    private IotMqttConfigProperties iotMqttConfigProperties;

    @Autowired
    public void setIotMqttConfigProperties(IotMqttConfigProperties iotMqttConfigProperties) {
        this.iotMqttConfigProperties = iotMqttConfigProperties;
    }

    /**
     * mqtt服务地址
     */
    @Getter
    @Setter
    private String host;

    @Getter
    @Setter
    private int port;

    /**
     * 用户名
     */
    @Getter
    @Setter
    private String userName;

    /**
     * 密码
     */
    @Getter
    @Setter
    private String passWord;

    /**
     * 是否清除会话
     */
    @Getter
    @Setter
    private boolean cleanSession;

    /**
     * 回调
     */
    @Getter
    @Setter
    private MqttCallback mqttCallback;

    /**
     * 获取客户端id
     * 优先返回系统参数中的设备序列号，若为空则返回当前时间戳
     */
    public String getClientId() {
        String newClientId = SysParameterUtil.getDeviceSn();
        if (StrUtil.isBlank(newClientId)) {
            newClientId = "dqyt_" + System.currentTimeMillis();
        } else {
            newClientId = "dqyt_" + newClientId;
        }
        // 清理非法字符，只保留 [A-Za-z0-9_-]
        newClientId = newClientId.replaceAll("[^A-Za-z0-9_-]", "");

        // 限制长度（防止太长）
        if (newClientId.length() > 23) {
            newClientId = newClientId.substring(0, 23);
        }
        log.info("Mqtt ClientId: {}", newClientId);
        return newClientId;
    }

    public String getSubTopic() {
        return iotMqttConfigProperties.getSubTopic();
    }

    public String getPubTopic() {
        return iotMqttConfigProperties.getPubTopic();
    }

    /**
     * 初始化MqttCustomOptions
     */
    public void initMqttCustomOptions() {
        setHost(iotMqttConfigProperties.getHost());
        setPort(iotMqttConfigProperties.getPort());
        setUserName(iotMqttConfigProperties.getUserName());
        setPassWord(iotMqttConfigProperties.getPassWord());
        setCleanSession(Boolean.TRUE);
        setMqttCallback(new IotMqttCustomCallback());
    }
}
