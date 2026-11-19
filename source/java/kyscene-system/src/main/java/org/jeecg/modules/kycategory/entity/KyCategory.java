package org.jeecg.modules.kycategory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.List;

/**
 * 分类数据
 */
@Data
@TableName("ky_category")
@ApiModel(value = "ky_category对象", description = "分类数据")
public class KyCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
    /**
     * 名称
     */
    @Excel(name = "名称", width = 15)
    @ApiModelProperty(value = "名称")
    private java.lang.String name;
    /**
     * 编码
     */
    @Excel(name = "编码", width = 15)
    @ApiModelProperty(value = "编码")
    private java.lang.String code;
    /**
     * 父ID
     */
    @Excel(name = "父ID", width = 15)
    @ApiModelProperty(value = "父ID")
    private java.lang.String pid;

    /**
     * 数据类别 0:告警 1:区域
     */
    @Excel(name = "数据类别 0:告警 1:区域", width = 15)
    @ApiModelProperty(value = "数据类别 0:告警 1:区域")
    private java.lang.String category;
    /**
     * 是否有子节点
     */
    @Excel(name = "是否有子节点", width = 15, dicCode = "yn")
    @Dict(dicCode = "yn")
    @ApiModelProperty(value = "是否有子节点")
    private java.lang.String hasChild;
    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private java.util.Date createTime;

    /**
     * 算法默认阈值
     */
    @Excel(name = "算法默认阈值", width = 15)
    @ApiModelProperty(value = "算法默认阈值")
    private java.lang.Double threshold;
    /**
     * 目标数量阈值
     */
    @Excel(name = "目标数量阈值", width = 15)
    @ApiModelProperty(value = "目标数量阈值")
    private java.lang.Integer targetThreshold;

    @TableField(exist = false)
    private List<KyCategory> children;

    /**
     * 连续命中次数
     */
    private Integer hitCount;

    /**
     * 跟踪框交并比
     */
    private Integer iouThreshold;

    /**
     * 误报灵敏度
     */
    private Integer sensitivity;

    /**
     * 时间持续间隔
     */
    private Integer timeSpace;

}
