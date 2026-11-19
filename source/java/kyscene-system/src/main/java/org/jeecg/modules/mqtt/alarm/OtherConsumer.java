/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.mqtt.alarm;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author： DecoderX
 * @date： 2025/7/29
 */
public class OtherConsumer {

    // 可缓存的线程池, 动态创建线程
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    public static void run(List<JSONObject> warnAlgList) {
        executor.submit(new OtherConsumerTask(warnAlgList));
    }
}
