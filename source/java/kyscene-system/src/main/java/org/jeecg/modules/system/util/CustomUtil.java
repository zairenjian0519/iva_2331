/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.system.util;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ValueFilter;

/**
 * @author： DecoderX
 * @date： 2025/3/28
 */
public class CustomUtil {

    public static JSONObject convertToJSONObject(Object object) {
        // 使用 ValueFilter 自定义序列化逻辑
        ValueFilter valueFilter = (object1, name, value) -> {
            // 如果值为 null，则返回空字符串 ""
            return value == null ? "" : value;
        };

        // 使用 toJSONString 方法并指定 ValueFilter
        String jsonString = JSONObject.toJSONString(object, valueFilter, SerializerFeature.WriteMapNullValue);

        // 将 JSON 字符串解析为 JSONObject
        return JSONObject.parseObject(jsonString);
    }

    // 从rtsp地址中获取ip地址 rtsp://user:password@192.168.3.108/h264/ch1/main/av_stream0
    public static String getIpFromRtsp(String rtsp) {
        if (rtsp == null || !rtsp.startsWith("rtsp://")) {
            return null;
        }

        String temp = rtsp.substring("rtsp://".length());

        // 如果有用户名密码，则截掉前面的部分
        int atIndex = temp.indexOf("@");
        if (atIndex != -1) {
            temp = temp.substring(atIndex + 1);
        }

        // 提取IP部分（直到第一个斜杠）
        int slashIndex = temp.indexOf("/");
        if (slashIndex != -1) {
            temp = temp.substring(0, slashIndex);
        }

        // 如果有端口号，例如 192.168.3.108:554，也只取IP
        int colonIndex = temp.indexOf(":");
        if (colonIndex != -1) {
            temp = temp.substring(0, colonIndex);
        }

        return temp;
    }

    // 判断ip是否在线 0 不在线 1 在线
    public static int isOnlineFromIp(String ip) {
        if (StrUtil.isNotBlank(ip))
            return NetUtil.ping(ip)? 1 : 0;
        return 0;
    }
}
