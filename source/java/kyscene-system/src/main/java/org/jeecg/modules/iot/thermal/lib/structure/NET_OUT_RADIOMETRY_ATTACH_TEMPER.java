package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * CLIENT_RadiometryAttachTemper 出参
*/
public class NET_OUT_RADIOMETRY_ATTACH_TEMPER extends NetSDKLib.SdkStructure
{
    public int              dwSize;

    public NET_OUT_RADIOMETRY_ATTACH_TEMPER() {
        this.dwSize = this.size();
    }
}

