package org.jeecg.modules.deviceinfo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author 
 * @since 2023-12-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("deviceinfo")
public class Deviceinfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 平台地址
     */
    private String address;

    /**
     * 用户
     */
    private String name;

    /**
     * 国标鉴权密码经base64加密
     */
    private String password;

    /**
     * 默认为7100
     */
    private String port;

    /**
     * 每个相机的国标流编码
     */
    private String sipid;

    /**
     * 平台的国标流编码
     */
    private String platformid;

    /**
     * 1:平台,2:摄像头,默认为2
     */
    private String type;


}
