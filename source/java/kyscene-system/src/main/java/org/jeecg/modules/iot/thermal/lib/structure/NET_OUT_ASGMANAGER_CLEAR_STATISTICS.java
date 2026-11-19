package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * CLIENT_ASGManagerClearStatistics 出参
*/
public class NET_OUT_ASGMANAGER_CLEAR_STATISTICS extends NetSDKLib.SdkStructure
{
    /**
     * 结构体大小
    */
    public int              dwSize;

    public NET_OUT_ASGMANAGER_CLEAR_STATISTICS() {
        this.dwSize = this.size();
    }
}

