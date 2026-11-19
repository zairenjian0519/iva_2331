package org.jeecg.modules.deviceinfo.entity;

import lombok.Data;

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

public class DeviceinfoVO implements Serializable {


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
