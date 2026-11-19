/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.job;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.face.entity.FaceRemoteUpdateMsg;
import org.jeecg.modules.face.service.FaceRemoteUpdateMsgService;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.MqttConfigProperties;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author： DecoderX
 * @date： 2025/5/21
 */
@Slf4j
public class HearbeatJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("人脸同步心跳定时任务触发：{}", DateUtils.formatDateTime());
        DeviceHeart.sendHeartbeat();
    }
}
