package org.jeecg.modules.mqtt.handler;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.thirdApi.CameraStreamHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CameraStatusHandler {

    private final ICameraManagerService cameraManagerService;

    private final RedisUtil redisUtil;

    @Autowired
    private CameraStreamHandler cameraStreamHandler;

    @Autowired
    public CameraStatusHandler(ICameraManagerService cameraManagerService, RedisUtil redisUtil) {
        this.cameraManagerService = cameraManagerService;
        this.redisUtil = redisUtil;
    }

    public void handleMessage(String msg, String mqttClientId) {
        try {
            JSONArray cameraAry = JSON.parseObject(msg).getJSONArray("cameraStatusList");
            List<CameraManager> list = new ArrayList<>();
            for (Object obj : cameraAry) {
                JSONObject json = (JSONObject) obj;
                CameraManager cameraManager = new CameraManager();
                cameraManager.setId(json.getString("camera_id"));
                cameraManager.setCameraStatus(json.getString("status"));
                list.add(cameraManager);
            }
            //if (ObjectUtil.isNotEmpty(list)) cameraManagerService.updateBatchById(list);
            redisUtil.set(mqttClientId + "_cameraStatus", JSON.parseObject(msg));
            handleCameraPlayProcess(list);
        } catch (Exception e) {
            log.error("相机状态处理异常：{}", e.getLocalizedMessage());
        }
    }

    // 处理相机播放进程
    private void handleCameraPlayProcess(List<CameraManager> list) {
        if (ObjectUtil.isEmpty(list)) return;
        for (CameraManager cameraManager : list) {
            // 相机状态1.在线 4.离线
            if (Integer.parseInt(cameraManager.getCameraStatus()) == 4){
                JSONObject params = new JSONObject();
                params.put("cameraId", cameraManager.getId());
                cameraStreamHandler.stopCameraStream(params);
            }

        }
    }

}
