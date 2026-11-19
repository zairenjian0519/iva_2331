package org.jeecg.modules.wireless;

/**
 * 基础信息
 */
public interface Constant {

    // redis 缓存 系统参数
    String SYSTEM_PARAMETER_PREFIX = "system_parameter_cache_";

    // 4g 音柱token key
    String WIRELESS_PILLAR_TOKEN = "wireless_pillar_token";

    // 4g 音柱 token 获取的url
    String TOKEN_URL = "https://norsos.lionking110.com/sos/v1/mntn/account/appId/token";

    // 4G 音柱 appid
    String APPID = "b9c3b79c467c4983a3705dc702dab385";

    // 4g 音柱 appcode
    String APPCODE = "530c08a037524b1b9ac724c3c12e8074";

    // 4G 音柱列表
    String SOUND_PILLAR_LIST_URL = "https://norsos.lionking110.com/sos/v1/mntn/device/list";

    // 4G 音柱设备树
    String SOUND_PILLAR_LIST_TREE = "https://norsos.lionking110.com/sos/v1/sdk/device/groupTree/node/get";

    // 获取音频列表
    String AUDIO_FILE_LIST_URL = "https://norsos.lionking110.com/sos/v1/mntn/business/audioFile/list";

    // 播放音频文件地址
    String AUDIO_FILE_PLAY = "https://norsos.lionking110.com/sos/v1/mntn/business/play/task/start";

    // 关闭音频播放任务地址
    String AUDIO_FILE_STOP = "https://norsos.lionking110.com/sos/v1/mntn/business/play/task/stop";

    // 广播任务列表
    String AUDIO_BROADCAST_TASK_URL = "https://norsos.lionking110.com/sos/v1/mntn/business/play/task/list";

    // 删除广播任务
    String DELETE_BROADCAST_TASK = "https://norsos.lionking110.com/sos/v1/mntn/business/play/task/del";

}
