package org.jeecg.modules.thirdApi.retry.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.dataPush.Constant;
import org.jeecg.modules.thirdApi.retry.AlarmRetryData;
import org.jeecg.modules.thirdApi.retry.RetryTimerTask;
import org.jeecg.modules.thirdApi.retry.mapper.AlarmRetryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlarmRetryServiceImpl extends ServiceImpl<AlarmRetryMapper, AlarmRetryData> implements AlarmRetryService {

    private Timer timer;

    @Override
    public void saveData(AlarmRetryData alarmRetryData) {
        if (alarmRetryData == null) return;
        this.save(alarmRetryData);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void retryPushData() {
        // 默认取50条数据
        List<AlarmRetryData> alarmRetryDataList = this.baseMapper.selectAlarmRetryList();
        if (ObjectUtil.isEmpty(alarmRetryDataList)) return;
        // 先把取出来的数据删除，因为推送失败后还会保存
        List<String> collect = alarmRetryDataList.stream().map(AlarmRetryData::getId).collect(Collectors.toList());
        this.removeBatchByIds(collect);
        // 通过timer delay 500 毫秒推送一次
        if (timer == null) timer = new Timer("Retry_Alarm_Data");
        timer.purge();
        for (AlarmRetryData alarmRetryData : alarmRetryDataList) {
            RetryTimerTask timerTask = new RetryTimerTask(alarmRetryData);
            timer.schedule(timerTask, Constant.delayTime);
        }
    }

    @Override
    public void removeByCameraIds(List<String> cameraIds) {
        if (ObjectUtil.isEmpty(cameraIds)) return;
        this.remove(Wrappers.lambdaQuery(AlarmRetryData.class).in(AlarmRetryData::getCameraId, cameraIds));
    }
}
