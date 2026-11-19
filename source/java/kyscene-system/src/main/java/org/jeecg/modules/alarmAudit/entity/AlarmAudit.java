package org.jeecg.modules.alarmAudit.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 告警审核
 * @Author: jeecg-boot
 * @Date:   2022-08-30
 * @Version: V1.0
 */
@Data
@TableName("alarm_audit")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="alarm_audit对象", description="告警审核")
public class AlarmAudit implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;
	/**创建时间*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
	/**告警id*/
	@Excel(name = "告警id", width = 15)
    @ApiModelProperty(value = "告警id")
    private String alarmId;
	/**审核状态*/
	@Excel(name = "审核状态", width = 15)
    @ApiModelProperty(value = "审核状态")
    private String auditStatus;
	/**审核结果*/
	@Excel(name = "审核结果", width = 15, dicCode = "audit_ret")
	@Dict(dicCode = "audit_ret")
    @ApiModelProperty(value = "审核结果")
    private String auditResult;
	/**审核建议*/
	@Excel(name = "审核建议", width = 15)
    @ApiModelProperty(value = "审核建议")
    private String auditReason;
	/**审核时间*/
	@Excel(name = "审核时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "审核时间")
    private Date auditTime;
	/**审核人*/
	@Excel(name = "审核人", width = 15)
    @ApiModelProperty(value = "审核人")
    private String auditPeople;
}
