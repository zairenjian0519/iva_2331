package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * CLIENT_GetSoftwareVersion 入参
*/
public class NET_IN_GET_SOFTWAREVERSION_INFO extends NetSDKLib.SdkStructure
{
    /**
     * 结构体大小
    */
    public int              dwSize;

    public NET_IN_GET_SOFTWAREVERSION_INFO() {
        this.dwSize = this.size();
    }
}

