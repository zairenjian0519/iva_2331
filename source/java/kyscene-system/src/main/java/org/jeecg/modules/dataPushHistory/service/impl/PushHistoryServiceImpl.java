/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.dataPushHistory.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.dataPushHistory.entity.DataPushHistory;
import org.jeecg.modules.dataPushHistory.entity.PushHistoryDto;
import org.jeecg.modules.dataPushHistory.mapper.PushHistoryMapper;
import org.jeecg.modules.dataPushHistory.service.IPushHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/6/5
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PushHistoryServiceImpl extends ServiceImpl<PushHistoryMapper, DataPushHistory> implements IPushHistoryService {

    private final PushHistoryMapper pushHistoryMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void clearLog(int clearNum, int offsetNum) {
        boolean b = this.count() > 10000;
        if (!b) {
            log.info("推送日志数量小于 10000 不进行清理");
            return;
        }
        List<DataPushHistory> dataPushHistories = pushHistoryMapper.selectHistoryAfterNum(clearNum, offsetNum);
        this.removeByIds(dataPushHistories.stream().map(DataPushHistory::getId).toList());
    }

    @Override
    public IPage<DataPushHistory> getPageData(PushHistoryDto pushHistoryDto) {
        Integer pageNo = pushHistoryDto.getPageNo();
        Integer pageSize = pushHistoryDto.getPageSize();
        if (ObjectUtil.isEmpty(pageNo)) pushHistoryDto.setPageNo(1);
        if (ObjectUtil.isEmpty(pageSize)) pushHistoryDto.setPageSize(20);
        Page<DataPushHistory> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<DataPushHistory> queryWrapper = Wrappers.lambdaQuery(DataPushHistory.class)
                .between(ObjectUtil.isAllNotEmpty(pushHistoryDto.getStartTime(), pushHistoryDto.getEndTime()), DataPushHistory::getCreateTime, pushHistoryDto.getStartTime(), pushHistoryDto.getEndTime())
                .orderByDesc(DataPushHistory::getCreateTime);

        return this.page(page, queryWrapper);
    }
}
