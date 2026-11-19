package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * CLIENT_SetSplitAudioOuput接口输出参数(设置音频输出模式)
*/
public class NET_OUT_SET_AUDIO_OUTPUT extends NetSDKLib.SdkStructure
{
    public int              dwSize;

    public NET_OUT_SET_AUDIO_OUTPUT() {
        this.dwSize = this.size();
    }
}

