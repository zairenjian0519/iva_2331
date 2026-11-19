/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.wireless;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 音频播放任务队列
 * @author： DecoderX
 * @date： 2025/4/22
 */
@Slf4j
@Component
public class AudioPlayClearSchedule {

    private static final LinkedBlockingQueue<String> PLAY_TASK_QUEUE = new LinkedBlockingQueue<>();

    public static void addTask(String taskCode) {
        PLAY_TASK_QUEUE.add(taskCode);
    }

    /**
     * 定时获取队列中的任务，并删除
     */
    @Scheduled(cron = "0/3 * * * * ?")
    public void clearTask() {
        String taskCode = PLAY_TASK_QUEUE.poll();
        if (taskCode != null) {
            try {
                String token = SosHttpApiUtil.getToken();
                if (token == null) return;
                JSONObject params = new JSONObject();
                params.put("TaskCode", taskCode);
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("Token", token);
                String responseStr = HttpClientUtil.requestPostJsonWithHeader(Constant.DELETE_BROADCAST_TASK, params, headerMap);
                log.info("执行广播清楚任务返回：{}", responseStr);
            } catch (Exception e) {
                log.error("执行广播清楚任务失败：{}", e.getLocalizedMessage());
            }
        }
    }
}