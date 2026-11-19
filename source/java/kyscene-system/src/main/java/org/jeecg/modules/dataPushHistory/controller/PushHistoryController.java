/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.dataPushHistory.controller;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.dataPushHistory.entity.DataPushHistory;
import org.jeecg.modules.dataPushHistory.entity.PushHistoryDto;
import org.jeecg.modules.dataPushHistory.service.IPushHistoryService;
import org.springframework.web.bind.annotation.*;

/**
 * @author： DecoderX
 * @date： 2025/6/5
 */
@RequiredArgsConstructor
@RequestMapping("/dataPushHistory")
@RestController
public class PushHistoryController {

    private final IPushHistoryService pushHistoryService;

    /**
     * 分页查询
      * @return 分页数据
     */
    @PostMapping("/page")
    public Result<?> page(@RequestBody PushHistoryDto pushHistoryDto) {

        return Result.OK(pushHistoryService.getPageData(pushHistoryDto));
    }
}
