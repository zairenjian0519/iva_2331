package org.jeecg.modules.algorithmmanager.entity;

import java.io.Serializable;
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
 * @Description: 算法实例
 * @Author: jeecg-boot
 * @Date:   2022-09-07
 * @Version: V1.0
 */
@Data
@TableName("algorithm_manager")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="algorithm_manager对象", description="算法实例")
public class AlgorithmManager implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
	/**创建人*/
    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
	/**算法名称*/
	@Excel(name = "算法名称", width = 15, dictTable = "ky_category", dicText = "name", dicCode = "id")
	@Dict(dictTable = "ky_category", dicText = "name", dicCode = "id")
    @ApiModelProperty(value = "算法名称")
    private java.lang.String modelName;
	/**任务备注*/
	@Excel(name = "任务备注", width = 15)
    @ApiModelProperty(value = "任务备注")
    private java.lang.String taskRemark;
	/**所属相机*/
	@Excel(name = "所属相机", width = 15, dictTable = "camera_manager", dicText = "camera_name", dicCode = "id")
	@Dict(dictTable = "camera_manager", dicText = "camera_name", dicCode = "id")
    @ApiModelProperty(value = "所属相机")
    private java.lang.String belongTo;
	/**检测区域*/
	@Excel(name = "检测区域", width = 15)
    @ApiModelProperty(value = "检测区域")
    private java.lang.String checkArea;
	/**生效日期*/
	@Excel(name = "生效日期", width = 15)
    @ApiModelProperty(value = "生效日期")
    private java.lang.String checkDate;
	/**检测时间*/
	@Excel(name = "检测时间", width = 15)
    @ApiModelProperty(value = "检测时间")
    private java.lang.String checkTime;
	/**检测周期*/
	@Excel(name = "检测周期", width = 15)
    @ApiModelProperty(value = "检测周期")
    private java.lang.Integer checkCycle;
	/**阈值*/
	@Excel(name = "阈值", width = 15)
    @ApiModelProperty(value = "阈值")
    private java.lang.Double threshold;
	/**命中次数*/
	@Excel(name = "命中次数", width = 15)
    @ApiModelProperty(value = "命中次数")
    private java.lang.Integer hitCount;
	/**告警推送间隔*/
	@Excel(name = "告警推送间隔", width = 15)
    @ApiModelProperty(value = "告警推送间隔")
    private java.lang.Integer alarmCycle;
	/**告警等级*/
	@Excel(name = "告警等级", width = 15, dicCode = "alarm_level")
	@Dict(dicCode = "alarm_level")
    @ApiModelProperty(value = "告警等级")
    private java.lang.String alarmLevel;
	/**告警类型*/
	@Excel(name = "告警类型", width = 15, dictTable = "ky_category", dicText = "name", dicCode = "id")
	@Dict(dictTable = "ky_category", dicText = "name", dicCode = "id")
    @ApiModelProperty(value = "告警类型")
    private java.lang.String alarmType;
	/**运行状态*/
	@Excel(name = "运行状态", width = 15)
    @ApiModelProperty(value = "运行状态")
    private java.lang.String runStatus;
	/**误报灵敏度*/
	@Excel(name = "误报灵敏度", width = 15)
    @ApiModelProperty(value = "误报灵敏度")
    private java.lang.Integer sensitivity;
	/**时间持续间隔*/
	@Excel(name = "时间持续间隔", width = 15)
    @ApiModelProperty(value = "时间持续间隔")
    private java.lang.Integer timeSpace;
	/**轮询组id*/
	@Excel(name = "轮询组id", width = 15)
	@ApiModelProperty(value = "轮询组id")
	private java.lang.String groupId;
	/**是否为轮询*/
	@Excel(name = "是否为轮询", width = 15)
	@ApiModelProperty(value = "是否为轮询")
	private java.lang.Integer isRotate;	/**是否为轮询*/
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

	/**
	 * 人脸组id，逗号分隔
	 */
	private java.lang.String faceGroupIds;

	/**
	 * 算法底图，大模型使用（围栏缺失算法）
	 */
	private String sourceImage;

	/**
	 * 帧间隔
	 */
	private Integer frameInterval;
}
