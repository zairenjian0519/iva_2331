package org.jeecg.modules.viewPreview.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

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
 * @Description: 实时预览
 * @Author: jeecg-boot
 * @Date:   2022-08-16
 * @Version: V1.0
 */
@Data
@TableName("camera_manager")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="camera_manager对象", description="实时预览")
public class ViewPreview {
    
	/**id*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
	private String id;
	/**创建人*/
	@Excel(name = "创建人", width = 15)
    @ApiModelProperty(value = "创建人")
	private String createBy;
	/**创建日期*/
	@Excel(name = "创建日期", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
	private Date createTime;
	/**相机型号*/
	@Excel(name = "相机型号", width = 15)
    @ApiModelProperty(value = "相机型号")
	private String cameraType;
	/**厂商*/
	@Excel(name = "厂商", width = 15)
    @ApiModelProperty(value = "厂商")
	private String factory;
	/**相机名称*/
	@Excel(name = "相机名称", width = 15)
    @ApiModelProperty(value = "相机名称")
	private String cameraName;
	/**场景别名*/
	@Excel(name = "场景别名", width = 15)
    @ApiModelProperty(value = "场景别名")
	private String cameraAlias;
	/**工作类型*/
	@Excel(name = "工作类型", width = 15)
    @ApiModelProperty(value = "工作类型")
	private String workType;
	/**相机归属*/
	@Excel(name = "相机归属", width = 15)
    @ApiModelProperty(value = "相机归属")
	private String cameraOwnership;
	/**场景定位*/
	@Excel(name = "场景定位", width = 15)
    @ApiModelProperty(value = "场景定位")
	private String location;
	/**IP*/
	@Excel(name = "IP", width = 15)
    @ApiModelProperty(value = "IP")
	private String ip;
	/**端口*/
	@Excel(name = "端口", width = 15)
    @ApiModelProperty(value = "端口")
	private Integer port;
	/**用户名*/
	@Excel(name = "用户名", width = 15)
    @ApiModelProperty(value = "用户名")
	private String username;
	/**密码*/
	@Excel(name = "密码", width = 15)
    @ApiModelProperty(value = "密码")
	private String password;
	/**相机状态*/
	@Excel(name = "相机状态", width = 15)
    @ApiModelProperty(value = "相机状态")
	private String cameraStatus;
	/**1:主码流 0:子码流*/
	@Excel(name = "1:主码流 0:子码流", width = 15)
    @ApiModelProperty(value = "1:主码流 0:子码流")
	private String streamType;
	/**码流格式*/
	@Excel(name = "码流格式", width = 15)
    @ApiModelProperty(value = "码流格式")
	private String streamFormat;
	/**流地址*/
	@Excel(name = "流地址", width = 15)
    @ApiModelProperty(value = "流地址")
	private String streamAddress;
	/**预览图*/
	@Excel(name = "预览图", width = 15)
    @ApiModelProperty(value = "预览图")
	private String pic;
	/**运行状态*/
	@Excel(name = "运行状态", width = 15)
    @ApiModelProperty(value = "运行状态")
	private String runStatus;
	/**备注*/
	@Excel(name = "备注", width = 15)
    @ApiModelProperty(value = "备注")
	private String remark;
	/**通道号*/
	@Excel(name = "通道号", width = 32)
	@ApiModelProperty(value = "通道号")
	private Integer channel;
	/**相机的所有告警数*/
	@TableField(exist = false)
	private Integer allCameraCount;
	/**相机的严重告警数*/
	@TableField(exist = false)
	private Integer seriousCameraCount;
	@TableField(exist = false)
	private String alarmType;
	@TableField(exist = false)
	private String alarmTypeDict;
	@TableField(exist = false)
	private String locationDict;
	@TableField(exist = false)
	private String deviceDict;
	@TableField(exist = false)
	private String deviceIp;
	/**流地址*/
	@Excel(name = "码流地址", width = 15)
	@ApiModelProperty(value = "流地址")
	private String streamAddressPush;
	/**1:主码流 0:子码流*/
	@Excel(name = "码流", width = 15, dicCode = "stream_type")
	@ApiModelProperty(value = "main:主码流 sub:子码流")
	private String streamTypePush;
	/**码流格式*/
	@Excel(name = "码流格式", width = 15, dicCode = "stream_format")
	@ApiModelProperty(value = "码流格式")
	private String streamFormatPush;
}
