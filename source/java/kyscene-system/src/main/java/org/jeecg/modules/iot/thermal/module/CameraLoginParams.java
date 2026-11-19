/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal.module;

import lombok.Data;

/**
 * @author： DecoderX
 * @date： 2025/11/18
 */
@Data
public class CameraLoginParams {
    /**
     * 相机ip
     */
    private String ip;

    /**
     * 相机端口
     */
    private int port;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 是否重新登录
     */
    private boolean reLogin;
}
