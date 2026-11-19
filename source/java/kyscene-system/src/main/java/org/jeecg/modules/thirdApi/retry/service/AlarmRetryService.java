package org.jeecg.modules.thirdApi.retry.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.thirdApi.retry.AlarmRetryData;

import java.util.List;

/**
 * 数据推送重试service
 */
public interface AlarmRetryService extends IService<AlarmRetryData> {

    void saveData(AlarmRetryData alarmRetryData);

    void retryPushData();

    void removeByCameraIds(List<String> cameraIds);
}
