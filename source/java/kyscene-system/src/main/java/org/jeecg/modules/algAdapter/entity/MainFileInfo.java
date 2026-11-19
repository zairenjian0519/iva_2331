/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.algAdapter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author： DecoderX
 * @date： 2025/7/15
 */
@Data
@TableName("algorithm_main_file")
public class MainFileInfo {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件名
     */
    private String fileOriginalName;

    /**
     * 文件新名称
     */
    private String fileNewName;

    /**
     * 文件大小
     */
    private Double fileSize;

    /**
     * 文件后缀
     */
    private String fileExt;

    /**
     * 文件类型：main 主文件 model 模型文件
     */
    private String fileType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 工作路径
     */
    private String workPath;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
