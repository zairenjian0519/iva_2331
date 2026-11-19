package org.jeecg.modules.platformInfo.entity;

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
 * @since 2023-12-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "platforminfo")
public class Platforminfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 平台地址
     */

    private String address;

    /**
     * 用户名称
     */

    private String name;

    /**
     * 国标鉴权密码经base64 加密
     */

    private String password;

    /**
     * 固定为7100
     */

    private String port;

    /**
     * 平台的国标流编码(20)
     */

    private String sipid;


    /**
     * 1:平台,2:摄像头,默认为2
     */

    private String type;


}
