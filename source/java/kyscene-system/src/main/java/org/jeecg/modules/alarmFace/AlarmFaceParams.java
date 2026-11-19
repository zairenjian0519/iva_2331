/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.alarmFace;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/4/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AlarmFaceParams extends AlarmFaceDo{

    private List<String> cameraNames;

    private String field; //排序字段

    private String order; //排序规则

    private String startTime;

    private String endTime;

}
