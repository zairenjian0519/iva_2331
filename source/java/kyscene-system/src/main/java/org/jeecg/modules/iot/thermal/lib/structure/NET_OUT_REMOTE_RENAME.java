package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * CLIENT_RemoteRename 接口输出参数
*/
public class NET_OUT_REMOTE_RENAME extends NetSDKLib.SdkStructure
{
    /**
     * 结构体大小
    */
    public int              dwSize;

    public NET_OUT_REMOTE_RENAME() {
        this.dwSize = this.size();
    }
}

