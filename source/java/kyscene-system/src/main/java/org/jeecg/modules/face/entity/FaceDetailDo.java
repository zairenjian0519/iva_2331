package org.jeecg.modules.face.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@TableName("face_detail")
public class FaceDetailDo {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 人脸组ID
     */
    private String faceGroup;

    /**
     * 矢量ID
     */
    private String vectorId;

    /**
     * 人脸姓名
     */
    private String faceName;

    /**
     * 人脸手机号
     */
    private String facePhone;

    /**
     * 人脸身份证号
     */
    private String faceIdCard;

    /**
     * 人脸图片原始名称
     */
    private String imageOriginalName;

    /**
     * 人脸图片新名称
     */
    private String imageNewName;

    /**
     * 人脸图片大小
     */
    private Long imageSize;

    /**
     * 人脸图片扩展名
     */
    private String extName;

    /**
     * 人脸图片路径
     */
    private String imagePath;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date createTime;

    /**
     * 更新人
     */
    private java.lang.String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date updateTime;

    /**
     * 人脸图片url
     */
    @TableField(exist = false)
    private String imgUrl;

    /**
     * 人脸token
     */
    private String faceToken;

    /**
     * 日志id
     */
    private String logId;

    @TableField(exist = false)
    private Integer deleteRetryCount;

    @TableField(exist = false)
    private Integer uploadRetryCount;
}
