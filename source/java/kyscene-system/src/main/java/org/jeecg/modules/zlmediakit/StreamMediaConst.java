package org.jeecg.modules.zlmediakit;

/**
 * 流媒体const字段
 */
public interface StreamMediaConst {

    // 获取流媒体列表
    String GET_MEDIA_LIST_URI = "/index/api/getMediaList";

    // 开启流代理
    // 直接代理
     String DIRECT_STREAM_PROXY = "/index/api/addStreamProxy";

    // ffmpeg代理
    String FFMPEG_STREAM_PROXY = "/index/api/addFFmpegSource";

    // 关闭流
    String CLOSE_STREAM = "/index/api/close_streams";

    // 流格式
    String STREAM_FORMAT_H264 = "h264";

    String STREAM_FORMAT_H265 = "hevc";

    String STREAM_FORMAT_MJPEG = "mjpeg";
}