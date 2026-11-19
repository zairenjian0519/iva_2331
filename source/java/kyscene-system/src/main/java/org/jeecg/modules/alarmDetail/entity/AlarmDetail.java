package org.jeecg.modules.alarmDetail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecg.modules.face.entity.FaceDetailDo;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @Description: 告警检测表
 * @Author: jeecg-boot
 * @Date: 2022-08-15
 * @Version: V1.0
 */
@Data
@TableName("alarm_detail")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "alarm_detail对象", description = "告警检测表")
public class AlarmDetail {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
    private String id;
    /**
     * 告警ID
     */
    @Excel(name = "告警ID", width = 15)
    @ApiModelProperty(value = "告警ID")
    private String alarmId;
    /**
     * 告警时间
     */
    @Excel(name = "告警时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "告警时间")
    private Date alarmTime;
    /**
     * 监控职责描述
     */
    @Excel(name = "监控职责描述", width = 15)
    @ApiModelProperty(value = "监控职责描述")
    private String monitorDuty;
    /**
     * 告警地点
     */
    @Excel(name = "告警地点", width = 15)
    @ApiModelProperty(value = "告警地点")
    private String alarmArea;
    /**
     * 相机ID
     */
    @Excel(name = "相机ID", width = 15)
    @ApiModelProperty(value = "相机ID")
    private String cameraId;
    /**
     * 相机名称
     */
    @Excel(name = "相机名称", width = 15)
    @ApiModelProperty(value = "相机名称")
    private String cameraName;
    /**
     * 告警类型
     */
    @Excel(name = "告警类型", width = 15)
//	@Dict(dicCode = "code",dicText = "name",dictTable = "ky_category")
    @ApiModelProperty(value = "告警类型")
    private String alarmType;
    /**
     * 告警子类型
     */
    @Excel(name = "告警子类型", width = 15)
    @ApiModelProperty(value = "告警子类型")
    private String alarmSubtype;
    /**
     * 告警等级
     */
    @Excel(name = "告警等级", width = 15, dicCode = "alarm_level")
    @Dict(dicCode = "alarm_level")
    @ApiModelProperty(value = "告警等级") //需求临时增加正常状态：”0:正常“ 1：提醒，2：一般，3：警告，4：严重
    private String alarmLevel;
    /**
     * 缩略图地址
     */
    @Excel(name = "缩略图地址", width = 15)
    @ApiModelProperty(value = "缩略图地址")
    private String thumbnailPath;
    /**
     * 原图地址
     */
    @Excel(name = "原图地址", width = 15)
    @ApiModelProperty(value = "原图地址")
    private String picPath;
    /**
     * 告警图位置信息
     */
    @Excel(name = "告警图位置信息", width = 15)
    @ApiModelProperty(value = "告警图位置信息")
    private String detectObjs;
    /**
     * 码流
     */
    @Excel(name = "码流", width = 15, dicCode = "stream_type")
    @Dict(dicCode = "stream_type")
    @ApiModelProperty(value = "码流")
    private String stream;
    /**
     * 备注
     */
    @Excel(name = "备注", width = 15)
    @ApiModelProperty(value = "备注")
    private String remarks;
    /**
     * 创建时间
     */
    @Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    //新增相机编码字段
    @Excel(name = "相机编码", width = 20)
    @ApiModelProperty(value = "相机编码")
    private String cameraNumber;

    /**
     * 目标数量
     */
    @Excel(name = "目标数量", width = 20)
    @ApiModelProperty(value = "目标数量")
    private Integer targetCount;
    /**
     * 车牌号
     */
    @Excel(name = "车牌号", width = 20)
    @ApiModelProperty(value = "车牌号")
    private String carNumber;
    /**
     * 车辆颜色
     */
    @Excel(name = "车辆颜色", width = 20)
    @ApiModelProperty(value = "车辆颜色")
    private String carColor;
    /**
     * 车辆类型
     */
    @Excel(name = "车辆类型", width = 20)
    @ApiModelProperty(value = "车辆类型")
    private String carType;
    /**
     * 轮询和实时标志
     */
    @Excel(name = "轮询和实时标志", width = 20)
    @ApiModelProperty(value = "轮询和实时标志")
    private Integer isRotate;

    /**
     * 人脸id
     */
    private String faceName;

    @TableField(exist = false)
    private String auditStatus;

    @TableField(exist = false)
    private String auditResult;

    @TableField(exist = false)
    private String auditPeople;

    @TableField(exist = false)
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    @TableField(exist = false)
    private String auditReason;

    @TableField(exist = false)
    private String alarmTypeDict;

    @TableField(exist = false)
    private String alarmAreaDict;

    @TableField(exist = false)
    private String auditPeopleName;

    // 人脸识别信息
    @TableField(exist = false)
    private FaceDetailDo faceDetailDo;

    @TableField(exist = false)
    private FaceGroupDo faceGroupDo;

	@TableField(exist = false)
	private String deviceName;
}
