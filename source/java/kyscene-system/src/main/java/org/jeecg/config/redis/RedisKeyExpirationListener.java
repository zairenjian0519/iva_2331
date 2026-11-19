package org.jeecg.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisKeyExpirationListener {

    public void onMessage(String message, String pattern) {
        log.info("监听到key过期：{}", message);
    }

}
