/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.thirdApi.HttpClientUtil;

/**
 * @author： DecoderX
 * @date： 2025/8/6
 */
@Slf4j
public class TokenUtils {

    private static final String TOKEN_KEY = "baidu_face_token";

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    public static String getToken() {
        // 先从redis中获取
        RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
        Object o = redisUtil.get(TOKEN_KEY);
        if (ObjectUtil.isNotEmpty(o)) return (String) o;
        BaiduProperties properties = SpringContextUtils.getBean(BaiduProperties.class);
        String apiKey = properties.getApiKey();
        String secretKey = properties.getSecretKey();
        if (ObjectUtil.hasEmpty(apiKey, secretKey)) {
            log.error("请在yml中配置face的apiKey和secretKey");
            return null;
        }
        try {
            String requestUrl = String.format("%s?client_id=%s&client_secret=%s&grant_type=client_credentials",TOKEN_URL, apiKey, secretKey);
            String responseStr = HttpClientUtil.requestPostJson(requestUrl, null);
            log.info("face token 获取结果：{}", responseStr);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            if (responseObj.containsKey("error")) {
                log.error("face token 获取失败：{}", responseStr);
                return null;
            }
            String token = responseObj.getString("access_token");
            long expire = responseObj.getLong("expires_in"); // 秒
            redisUtil.set(TOKEN_KEY, token, expire - 10);
            return token;
        } catch (Exception e) {
            log.error("face token 获取失败：{}", e.getMessage(), e);
            return null;
        }
    }

}
