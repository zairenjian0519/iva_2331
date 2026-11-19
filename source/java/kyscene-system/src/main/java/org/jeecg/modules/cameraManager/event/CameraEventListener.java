package org.jeecg.modules.cameraManager.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.cameraManager.utils.CameraStatusCheckerNew;
import org.jeecg.modules.configjson.Const;
import org.jeecg.modules.devwebsocket.CameraStatusWebSocket;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.system.util.CmdUti;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 新增相机时候会发布一个事件，监听这个事件，检查相机是否在线
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CameraEventListener {

    private static final int OFFLINE_REASON_MAX_LENGTH = 50;

    private final ICameraManagerService cameraManagerService;

    private final RedisUtil redisUtil;

    private final CameraStatusWebSocket cameraStatusWebSocket;

    @Async
    @EventListener
    public void onApplicationEvent(CameraCheckEvent event) {
        // 异步处理相机状态
        handleCameraStatus(event);
    }

    private void handleCameraStatus(CameraCheckEvent cameraCheckEvent) {
        List<CameraManager> cameraManagerList = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        List<CameraEventMsg> cameraEvents = cameraCheckEvent.getCameraEventMsgList();
        for (CameraEventMsg cameraEventMsg : cameraEvents) {
            String cameraId = cameraEventMsg.getCameraId();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("camera_id", cameraId);

            String streamAddress = cameraEventMsg.getStreamAddress();
            JSONObject cameraOnlineJson = CameraStatusCheckerNew.check(streamAddress, cameraId);
            if (cameraOnlineJson == null) continue;

            CameraManager cameraManager = new CameraManager();
            cameraManager.setId(cameraId);
            if (cameraOnlineJson.getBoolean("success")) {
                cameraManager.setCameraStatus("1");
                cameraManager.setOfflineReason("");
                JSONObject streamInfo = cameraOnlineJson.getJSONObject("stream_info");
                cameraManager.setStreamFormat(streamInfo.getString("codec_name"));

                jsonObject.put("width", streamInfo.getIntValue("width"));
                jsonObject.put("height", streamInfo.getIntValue("height"));
                jsonObject.put("codec_id", streamInfo.get("codec_id"));
            } else {
                cameraManager.setCameraStatus("4");
                cameraManager.setOfflineReason(limitOfflineReason(cameraOnlineJson.getString("error")));
                jsonObject.put("height", 0);
                jsonObject.put("width", 0);
                jsonObject.put("codec_id", 0);
            }
            cameraManagerList.add(cameraManager);
            jsonArray.add(jsonObject);
        }

        if (!cameraManagerList.isEmpty()) {
            // Isolate persistence failures so one malformed camera response cannot
            // prevent every camera status from being refreshed.
            List<CameraManager> persistedCameraList = new ArrayList<>();
            for (CameraManager cameraManager : cameraManagerList) {
                try {
                    if (cameraManagerService.updateById(cameraManager)) {
                        persistedCameraList.add(cameraManager);
                    } else {
                        log.error("更新相机状态失败，cameraId={}", cameraManager.getId());
                    }
                } catch (Exception e) {
                    log.error("更新相机状态异常，cameraId={}，error={}", cameraManager.getId(), e.getMessage(), e);
                }
            }
            Map<String, CameraManager> persistedCameraMap = persistedCameraList.stream()
                    .collect(Collectors.toMap(CameraManager::getId, camera -> camera));
            JSONArray persistedJsonArray = new JSONArray();
            jsonArray.stream()
                    .filter(item -> persistedCameraMap.containsKey(((JSONObject) item).getString("camera_id")))
                    .forEach(persistedJsonArray::add);
            cameraManagerList = persistedCameraList;
            jsonArray = persistedJsonArray;
        }

        if (!cameraManagerList.isEmpty()) {
            // 通过mqtt广播相机状态
            broadcastCameraStatus(cameraCheckEvent.getEventOptions().getDeviceIp(), cameraManagerList);
            // 通过websocket广播相机状态
            JSONObject message = new JSONObject();
            message.put("event", "refresh");
            cameraStatusWebSocket.sendMessageToUser(message.toJSONString());
        }

        log.info("相机状态检查集合jsonArray：{}", jsonArray);
        if (!jsonArray.isEmpty()) {
            // 更新redis中的相机信息
            updateCache(jsonArray);
            // 是否重启AI服务，定时任务需要判断是否重启AI服务
            String eventType = cameraCheckEvent.getEventOptions().getEventType();
            boolean statusChanged = false;
            if ("schedule_job".equals(eventType) || "algorithm_config".equals(eventType)) {
                statusChanged = isRestartAiServer(cameraCheckEvent, cameraManagerList);
                log.info("是否重启AI服务：{}", statusChanged);
            }
            if (shouldRestartAiServer(eventType, statusChanged)) CmdUti.restartAiServer();
        }

    }

    static boolean shouldRestartAiServer(String eventType, boolean statusChanged) {
        if ("schedule_job".equals(eventType) || "algorithm_config".equals(eventType)) {
            return statusChanged;
        }
        return true;
    }

    static String limitOfflineReason(String reason) {
        if (reason == null) return null;
        int codePointCount = reason.codePointCount(0, reason.length());
        if (codePointCount <= OFFLINE_REASON_MAX_LENGTH) return reason;
        int endIndex = reason.offsetByCodePoints(0, OFFLINE_REASON_MAX_LENGTH);
        return reason.substring(0, endIndex);
    }

    // 只要当前相机状态与原始状态不一致，就返回 true, 否则返回 false
    private static boolean isRestartAiServer(CameraCheckEvent cameraCheckEvent, List<CameraManager> cameraManagerList) {
        List<CameraManager> cameraOriginalList = cameraCheckEvent.getEventOptions().getCameraOriginalList();

        // 将原始相机状态转换为 Map：key 是相机 ID，value 是状态
        Map<String, String> originalStatusMap = cameraOriginalList.stream()
                .collect(Collectors.toMap(CameraManager::getId, CameraManager::getCameraStatus));

        // 遍历当前相机列表，只要有任一相机状态与原始状态不同，就返回 true
        return cameraManagerList.stream()
                .anyMatch(camera -> {
                    String originalStatus = originalStatusMap.get(camera.getId());
                    return originalStatus != null && !originalStatus.equals(camera.getCameraStatus());
                });
    }

    private void updateCache(JSONArray jsonArray) {
        // 缓存的是字符串
        Object cacheInfo = redisUtil.get(Const.CAMERA_LIST_INFO_KEY);
        if (cacheInfo == null) return;
        if (cacheInfo instanceof String cacheStr) {
            Map<String, JSONObject> jsonMap = jsonArray.stream()
                    .collect(Collectors.toMap(json -> ((JSONObject) json).getString("camera_id"), json -> (JSONObject) json));

            JSONArray cacheArray = JSON.parseArray(cacheStr);
            for (int i = 0; i < cacheArray.size(); i++) {
                JSONObject cacheJsonObj = cacheArray.getJSONObject(i);
                String cameraId = cacheJsonObj.getString("camera_id");
                if (jsonMap.containsKey(cameraId)) {
                    JSONObject json = jsonMap.get(cameraId);
                    cacheJsonObj.put("codec_id", json.getString("codec_id"));
                    cacheJsonObj.put("width", json.getInteger("width"));
                    cacheJsonObj.put("height", json.getInteger("height"));
                }
            }
            redisUtil.set(Const.CAMERA_LIST_INFO_KEY, JSON.toJSONString(cacheArray));
        }
    }

    private void broadcastCameraStatus(String deviceIp, List<CameraManager> cameraManagerList) {
        JSONObject jsonObject = new JSONObject();
        List<JSONObject> list = cameraManagerList.stream().map(cameraManager -> {
            JSONObject cameraJson = new JSONObject();
            cameraJson.put("camera_id", cameraManager.getId());
            cameraJson.put("status", cameraManager.getCameraStatus());
            return cameraJson;
        }).toList();
        jsonObject.put("cameraStatusList", list);
        MqttClientConnect mqttClient;
        try {
            mqttClient = MqttClientService.getInstance().getMqttClient(deviceIp);
            mqttClient.pub("web_getcamerastatus", jsonObject.toJSONString(), 1);
        } catch (MqttException e) {
            log.error("发布相机状态消息失败:{}", e.getLocalizedMessage());
        }
    }
}
