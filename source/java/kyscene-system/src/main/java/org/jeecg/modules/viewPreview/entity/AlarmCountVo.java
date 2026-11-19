package org.jeecg.modules.viewPreview.entity;

import lombok.Data;

@Data
public class AlarmCountVo {
    private String cameraId;
    private Integer allCount;
    private Integer seriousCount;
}
