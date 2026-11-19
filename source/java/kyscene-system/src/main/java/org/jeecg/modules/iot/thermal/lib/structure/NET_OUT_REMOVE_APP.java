package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * CLIENT_RemoveApp 接口输出参数
*/
public class NET_OUT_REMOVE_APP extends NetSDKLib.SdkStructure
{
    /**
     * 结构体大小
    */
    public int              dwSize;

    public NET_OUT_REMOVE_APP() {
        this.dwSize = this.size();
    }
}

