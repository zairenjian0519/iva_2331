package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * CLIENT_AttachResultOfVehicleHistoryByPic 接口输出参数
*/
public class NET_OUT_ATTACH_RESULT_VEHICLE_HISTORY_BYPIC extends NetSDKLib.SdkStructure
{
    /**
     * 结构体大小
    */
    public int              dwSize;

    public NET_OUT_ATTACH_RESULT_VEHICLE_HISTORY_BYPIC() {
        this.dwSize = this.size();
    }
}

