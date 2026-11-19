package org.jeecg.modules.devicelog.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 设备升级日志
 * @Author: jeecg-boot
 * @Date:   2022-09-06
 * @Version: V1.0
 */
@Data
@TableName("device_log")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="device_log对象", description="设备升级日志")
public class DeviceLog implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
	/**操作人*/
	@Excel(name = "操作人", width = 15)
    @ApiModelProperty(value = "操作人")
    private java.lang.String upPeople;
	/**升级结果时间*/
	@Excel(name = "升级结果时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "升级结果时间")
    private java.util.Date resultTime;
	/**升级结果状态*/
	@Excel(name = "升级结果状态", width = 15)
    @ApiModelProperty(value = "升级结果状态")
    private java.lang.Integer resultStatus;
	/**升级结果描述*/
	@Excel(name = "升级结果描述", width = 15)
    @ApiModelProperty(value = "升级结果描述")
    private java.lang.String resultDes;
	/**下发时间*/
	@Excel(name = "下发时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "下发时间")
    private java.util.Date downTime;
	/**下发状态*/
	@Excel(name = "下发状态", width = 15)
    @ApiModelProperty(value = "下发状态")
    private java.lang.Integer downStatus;
	/**下发描述*/
	@Excel(name = "下发描述", width = 15)
    @ApiModelProperty(value = "下发描述")
    private java.lang.String downDes;
	/**备份时间*/
	@Excel(name = "备份时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "备份时间")
    private java.util.Date backupTime;
	/**备份状态*/
	@Excel(name = "备份状态", width = 15)
    @ApiModelProperty(value = "备份状态")
    private java.lang.Integer backupStatus;
	/**备份描述*/
	@Excel(name = "备份描述", width = 15)
    @ApiModelProperty(value = "备份描述")
    private java.lang.String backupDes;
	/**升级时间*/
	@Excel(name = "升级时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "升级时间")
    private java.util.Date upTime;
	/**升级状态*/
	@Excel(name = "升级状态", width = 15)
    @ApiModelProperty(value = "升级状态")
    private java.lang.Integer upStatus;
	/**升级描述*/
	@Excel(name = "升级描述", width = 15)
    @ApiModelProperty(value = "升级描述")
    private java.lang.String upDes;
	/**设备id*/
	@Excel(name = "设备id", width = 15)
    @ApiModelProperty(value = "设备id")
    private java.lang.String devid;
}
