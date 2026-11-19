package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * 智能球机控制输出参数
*/
public class NET_OUT_CONTROL_INTELLITRACKER extends NetSDKLib.SdkStructure
{
    public int              dwSize;

    public NET_OUT_CONTROL_INTELLITRACKER() {
        this.dwSize = this.size();
    }
}

