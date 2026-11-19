/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.cameraManager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/6/12
 */
@Slf4j
@Service
public class AsyncCameraManagerService {
    @Autowired
    private IAlarmDetailService alarmDetailService;


    @Async("kylandTaskExecutor") // 使用自定义线程池更佳
    public void updateAlarmDetailsAsync(String cameraId, String cameraName, String location) {
        log.info("开始异步更新, cameraId: {}, cameraName: {}, location: {}", cameraId, cameraName, location);

        int pageSize = 1000;
        int currentPage = 1;
        boolean hasMore;

        do {
            List<AlarmDetail> alarmDetailList = alarmDetailService.page(new Page<>(currentPage, pageSize),
                    new QueryWrapper<AlarmDetail>().lambda().eq(AlarmDetail::getCameraId, cameraId)).getRecords();

            if (alarmDetailList == null || alarmDetailList.isEmpty()) {
                hasMore = false;
            } else {
                for (AlarmDetail detail : alarmDetailList) {
                    detail.setCameraName(cameraName);
                    detail.setAlarmArea(location);
                }
                alarmDetailService.updateBatchById(alarmDetailList);
                currentPage++;
                hasMore = true;
            }

        } while (hasMore);
        log.info("异步更新完成");
    }
}