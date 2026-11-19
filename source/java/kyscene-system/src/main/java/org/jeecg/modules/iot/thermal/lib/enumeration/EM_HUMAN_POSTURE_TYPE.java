package org.jeecg.modules.iot.thermal.lib.enumeration;

import org.jeecg.modules.iot.thermal.lib.NetSDKLib;


/**
 * 关联的人体消息类型
 * 
 * @author ： 260611
 * @since ： Created in 2021/10/19 19:35
 */
public class EM_HUMAN_POSTURE_TYPE extends NetSDKLib.SdkStructure {
    /**
     *  未知
     */
    public static final int   EM_HUMAN_POSTURE_UNKNOWN = 0;
    /**
     *  与最优人脸同画面的人体
     */
    public static final int   EM_HUMAN_POSTURE_ALONG_WITH_FACE = 1;
}

