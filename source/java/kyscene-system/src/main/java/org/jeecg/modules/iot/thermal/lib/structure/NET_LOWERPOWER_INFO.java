package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * 低电压附带信息
*/
public class NET_LOWERPOWER_INFO extends NetSDKLib.SdkStructure
{
    /**
     * 当前电量
    */
    public int              nPercent;
    /**
     * 预留
    */
    public byte[]           szReserved = new byte[128];

    public NET_LOWERPOWER_INFO() {
    }
}

