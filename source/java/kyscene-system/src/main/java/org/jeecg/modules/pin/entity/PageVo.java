package org.jeecg.modules.pin.entity;

import lombok.Data;

@Data
public class PageVo {

    /**
     * 主键id
     */
    private String id;

    /**
     * pinId
     */
    private String serialPinId;

    /**
     * GPO名称
     */
    private String pinName;

    /**
     * GPO编号
     */
    private Integer pinNumber;

    /**
     * 电平类型 High Low
     */
    private String pinType;

    /**
     * 高低电平表示值
     */
    private String pinVal;

    /**
     * 算法名称
     */
    private Integer algorithmCode;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

}
