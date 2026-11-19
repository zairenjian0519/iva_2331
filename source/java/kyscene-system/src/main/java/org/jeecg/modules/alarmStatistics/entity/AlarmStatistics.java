package org.jeecg.modules.alarmStatistics.entity;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * @Description: 统计分析表
 * @Author: jeecg-boot
 * @Date:   2022-08-12
 * @Version: V1.0
 */
@Data
@TableName("alarm_detail")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="alarm_detail对象", description="统计分析表")
public class AlarmStatistics {
    
	/**id*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
	private String id;
	/**告警时间*/
	@Excel(name = "告警时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "告警时间")
	private Date alarmTime;
	/**告警地点*/
	@Excel(name = "告警地点", width = 15)
    @ApiModelProperty(value = "告警地点")
	private String alarmArea;
	/**相机名称*/
	@Excel(name = "相机名称", width = 15)
    @ApiModelProperty(value = "相机名称")
	private String cameraName;
	/**告警类型*/
	@Excel(name = "告警类型", width = 15)
    @ApiModelProperty(value = "告警类型")
	private String alarmType;
	/**告警子类型*/
	@Excel(name = "告警子类型", width = 15)
    @ApiModelProperty(value = "告警子类型")
	private String alarmSubtype;
	/**告警等级*/
	@Excel(name = "告警等级", width = 15)
    @ApiModelProperty(value = "告警等级")
	private String alarmLevel;
	/**缩略图地址*/
	@Excel(name = "缩略图地址", width = 15)
    @ApiModelProperty(value = "缩略图地址")
	private String thumbnailPath;
	/**原图地址*/
	@Excel(name = "原图地址", width = 15)
    @ApiModelProperty(value = "原图地址")
	private String picPath;
	/**告警图位置信息*/
	@Excel(name = "告警图位置信息", width = 15)
    @ApiModelProperty(value = "告警图位置信息")
	private String detectObjs;
	/**码流*/
	@Excel(name = "码流", width = 15)
    @ApiModelProperty(value = "码流")
	private String stream;
	/**备注*/
	@Excel(name = "备注", width = 15)
    @ApiModelProperty(value = "备注")
	private String remarks;
	/**创建时间*/
	@Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
	private Date createTime;
}
