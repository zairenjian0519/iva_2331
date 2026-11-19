package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * CLIENT_GetASGState 接口输入参数
*/
public class NET_IN_ACCESS_GET_ASG_STATE extends NetSDKLib.SdkStructure
{
    public int              dwSize;

    public NET_IN_ACCESS_GET_ASG_STATE() {
        this.dwSize = this.size();
    }
}

