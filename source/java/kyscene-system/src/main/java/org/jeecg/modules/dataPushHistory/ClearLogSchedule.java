/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.dataPushHistory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.dataPushHistory.service.IPushHistoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/6/5
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ClearLogSchedule {

    private final IPushHistoryService pushHistoryService;

    /**
     * 大于 1w 条数据开始清理 推送日志
     * 每两小时执行一次
     */
    @Scheduled(cron = "0 5 0/2 * * ?")
    public void clearLog() {
        log.info("开始清理推送日志");
        pushHistoryService.clearLog(10000, 1000);
        log.info("清理推送日志完成");
    }
}
