package org.jeecg.modules.wireless.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 音柱信息
 */
@Data
@TableName("sound_device_info")
public class SoundDeviceInfo {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 音柱id
     */
    private Integer deviceId;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备序列号
     */
    private String serialNum;

    /**
     * 证书ID
     */
    private String licenseId;

    /**
     * 在线时间
     */
    private Date onlineTime;

    /**
     * 在线状态
     */
    private String appState;

    /**
     * 信号强度
     */
    private String signalStrength;

    /**
     * 启用 1或者禁用 0
     */
    private Integer enable;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区域
     */
    private String district;

    /**
     * 乡镇
     */
    private String town;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 音量
     */
    private Integer volume;

    /**
     * 绑定的相机列表
     */
    private String cameraList;
}
