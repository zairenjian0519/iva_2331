package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * 添加电视墙输出参数
*/
public class NET_OUT_MONITORWALL_ADD extends NetSDKLib.SdkStructure
{
    public int              dwSize;
    /**
     * 电视墙ID
    */
    public int              nMonitorWallID;

    public NET_OUT_MONITORWALL_ADD() {
        this.dwSize = this.size();
    }
}

