package org.jeecg.modules.dataPushHistory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.dataPushHistory.entity.DataPushHistory;
import org.jeecg.modules.dataPushHistory.entity.PushHistoryDto;

/**
 * @author： DecoderX
 * @date： 2025/6/5
 */
public interface IPushHistoryService extends IService<DataPushHistory> {

    void clearLog(int clearNum, int offsetNum);

    IPage<DataPushHistory> getPageData(PushHistoryDto pushHistoryDto);
}
