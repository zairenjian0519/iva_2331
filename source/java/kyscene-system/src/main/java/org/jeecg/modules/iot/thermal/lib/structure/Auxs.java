package org.jeecg.modules.iot.thermal.lib.structure;/**
 * @author 47081
 * @descriptio
 * @date 2020/11/9
 * @version 1.0
 */

import org.jeecg.modules.iot.thermal.lib.NetSDKLib;

import static org.jeecg.modules.iot.thermal.lib.constant.SDKStructureFieldLenth.CFG_COMMON_STRING_32;

/**
 * @author 47081
 * @version 1.0
 * @description
 * @date 2020/11/9
 */
public class Auxs extends NetSDKLib.SdkStructure {
    public byte[]           auxs = new byte[CFG_COMMON_STRING_32];
}

