/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal.module;

import com.sun.jna.Pointer;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.iot.thermal.ThermalService;
import org.jeecg.modules.iot.thermal.common.Res;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib;
import org.jeecg.modules.iot.thermal.lib.NetSDKLib.LLong;
import org.jeecg.modules.iot.thermal.lib.ToolKits;

/**
 * @author： DecoderX
 * @date： 2025/11/18
 */
public class ThermalCamera {

    // 设备断线通知回调
    private static final DisConnect disConnect = new DisConnect();

    // 网络连接恢复
    private static final HaveReConnect haveReConnect = new HaveReConnect();

    public static boolean login(CameraLoginParams cameraLoginParams) {
        if (NetSDKLib.NETSDK_INSTANCE == null || NetSDKLib.CONFIG_INSTANCE == null) {
            System.err.println("加载lib失败");
            return false;
        }
        // 是否为重新登录
        if (cameraLoginParams.isReLogin()) {
            boolean bRet = LoginModule.logout(cameraLoginParams.getIp());
            if (bRet) {
                LoginModule.cleanup();
            }
            boolean init = LoginModule.init(disConnect, haveReConnect);
            if (!init) {
                return false;
            }
            return LoginModule.login(cameraLoginParams.getIp(),
                    cameraLoginParams.getPort(),
                    cameraLoginParams.getUsername(),
                    cameraLoginParams.getPassword());
        }

        // 不是重新登录， 判断是否已经登录
        LLong loginHandle = LoginModule.loginHandleMap.get(cameraLoginParams.getIp());
        if (loginHandle!=null && loginHandle.longValue() != 0) {
            System.out.printf("[%s] 已经登录, 无需登录\n", cameraLoginParams.getIp());
            return true;
        }
        LoginModule.init(disConnect, haveReConnect);
        return LoginModule.login(cameraLoginParams.getIp(),
                cameraLoginParams.getPort(),
                cameraLoginParams.getUsername(),
                cameraLoginParams.getPassword());
    }

    public static boolean logout(String ipAddress) {
        LLong loginHandle = LoginModule.loginHandleMap.get(ipAddress);
        if (loginHandle!= null && loginHandle.longValue() == 0) {
            System.out.printf("[%s] 未登录, 无需登出\n", ipAddress);
            return true;
        }
        boolean bRet = LoginModule.logout(ipAddress);
        if (bRet) {
            LoginModule.cleanup();
        }
        return bRet;
    }

    // 根据点位规则获取温度
    public static TemperQueryResult queryTemperature(ItemQueryParams itemQueryParams) {
        int nQueryType = NetSDKLib.NET_QUERY_DEV_RADIOMETRY_TEMPER;

        // 入参
        NetSDKLib.NET_IN_RADIOMETRY_GETTEMPER stIn = new NetSDKLib.NET_IN_RADIOMETRY_GETTEMPER();
        stIn.stCondition.nPresetId = itemQueryParams.getPresetId();
        stIn.stCondition.nRuleId = itemQueryParams.getRuleId();
        stIn.stCondition.nMeterType = itemQueryParams.getMeterType(); 	// eg: NET_RADIOMETRY_METERTYPE.NET_RADIOMETRY_METERTYPE_AREA;
        stIn.stCondition.nChannel = itemQueryParams.getChannel();

        // 出参
        NetSDKLib.NET_OUT_RADIOMETRY_GETTEMPER stOut = new NetSDKLib.NET_OUT_RADIOMETRY_GETTEMPER();

        stIn.write();
        stOut.write();
        LLong loginHandle = LoginModule.loginHandleMap.get(itemQueryParams.getIpAddress());
        if (loginHandle == null || loginHandle.longValue() == 0) {
            System.err.printf("[%s] 登录句柄为空\n", itemQueryParams.getIpAddress());
            return null;
        }
        boolean bRet = LoginModule.netsdk.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
        if(!bRet) {
            System.err.printf("QueryPointTemper Failed!\n" + ToolKits.getErrorCodePrint());
            return null;
        }

        stOut.read();
        NetSDKLib.NET_RADIOMETRYINFO stItemInfo = stOut.stTempInfo;

        String[] data = new String[7];

        String [] arrMeterType = Res.string().getMeterTypeList();
        if (stItemInfo.nMeterType >= 1 &&
                stItemInfo.nMeterType <= arrMeterType.length) {
            data[0] = arrMeterType[stItemInfo.nMeterType-1];
        }else {
            data[0] = Res.string().getShowInfo("UNKNOWN");
        }

        String [] arrTemperUnit = Res.string().getTemperUnitList();
        if (stItemInfo.nTemperUnit >= 1 &&
                stItemInfo.nTemperUnit <= arrTemperUnit.length) {
            data[1] = arrTemperUnit[stItemInfo.nTemperUnit-1];
        }else {
            data[1] = Res.string().getShowInfo("UNKNOWN");
        }

        data[2] = String.valueOf(stItemInfo.fTemperAver);
        data[3] = String.valueOf(stItemInfo.fTemperMax);
        data[4] = String.valueOf(stItemInfo.fTemperMin);
        data[5] = String.valueOf(stItemInfo.fTemperMid);
        data[6] = String.valueOf(stItemInfo.fTemperStd);

        TemperQueryResult temperQueryResult = new TemperQueryResult();
        temperQueryResult.setMeterType(data[0]);
        temperQueryResult.setTemperUnit(data[1]);
        temperQueryResult.setTemperAver(data[2]);
        temperQueryResult.setTemperMax(data[3]);
        temperQueryResult.setTemperMin(data[4]);
        temperQueryResult.setTemperMid(data[5]);
        temperQueryResult.setTemperStd(data[6]);

        return temperQueryResult;
    }



    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    private static class DisConnect implements NetSDKLib.fDisConnect {
        public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("设备 [%s] 端口 [%d] 断开连接!\n", pchDVRIP, nDVRPort);
            LoginModule.loginHandleMap.remove(pchDVRIP);
            ThermalService thermalService = SpringContextUtils.getBean(ThermalService.class);
            thermalService.updateStatusByIp(pchDVRIP, 0);
        }
    }

    // 网络连接恢复，设备重连成功回调
    // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
    private static class HaveReConnect implements NetSDKLib.fHaveReConnect {
        public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("设备 [%s] 端口 [%d] 重连成功\n", pchDVRIP, nDVRPort);
            LoginModule.loginHandleMap.put(pchDVRIP, m_hLoginHandle);
            ThermalService thermalService = SpringContextUtils.getBean(ThermalService.class);
            thermalService.updateStatusByIp(pchDVRIP, 1);
        }
    }
}
