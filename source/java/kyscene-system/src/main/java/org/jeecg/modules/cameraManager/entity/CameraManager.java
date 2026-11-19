package org.jeecg.modules.cameraManager.entity;

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
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 相机管理
 * @Author: jeecg-boot
 * @Date: 2022-08-23
 * @Version: V1.0
 */
@Data
@TableName("camera_manager")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@ApiModel(value = "camera_manager对象", description = "相机管理")
public class CameraManager implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private Date createTime;

    /**
     * 相机名称
     */
    @Excel(name = "相机名称", width = 15)
    @ApiModelProperty(value = "相机名称")
    private String cameraName;

    /**
     * 相机归属
     */
    @Excel(name = "归属设备名称", width = 15, dictTable = "device_management", dicText = "device_name", dicCode = "id")
    @ApiModelProperty(value = "相机归属")
    private String cameraOwnership;

    /**
     * 相机状态
     */
    @Excel(name = "相机状态", width = 15, dicCode = "camera_status")
    @ApiModelProperty(value = "相机状态")
    @Dict(dicCode = "camera_status")
    private String cameraStatus;

    /**
     * 离线原因
     */
    @Excel(name = "离线原因", width = 15)
    @ApiModelProperty(value = "离线原因")
    private String offlineReason;

    /**
     * 场景定位
     */
    @Excel(name = "归属区域", width = 15, dictTable = "ky_category", dicText = "name", dicCode = "id")
    @ApiModelProperty(value = "场景定位")
    @Dict(dictTable = "ky_category", dicText = "name", dicCode = "id")
    private String location;

    @TableField(exist = false)
    private String locationDict;

    /**
     * 用户名
     */
    @Excel(name = "用户名", width = 15)
    @ApiModelProperty(value = "用户名")
    private String username;

    /**
     * 密码
     */
    @Excel(name = "密码", width = 15)
    @ApiModelProperty(value = "密码")
    private String password;

    /**
     * 厂商
     */
    @Excel(name = "厂商", width = 15, dicCode = "factory_name")
    @Dict(dicCode = "factory_name")
    @ApiModelProperty(value = "厂商")
    private String factory;

    /**
     * 流地址
     */
    @Excel(name = "码流地址", width = 15)
    @ApiModelProperty(value = "流地址")
    private String streamAddress;

    /**
     * 相机型号
     */
    @Excel(name = "相机型号", width = 15, dictTable = "camera_factory", dicText = "camera_type", dicCode = "id")
    @Dict(dictTable = "camera_factory", dicText = "camera_type", dicCode = "id")
    @ApiModelProperty(value = "相机型号")
    private String cameraType;

    /**
     * 1:主码流 0:子码流
     */
    @Excel(name = "码流", width = 15, dicCode = "stream_type")
//	@Dict(dicCode = "stream_type")
    @ApiModelProperty(value = "main:主码流 sub:子码流")
    private String streamType;

    /**
     * 码流格式
     */
    @Excel(name = "码流格式", width = 15, dicCode = "stream_format")
//	@Dict(dicCode = "stream_format")
    @ApiModelProperty(value = "码流格式")
    private String streamFormat;

    /**
     * 通道号
     */
    @Excel(name = "通道号", width = 32)
    @ApiModelProperty(value = "通道号")
    private Integer channel;

    /**
     * 工作类型
     */
//	@Excel(name = "工作类型", width = 15, dicCode = "work_type")
    @Dict(dicCode = "work_type")
    @ApiModelProperty(value = "工作类型")
    private String workType;

    /**
     * 场景别名
     */
//	@Excel(name = "场景别名", width = 15)
    @ApiModelProperty(value = "场景别名")
    private String cameraAlias;

    /**
     * IP
     */
    @Excel(name = "IP", width = 15)
    @ApiModelProperty(value = "IP")
    private String ip;

    /**
     * 端口
     */
    @Excel(name = "端口", width = 15)
    @ApiModelProperty(value = "端口")
    private Integer port;

    /**
     * 预览图
     */
//	@Excel(name = "预览图", width = 15)
    @ApiModelProperty(value = "预览图")
    private String pic;

    /**
     * 运行状态
     */
//	@Excel(name = "运行状态", width = 15)
    @ApiModelProperty(value = "运行状态")
    private String runStatus;

    /**
     * 备注
     */
//	@Excel(name = "备注", width = 15)
    @ApiModelProperty(value = "备注")
    private String remark;

    @TableField(exist = false)
    private String alarmType;

    @TableField(exist = false)
    private String categorys;

    @ApiModelProperty(value = "是否开启配置")
    private String isConfig;

    /**
     * 流地址
     */
    @Excel(name = "码流地址", width = 15)
    @ApiModelProperty(value = "流地址")
    private String streamAddressPush;

    /**
     * 1:主码流 0:子码流
     */
    @Excel(name = "码流", width = 15, dicCode = "stream_type")
//	@Dict(dicCode = "stream_type")
    @ApiModelProperty(value = "main:主码流 sub:子码流")
    private String streamTypePush;

    /**
     * 码流格式
     */
    @Excel(name = "码流格式", width = 15, dicCode = "stream_format")
//	@Dict(dicCode = "stream_format")
    @ApiModelProperty(value = "码流格式")
    private String streamFormatPush;

    @ApiModelProperty(value = "相机编码")
    private String cameraNumber;

    @TableField(exist = false)
    private String groupId;

    /**
     * 人脸组id
     */
    private String faceGroupId;

    /**
     * 人脸判断阈值
     */
    private Integer faceCheckThreshold;

    /**
     * 人脸相似度阈值
     */
    private Integer faceSimilarityScore;

}
