/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.mqtt.IotMqttConfigProperties;
import org.jeecg.modules.iot.serial.mapper.THRealDataMapper;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@Slf4j
@Component
public class CommEventListener {

    private THRealDataService thRealDataService;

    @Autowired
    public void setTHRealDataService(THRealDataService thRealDataService) {
        this.thRealDataService = thRealDataService;
    }

    @Async("kylandTaskExecutor")
    @EventListener
    public void onCommEvent(CommEvent event) {
        JSONObject data = event.getData();
        String type = data.getString("type");
        log.info("CommEvent: {}", type);
        if (type.equals("HardwareErrorEvent")) {
            handleHardwareErrorEvent();
        } else if (type.equals("DataAvailableEvent")) {
            handleDataAvailableEvent(data.getJSONArray("data"));
        }
    }

    private void handleHardwareErrorEvent() {
        // 1. 数据库更新状态为离线
        updateCommPortStatus(0);

        // 2. 触发一次状态推送心跳， 传感器离线了
    }

    private void handleDataAvailableEvent(JSONArray arr) {
        // 更新串口状态: 在线
        updateCommPortStatus(1);

        // 处理数据
        int slaveAddr = arr.getInteger(0);
        double temperature = arr.getDouble(1);
        double humidity = arr.getDouble(2);
        thRealDataService.handleData(slaveAddr, temperature, humidity);
    }

    private void updateCommPortStatus(int status) {
        SysParameter sysParameter = SysParameterUtil.getSysParameter("TH_SENSOR");
        if (sysParameter != null && StrUtil.isNotBlank(sysParameter.getParamVal())) {
            Serial485Setting serial485Setting = JSON.toJavaObject(JSON.parseObject(sysParameter.getParamVal()), Serial485Setting.class);
            serial485Setting.setStatus(status);
            SysParameterUtil.addOrUpdateSysParameter(sysParameter);
        }
    }
}
