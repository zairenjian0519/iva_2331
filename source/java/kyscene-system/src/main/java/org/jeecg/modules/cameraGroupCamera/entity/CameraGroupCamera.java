package org.jeecg.modules.cameraGroupCamera.entity;

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
 * @Description: 轮询组相机表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
@Data
@TableName("camera_group_camera")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="camera_group_camera对象", description="轮询组相机表")
public class CameraGroupCamera {
    
	/**id*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
	private String id;
	/**cameraId*/
	@Excel(name = "cameraId", width = 15)
    @ApiModelProperty(value = "cameraId")
	private String cameraId;
	/**groupId*/
	@Excel(name = "groupId", width = 15)
    @ApiModelProperty(value = "groupId")
	private String groupId;
	/**createTime*/
	@Excel(name = "createTime", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "createTime")
	private Date createTime;
}
