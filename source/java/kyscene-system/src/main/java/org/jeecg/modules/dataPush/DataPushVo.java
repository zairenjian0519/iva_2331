package org.jeecg.modules.dataPush;

import lombok.Data;

/**
 * 数据推送实体类
 */
@Data
public class DataPushVo {

    /**
     * 告警推送启用
     */
    private boolean enable;

    /**
     * 推送类型
     */
    private String pushType;

    /**
     * 携带token的类型，参数形式/Header形式
     */
    private String tokenType;

    /**
     * 如果token携带类型是参数
     * 参数名称
     */
    private String paramName;

    /**
     * 如果token携带类型是Header
     * 携带token的header key
     */
    private String headerKey;

    /**
     * 告警推送地址
     */
    private String pushUrl;

    /**
     * 获取token用户名
     */
    private String username;

    /**
     * 获取token密码
     */
    private String password;

    /**
     * 获取token的地址
     */
    private String tokenUrl;
}
