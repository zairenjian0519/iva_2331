package org.jeecg.modules.alarmAudit.service;

import org.jeecg.modules.alarmAudit.entity.AlarmAudit;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @Description: 告警审核
 * @Author: jeecg-boot
 * @Date:   2022-08-30
 * @Version: V1.0
 */
public interface IAlarmAuditService extends IService<AlarmAudit> {

    /**
     * 批量删除
     * @param alarmIds
     */
    void removeByAlarmIds(List<String> alarmIds);
}
