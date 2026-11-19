package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * CLIENT_StartSniffer 接口输出参数
*/
public class NET_OUT_START_SNIFFER extends NetSDKLib.SdkStructure
{
    public int              dwSize;

    public NET_OUT_START_SNIFFER() {
        this.dwSize = this.size();
    }
}

