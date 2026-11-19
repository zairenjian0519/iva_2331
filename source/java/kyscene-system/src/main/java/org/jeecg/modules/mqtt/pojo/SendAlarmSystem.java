package org.jeecg.modules.mqtt.pojo;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class SendAlarmSystem {

    @Value("${Serial.state}")
    private String state;

    @Value("${Serial.voiceSegment:5}")
    private Long voiceSegment;

}
