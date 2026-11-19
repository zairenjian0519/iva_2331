package org.jeecg.modules.thirdApi.pojo;

import lombok.Data;

import java.util.Date;

/**
 * 视频流播放信息
 */
@Data
public class StreamPlayInfo {

    /**
     * 相机ID
     */
    private String cameraId;

    /**
     * 播放来源
     * 前端 - web
     * 后端 - api
     */
    private Boolean web = false;


    /**
     * 播放来源
     * 前端 - web
     * 后端 - api
     */
    private Boolean api = false;

    /**
     * 创建时间
     */
    private Date updateTime;
}
