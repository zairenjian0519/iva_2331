package org.jeecg.modules.mqtt.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthInfoHandler {

    private final IDeviceManagementService deviceManagementService;

    @Autowired
    public AuthInfoHandler(IDeviceManagementService deviceManagementService){
        this.deviceManagementService = deviceManagementService;
    }

    public void handleMessage(String msg, String mqttClientId){
        String info = "已获取认证信息";
        try {
            JSONObject authInfo = JSON.parseObject(msg);
            log.info("mqtt接收的认证信息：" + authInfo);
            DeviceManagement dev = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda().eq(DeviceManagement::getDeviceIp, mqttClientId));
            log.info("设备ip：" + dev.getDeviceIp());
            if (null == dev) {
                log.error("未找到设备信息");
                return;
            }
            //认证状态 0：未认证，1：已认证
            dev.setLicenseStatus(authInfo.getString("status"));
            if (authInfo.getString("status").equals("1")) {
                //授权可以配置算法的相机路数
                dev.setAlgorithmCameraNum(authInfo.getInteger("authorize_num"));
                //license版本号
                dev.setLicenseVersion(authInfo.getString("version"));
                //错误信息 成功为空
                dev.setLicenseError("");
                dev.setLicenseStatus("1");
            } else {
                dev.setAlgorithmCameraNum(null);
                dev.setLicenseVersion("");
            }
            //未认证成功才会返回错误信息
            if (authInfo.getString("status").equals("0")) {
                //license错误信息
                dev.setLicenseError(authInfo.getString("err_code"));
                info = authInfo.getString("err_code");
            } else {
                dev.setLicenseError("");
            }
            deviceManagementService.updateById(dev);
            log.info("设备状态：" + dev.getLicenseStatus());
            if (dev.getLicenseStatus().equals("0")) {
                log.error("设备认证错误：{}", dev.getLicenseStatus());
                return;
            }
            log.info("已获取到设备" + dev.getDeviceName() + "的认证信息！");
        } catch (Exception e) {
            log.error("处理设备认证信息出错：{}", e.getLocalizedMessage());
        }
    }
}
