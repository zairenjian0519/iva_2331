/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecg.modules.face.service.FaceGroupService;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author： DecoderX
 * @date： 2025/8/7
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BaiduFaceSearch {
    // 人脸搜索地址
    private static final String FACE_SEARCH_URL = "https://aip.baidubce.com/rest/2.0/face/v3/search";

    // private final IAlarmDetailService alarmDetailService;

    private final FaceDataQueue faceDataQueue;

    private final FaceGroupService faceGroupService;

    @PostConstruct
    public void startConsumer() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            Map<String, Object> event = faceDataQueue.get();
            try {
                if (event != null) {
                    JSONObject jsonResult = new JSONObject();
                    jsonResult.put("id", event.get("cameraId"));

                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("key", "");
                    dataMap.put("value", ObjectUtil.isEmpty(event.get("algorithmId")) ? "" : event.get("algorithmId").toString());
                    dataMap.put("grade", "1");
                    dataMap.put("isRotate", event.get("isRotate"));
                    dataMap.put("targetCount", ObjectUtil.isEmpty(event.get("targetCount")) ? "" : event.get("targetCount").toString());
                    dataMap.put("time", event.get("time"));
                    dataMap.put("picUrl", ObjectUtil.isEmpty(event.get("picUrl")) ? "" : event.get("picUrl").toString());
                    dataMap.put("videoUrl", "");

                    log.info("开始人脸搜索：{}", event);
                    String bindGroupId = event.get("bindGroupId").toString();
                    if (bindGroupId != null) {
                        FaceGroupDo faceGroupDo = faceGroupService.getById(bindGroupId);
                        if (faceGroupDo == null) {
                            log.error("人脸组不存在 {}, 请确认相机配置的人脸组", bindGroupId);
                            return;
                        }
                        String groupId = faceGroupDo.getGroupId();
                        String facePath = event.get("facePath").toString();
                        // 转base64
                        String base64 = Base64Utils.encodeToString(FileUtils.readFileToByteArray(new File(facePath)));
                        String userId = searchFace(groupId, base64);
                        if (StrUtil.isNotBlank(userId)) {
                            log.info("人脸搜索结果 userId:{}", userId);
                            // AlarmDetail alarmDetail = new AlarmDetail();
                            // alarmDetail.setId(event.get("alarmId"));
                            // alarmDetail.setFaceName(userId);
                            // alarmDetailService.updateById(alarmDetail);
                            dataMap.put("face_name", userId);
                            dataMap.put("face_path", "");
                        }
                    }
                    List<Map<String, Object>> data = Collections.singletonList(dataMap);
                    jsonResult.put("data", data);
                    MqttClientService.getInstance()
                            .getMqttClient("127.0.0.1")
                            .pub("alarm_result", jsonResult.toJSONString());
                }
            } catch (Exception e) {
                log.error("处理人脸搜索结果异常{}", e.getMessage(), e);
            }
        }, 0, 300, TimeUnit.MILLISECONDS); // 每次任务执行后延迟300毫秒执行一次
    }

    // 人脸搜索
    private String searchFace(String groupIdList, String imageBase64) throws Exception {
        String token = TokenUtils.getToken();
        if (StrUtil.isBlank(token)) {
            log.error("获取人脸识别token失败");
            return null;
        }
        String requestUrl = FACE_SEARCH_URL + "?access_token=" + token;
        JSONObject params = new JSONObject();
        params.put("group_id_list", groupIdList);
        params.put("image", imageBase64);
        params.put("image_type", "BASE64");
        params.put("max_user_num", 1);
        params.put("match_threshold", SysParameterUtil.getBaiduFaceScore());
        String responseStr = HttpClientUtil.requestPostJson(requestUrl, params);
        JSONObject responseObj = JSONObject.parseObject(responseStr);
        if (responseObj.getIntValue("error_code") == 0) {
            JSONObject searchResult = responseObj.getJSONObject("result");
            if (!searchResult.isEmpty()) {
                JSONArray jsonArray = searchResult.getJSONArray("user_list");
                if (!jsonArray.isEmpty()) {
                    JSONObject bdUser = jsonArray.getJSONObject(0);
                    log.info("人脸搜索结果：{}", bdUser);
                    return bdUser.getString("user_id");
                }
            }
        }
        return null;
    }
}
