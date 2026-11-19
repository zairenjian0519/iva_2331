package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * 获取区域状态 输入参数。此时CLIENT_GetAlarmRegionInfo的emType参数为NET_EM_GET_ALARMREGION_INFO_AREASTATUS
*/
public class NET_IN_GET_AREAS_STATUS extends NetSDKLib.SdkStructure
{
    /**
     * 结构体大小
    */
    public int              dwSize;
    /**
     * 获取异常防区类型,参见枚举定义 {@link org.jeecg.modules.iot.thermal.lib.enumeration.EM_GET_AREASSTATUS_TYPE}
    */
    public int              emType;

    public NET_IN_GET_AREAS_STATUS() {
        this.dwSize = this.size();
    }
}

