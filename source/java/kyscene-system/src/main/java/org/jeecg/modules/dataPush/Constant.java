package org.jeecg.modules.dataPush;

/**
 * 固定字段
 */
public interface Constant {

    // 数据retry，每条数据延迟
    Long delayTime = 500L;

    // 数据推送系统参数 code
    String DATA_PUSH_CODE = "AlarmDataPush";

    // 数据推送在系统参数中的名称
    String PARAM_NAME = "数据推送";

    // 数据推送在系统参数中的描述
    String PARAM_DESCRIPTION = "数据推送配置";

    // 数据推送类型 token
    String PUSH_TYPE_TOKEN = "token";

    // 数据推送类型 direct
    String PUSH_TYPE_DIRECT = "direct";

    // token请求接口，token携带类型为 参数
    String TOKEN_TYPE_PARAM = "param";

    // token请求接口，token携带类型为 header
    String TOKEN_TYPE_HEADER = "header";

    // 视频流业务地址系统参数code
    String STREAM_BIZ_ADDRESS = "StreamBusinessIpConfig";
}
