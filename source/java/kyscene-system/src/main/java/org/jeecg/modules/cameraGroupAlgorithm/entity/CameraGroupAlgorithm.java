package org.jeecg.modules.cameraGroupAlgorithm.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * @Description: 轮询组算法表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
@Data
@TableName("camera_group_algorithm")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="camera_group_algorithm对象", description="轮询组算法表")
public class CameraGroupAlgorithm {
    
	/**id*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
	private String id;
	/**groupId*/
	@Excel(name = "groupId", width = 15)
    @ApiModelProperty(value = "groupId")
	private String groupId;
	/**algorithmId*/
	@Excel(name = "algorithmId", width = 15)
    @ApiModelProperty(value = "algorithmId")
	private String algorithmId;
	/**createTime*/
	@Excel(name = "createTime", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "createTime")
	private Date createTime;
	/**算法名称*/
	@Excel(name = "算法名称", width = 15)
    @ApiModelProperty(value = "算法名称")
	private String modelName;
	/**所属相机*/
	@Excel(name = "所属相机", width = 15)
    @ApiModelProperty(value = "所属相机")
	private String belongTo;
	/**检测区域*/
	@Excel(name = "检测区域", width = 15)
    @ApiModelProperty(value = "检测区域")
	private String checkArea;
	/**检测时间*/
	@Excel(name = "检测时间", width = 15)
    @ApiModelProperty(value = "检测时间")
	private String checkTime;
	/**检测周期*/
	@Excel(name = "检测周期", width = 15)
    @ApiModelProperty(value = "检测周期")
	private Integer checkCycle;
	/**阈值*/
	@Excel(name = "阈值", width = 15)
    @ApiModelProperty(value = "阈值")
	private Double threshold;
	/**命中次数*/
	@Excel(name = "命中次数", width = 15)
    @ApiModelProperty(value = "命中次数")
	private Integer hitCount;
	/**告警推送间隔*/
	@Excel(name = "告警推送间隔", width = 15)
    @ApiModelProperty(value = "告警推送间隔")
	private Integer alarmCycle;
	/**告警等级*/
	@Excel(name = "告警等级", width = 15)
    @ApiModelProperty(value = "告警等级")
	private String alarmLevel;
	/**告警类型*/
	@Excel(name = "告警类型", width = 15)
    @ApiModelProperty(value = "告警类型")
	private String alarmType;
	/**运行状态*/
	@Excel(name = "运行状态", width = 15)
    @ApiModelProperty(value = "运行状态")
	private String runStatus;
	/**时间持续间隔*/
	@Excel(name = "时间持续间隔", width = 15)
    @ApiModelProperty(value = "时间持续间隔")
	private Integer timeSpace;
	/**任务备注*/
	@Excel(name = "任务备注", width = 15)
    @ApiModelProperty(value = "任务备注")
	private String taskRemark;
	/**生效日期*/
	@Excel(name = "生效日期", width = 15)
    @ApiModelProperty(value = "生效日期")
	private String checkDate;
	/**误报灵敏度*/
	@Excel(name = "误报灵敏度", width = 15)
    @ApiModelProperty(value = "误报灵敏度")
	private Integer sensitivity;
	/**是否为轮询：1是 0否*/
	@Excel(name = "是否为轮询：1是 0否", width = 15)
    @ApiModelProperty(value = "是否为轮询：1是 0否")
	private Integer isRotate;
	/**目标数量阈值*/
	@Excel(name = "目标数量阈值", width = 15)
	@ApiModelProperty(value = "目标数量阈值")
	private java.lang.Integer targetThreshold;
	/**跟踪框交并比*/
	@Excel(name = "跟踪框交并比", width = 15)
	@ApiModelProperty(value = "跟踪框交并比")
	private java.lang.Integer iouThreshold;
	/**名称*/
	@TableField(exist = false)
	@Excel(name = "名称", width = 15)
	@ApiModelProperty(value = "名称")
	private java.lang.String name;
	/**无效区域*/
	@Excel(name = "无效区域", width = 15)
	@ApiModelProperty(value = "无效区域")
	private java.lang.String invalidArea;
}
