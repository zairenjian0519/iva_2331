package org.jeecg.modules.wireless.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 音频文件实体类
 */
@Data
@TableName("sound_device_audio")
public class AudioFileInfo {
    /**
     * 主键id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 音频文件id
     */
    private Integer audioId;

    /**
     * 文件类型
     */
    private String  fileType;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 文件大小
     */
    private String fileSize;

    /**
     * 通道编号
     */
    private Integer chanelNum;

    /**
     * 文件时长
     */
    private String duration;

    /**
     * 频率
     */
    private Integer sampleRate;

    /**
     * MD5
     */
    private String md5;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 访问路径
     */
    private String accessUrl;

    /**
     * 内网地址
     */
    private String intranetUrl;

    /**
     * 备注
     */
    private String remark;

    /**
     * 上传时间
     */
    private Date uploadTime;

    /**
     * 文件uuid
     */
    private String fileUuid;

    /**
     * 无线网址
     */
    private String wirelessUrl;

    /**
     * 启用 1 禁用 0
     */
    private Integer status;

    /**
     * 算法code
     */
    private String algorithm;

}
