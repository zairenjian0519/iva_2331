package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * 获取SIM卡的状态类型出参
*/
public class NET_OUT_SIMINFO_GET_SIMSTATE extends NetSDKLib.SdkStructure
{
    /**
     * 赋值为结构体大小
    */
    public int              dwSize;
    /**
     * SIM卡状态类型,参见枚举定义 {@link org.jeecg.modules.iot.thermal.lib.enumeration.EM_SIMSTATE_MODE}
    */
    public int              emMode;

    public NET_OUT_SIMINFO_GET_SIMSTATE() {
        this.dwSize = this.size();
    }
}

