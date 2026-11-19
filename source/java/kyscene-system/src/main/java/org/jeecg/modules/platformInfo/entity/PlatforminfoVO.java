package org.jeecg.modules.platformInfo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 封装返回TXT数据,
 */
@Data
public class PlatforminfoVO {
/*    *//**
     * 主键id
     *//*
    private String id;*/

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

    @TableField(exist = false)//数据库没有此字段
    private List<Object> deviceidlist = new ArrayList<>();
    /**
     * 1:平台,2:摄像头,默认为2
     */

    private String type;

    public void devicedListAdd(String s) {
        deviceidlist.add(s);
    }
}
