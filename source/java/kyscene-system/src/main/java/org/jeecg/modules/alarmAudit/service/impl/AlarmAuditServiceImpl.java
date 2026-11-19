package org.jeecg.modules.alarmAudit.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.jeecg.modules.alarmAudit.entity.AlarmAudit;
import org.jeecg.modules.alarmAudit.mapper.AlarmAuditMapper;
import org.jeecg.modules.alarmAudit.service.IAlarmAuditService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
 * @Description: 告警审核
 * @Author: jeecg-boot
 * @Date:   2022-08-30
 * @Version: V1.0
 */
@Service
public class AlarmAuditServiceImpl extends ServiceImpl<AlarmAuditMapper, AlarmAudit> implements IAlarmAuditService {
    @Override
    public void removeByAlarmIds(List<String> alarmIds) {
        this.remove(Wrappers.lambdaQuery(AlarmAudit.class)
                .in(AlarmAudit::getAlarmId, alarmIds));
    }
}
