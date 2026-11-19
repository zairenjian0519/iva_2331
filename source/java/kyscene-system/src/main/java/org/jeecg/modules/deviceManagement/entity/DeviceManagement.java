package org.jeecg.modules.deviceManagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备管理
 */
@Data
@TableName("device_management")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "device_management对象", description = "设备管理")
public class DeviceManagement implements Serializable {

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
     * 设备名称
     */
    @Excel(name = "设备名称", width = 15)
    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    /**
     * 设备ip
     */
    @Excel(name = "设备ip", width = 15)
    @ApiModelProperty(value = "设备ip")
    private String deviceIp;

    /**
     * 网卡ip
     */
    @Excel(name = "网卡ip", width = 15)
    @ApiModelProperty(value = "网卡ip")
    private String networkIp;

    /**
     * 类别
     */
    @Excel(name = "类别", width = 15, dicCode = "device_category")
    @Dict(dicCode = "device_category")
    @ApiModelProperty(value = "类别")
    private String category;

    /**
     * 状态
     */
    @Excel(name = "状态", width = 15, dicCode = "device_status")
    @Dict(dicCode = "device_status")
    @ApiModelProperty(value = "状态")
    private String status;

    /**
     * 厂家名称
     */
    @Excel(name = "厂家名称", width = 15, dicCode = "factory_name")
    @Dict(dicCode = "factory_name")
    @ApiModelProperty(value = "厂家名称")
    private String factoryName;

    /**
     * 摄像头数量
     */
    @Excel(name = "摄像头数量", width = 15)
    @ApiModelProperty(value = "摄像头数量")
    private Integer cameraNum;

    /**
     * 算法数量
     */
    @Excel(name = "算法数量", width = 15)
    @ApiModelProperty(value = "算法数量")
    private Integer algorithmNum;

    /**
     * license状态
     */
    @Excel(name = "license状态", width = 15)
    @ApiModelProperty(value = "license状态")
    private String licenseStatus;

    /**
     * 用户名
     */
    @Excel(name = "用户名", width = 15)
    @ApiModelProperty(value = "用户名")
    private java.lang.String user;

    /**
     * 密码
     */
    @Excel(name = "密码", width = 15)
    @ApiModelProperty(value = "密码")
    private java.lang.String pwd;

    /**
     * 数据存储时长
     */
    @Excel(name = "数据存储时长", width = 15)
    @ApiModelProperty(value = "数据存储时长")
    private java.lang.String dataSave;

    @Excel(name = "公司编码加行业编码", width = 15)
    @ApiModelProperty(value = "公司编码加行业编码")
    private java.lang.String deviceCode;

    @Excel(name = "设备编码", width = 15)
    @ApiModelProperty(value = "设备编码")
    private java.lang.String deviceNumber;

    @TableField(exist = false)
    @Excel(name = "是否可选合成流", width = 15)
    @ApiModelProperty(value = "是否可选合成流")
    private java.lang.Boolean isTypeChoose;

    @Excel(name = "可配算法的相机数量", width = 15)
    @ApiModelProperty(value = "可配算法的相机数量")
    private java.lang.Integer algorithmCameraNum;

    @Excel(name = "设备机器码", width = 15)
    @ApiModelProperty(value = "设备机器码")
    private java.lang.String machineId;

    @Excel(name = "license版本号", width = 15)
    @ApiModelProperty(value = "license版本号")
    private java.lang.String licenseVersion;

    @Excel(name = "license错误信息", width = 15)
    @ApiModelProperty(value = "license错误信息")
    private java.lang.String licenseError;
}
