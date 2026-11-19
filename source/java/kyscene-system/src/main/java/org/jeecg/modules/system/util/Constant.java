package org.jeecg.modules.system.util;

/**
 * 固定字段
 */
public interface Constant {

    // 系统参数缓存的 key
    String PARAMETER_CACHE_KEY = "system_parameter_cache_key";

    /**
     *  是否开启算法视频预览模块，用于网页上判断
     *  系统参数
     */
    String ALG_STREAM_PAGE_SYS_CODE = "EnableAlgorithmStream";

    /**
     * 算法预览模块 开启：1 不开启：其他
     */
    String ALG_STREAM_PAGE_SYS_FLAG = "1";

    /**
     *  是否启用 百度人脸识别
     */
    String ENABLE_BAIDU_FACE_API = "baidu_face_setting";
}
