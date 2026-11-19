package org.jeecg.modules.mqtt.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jeecg.common.api.dto.message.MessageDTO;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.constant.enums.MessageTypeEnum;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.system.vo.SysUserOnlineVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class CameraWarningHandler {

    private final ICameraManagerService cameraManagerService;

    private RedisTemplate redisTemplate;

    private RedisUtil redisUtil;

    private ISysBaseAPI sysBaseAPI;

    private IDeviceManagementService deviceManagementService;

    @Autowired
    public CameraWarningHandler(ICameraManagerService cameraManagerService){
        this.cameraManagerService = cameraManagerService;
    }

    @Autowired
    public void setRedisTemplate(RedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setRedisUtil(RedisUtil redisUtil){
        this.redisUtil = redisUtil;
    }

    @Autowired
    public void setISysBaseAPI(ISysBaseAPI sysBaseAPI){
        this.sysBaseAPI = sysBaseAPI;
    }

    @Autowired
    public void setICameraManagerService(IDeviceManagementService deviceManagementService){
        this.deviceManagementService = deviceManagementService;
    }

    public void handleMessage(String msg, String mqttClientId){
        String[] cameraIdList = msg.split(",");
        StringBuilder cameraNames = new StringBuilder();
        for (String cameraId : cameraIdList) {
            CameraManager camera = cameraManagerService.getById(cameraId);
            if (null != camera) {
                cameraNames.append(camera.getCameraName());
                cameraNames.append(" ");
            }
        }
        if (cameraNames.toString().equals("")) return;
        Collection<String> keys = redisTemplate.keys(CommonConstant.PREFIX_USER_TOKEN + "*");
        Set<String> onlineList = new HashSet<>();
        for (String key : keys) {
            String token = (String) redisUtil.get(key);
            if (StringUtils.isNotEmpty(token)) {
                SysUserOnlineVO online = new SysUserOnlineVO();
                online.setToken(token);
                LoginUser loginUser = sysBaseAPI.getUserByName(JwtUtil.getUsername(token));
                if (loginUser != null) {
                    onlineList.add(loginUser.getUsername());
                }
            }
        }
        DeviceManagement dev = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda().eq(DeviceManagement::getDeviceIp, mqttClientId));
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setToAll(true);
        messageDTO.setTitle("相机故障");
        messageDTO.setType(MessageTypeEnum.XT.getType());
        messageDTO.setToUser(String.join(",", onlineList));
        messageDTO.setFromUser("系统监控");
        String content = mqttClientId;
        if (null != dev) {
            content = dev.getDeviceName();
        }
        messageDTO.setContent("设备：" + content + " 相机:" + cameraNames + "故障，请确认相机是否正常工作。");
        sysBaseAPI.sendTemplateMessage(messageDTO);
    }
}
