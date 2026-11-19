package org.jeecg.modules.system.vo;

import lombok.Data;

/**
 * @Desc:
 * @Author:lvgh
 * @Date:2024/5/23 14:22
 **/
@Data
public class DataPushVo {

    /**
     * 是否启动推送 1 启用  2 关闭
     */
    private int enablePush;


    private String url;
}
