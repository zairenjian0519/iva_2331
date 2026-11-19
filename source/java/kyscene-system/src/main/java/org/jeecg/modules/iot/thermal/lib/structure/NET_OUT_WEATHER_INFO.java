package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * 订阅气象信息输出参数
*/
public class NET_OUT_WEATHER_INFO extends NetSDKLib.SdkStructure
{
    public int              dwSize;

    public NET_OUT_WEATHER_INFO() {
        this.dwSize = this.size();
    }
}

