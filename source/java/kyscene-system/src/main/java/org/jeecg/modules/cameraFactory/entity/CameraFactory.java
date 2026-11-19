package org.jeecg.modules.cameraFactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * @Description: 相机基础信息
 * @Author: jeecg-boot
 * @Date:   2022-08-29
 * @Version: V1.0
 */
@Data
@TableName("camera_factory")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="camera_factory对象", description="相机基础信息")
public class CameraFactory implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private Date createTime;
	/**相机型号*/
	@Excel(name = "相机型号", width = 15)
    @ApiModelProperty(value = "相机型号")
    private String cameraType;
	/**用户名*/
	@Excel(name = "用户名", width = 15)
    @ApiModelProperty(value = "用户名")
    private String username;
	/**密码*/
	@Excel(name = "密码", width = 15)
    @ApiModelProperty(value = "密码")
    private String password;
	/**IP*/
	@Excel(name = "IP", width = 15)
    @ApiModelProperty(value = "IP")
    private String ip;
	/**端口*/
	@Excel(name = "端口", width = 15)
    @ApiModelProperty(value = "端口")
    private Integer port;
	/**厂家名称*/
	@Excel(name = "厂家名称", width = 15, dicCode = "factory_name")
	@Dict(dicCode = "factory_name")
    @ApiModelProperty(value = "厂家名称")
			private String factoryName;
	/**码流*/
	@Excel(name = "码流", width = 15, dicCode = "stream_type")
	@Dict(dicCode = "stream_type")
    @ApiModelProperty(value = "码流")
    private String streamType;
	/**码流格式*/
	@Excel(name = "码流格式", width = 15, dicCode = "stream_format")
	@Dict(dicCode = "stream_format")
    @ApiModelProperty(value = "码流格式")
    private String streamFormat;
	/**视频协议*/
	@Excel(name = "视频协议", width = 15)
    @ApiModelProperty(value = "视频协议")
    private String protocol;
}
