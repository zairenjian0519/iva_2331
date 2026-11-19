package org.jeecg.modules.pin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("serial_pin")
public class SerialPinDO {

    /**
     * 主键id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

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
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;
}
