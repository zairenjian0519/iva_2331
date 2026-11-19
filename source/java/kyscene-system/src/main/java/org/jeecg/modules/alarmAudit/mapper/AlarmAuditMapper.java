package org.jeecg.modules.alarmAudit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.alarmAudit.entity.AlarmAudit;

/**
 * @Description: 告警审核
 * @Author: jeecg-boot
 * @Date:   2022-08-30
 * @Version: V1.0
 */
public interface AlarmAuditMapper extends BaseMapper<AlarmAudit> {

    /**
     * 清空表
     */
    void truncateTable();

}
