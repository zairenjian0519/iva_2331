/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/5/21
 */
@Slf4j
@Component
public class MqttCustomOptions {
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
            newClientId = "AiBox_" + System.currentTimeMillis();
        }
        return newClientId;
    }

    /**
     * 获取订阅主题
     */
    public String getSubTopic() {
        String projectCode = SysParameterUtil.getProjectCode();
        if (StrUtil.isBlank(projectCode)) {
            log.error("项目编号未配置");
            return null;
        }
        MqttConfigProperties configProperties = SpringContextUtils.getBean(MqttConfigProperties.class);
        String topicPrefix = configProperties.getTopic();
        if (StrUtil.isBlank(topicPrefix)) {
            log.error("主题未配置");
            return null;
        }
        return String.format("%s%s", topicPrefix, projectCode);
    }

    /**
     * 初始化MqttCustomOptions
     */
    public void initMqttCustomOptions() {
        MqttConfigProperties configProperties = SpringContextUtils.getBean(MqttConfigProperties.class);
        this.host = configProperties.getHost();
        this.port = configProperties.getPort();
        this.userName = configProperties.getUserName();
        this.passWord = configProperties.getPassWord();
        this.cleanSession = Boolean.TRUE;
        this.mqttCallback = new MqttCustomCallback(getClientId());
    }

}
