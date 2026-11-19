package org.jeecg.modules.thirdApi.pojo;

import lombok.Data;

import java.util.Date;
import java.util.Timer;

/**
 * 心跳维持的定时器缓存实体
 */
@Data
public class StreamHeartTimerCache {

    /**
     * 相机id
     */
    private String cameraId;

    /**
     * 定时器
     */
    private Timer timer;

    /**
     * 缓存时候的时间
     */
    private Date createTime;

}
