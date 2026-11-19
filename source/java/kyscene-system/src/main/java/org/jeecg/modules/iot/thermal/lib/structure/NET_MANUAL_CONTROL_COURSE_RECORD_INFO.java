package org.jeecg.modules.iot.thermal.lib.structure;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
/**
 * 一键控制录播录像的信息
*/
public class NET_MANUAL_CONTROL_COURSE_RECORD_INFO extends NetSDKLib.SdkStructure
{
    /**
     * 控制类型,参见枚举定义 {@link org.jeecg.modules.iot.thermal.lib.enumeration.EM_MANUAL_CONTROL_COURSE_RECORD_TYPE}
    */
    public int              emType;
    /**
     * 预留对齐字节
    */
    public byte[]           szReserved1 = new byte[4];
    /**
     * 传入的标志符信息，Type参数为on时有效
    */
    public byte[]           szUUID = new byte[32];
    /**
     * 预留字节
    */
    public byte[]           szReserved = new byte[984];

    public NET_MANUAL_CONTROL_COURSE_RECORD_INFO() {
    }
}

