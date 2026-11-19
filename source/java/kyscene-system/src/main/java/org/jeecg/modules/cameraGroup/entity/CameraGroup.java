package org.jeecg.modules.cameraGroup.entity;

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
 * @Description: 相机组
 * @Author: jeecg-boot
 * @Date:   2023-02-14
 * @Version: V1.0
 */
@Data
@TableName("camera_group")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="camera_group对象", description="相机组")
public class CameraGroup {
    
	/**id*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
	private String id;
	/**轮询组名*/
	@Excel(name = "轮询组名", width = 15)
    @ApiModelProperty(value = "轮询组名")
	private String groupName;
	/**相机*/
	@Excel(name = "相机", width = 15)
    @ApiModelProperty(value = "相机")
	private String cameraIds;
	/**算法*/
	@Excel(name = "算法", width = 15)
    @ApiModelProperty(value = "算法")
	private String algorithmIds;
	/**创建时间*/
	@Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
	@DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value = "创建时间")
	private Date createTime;
	/**运行状态*/
	@Excel(name = "运行状态", width = 15)
	@ApiModelProperty(value = "运行状态")
	private Integer runStatus;
	/**轮询组检测周期*/
	@Excel(name = "轮询组检测周期", width = 15)
	@ApiModelProperty(value = "轮询组检测周期")
	private Integer pollingPeriod;

	@TableField(exist = false)
	private List<CameraVo> cameraList;
	@TableField(exist = false)
	private List<AlgorithmVo> algorithmList;

}
