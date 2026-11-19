/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.remoteSync;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecg.modules.face.entity.FaceRemoteUpdateMsg;
import org.jeecg.modules.face.service.FaceDetailService;
import org.jeecg.modules.face.service.FaceGroupService;
import org.jeecg.modules.face.service.FaceRemoteUpdateMsgService;
import org.jeecg.modules.mqtt.job.DeviceHeart;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author： DecoderX
 * @date： 2025/5/22
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RemoteSyncFaceData {

    private final ApiProperties apiProperties;

    private final RedisUtil redisUtil;

    private final FaceDetailService faceDetailService;

    private final FaceGroupService faceGroupService;

    private final GlobalSyncStateCache globalSyncStateCache;

    private final FaceRemoteUpdateMsgService faceRemoteUpdateMsgService;

    // 全量同步
    @Async("kylandTaskExecutor")
    public void syncAllFace() {
        log.info("开始同步人脸数据, {} 开始执行...", "SyncAllFace");
        globalSyncStateCache.getSyncState().setGlobalState(true);

        try {
            // 全量同步，先清楚库里面的所有数据
            faceDetailService.clearAllData();

            // 获取通用请求参数
            Map<String, String> headers = buildHeaders();
            Map<String, String> params = buildParams();
            if (headers == null || params == null) return;

            int pageNum = 1;
            int pageSize = 20;
            int total = 0;
            String feature;

            do {
                params.put("pageNum", String.valueOf(pageNum));
                params.put("pageSize", String.valueOf(pageSize));

                JSONObject dataObj = requestApi(apiProperties.getAllUrl(), params, headers);
                if (dataObj == null) return;

                // 第一次设置分页信息
                if (pageNum == 1) {
                    total = dataObj.getIntValue("total");
                    pageSize = dataObj.getIntValue("pageSize");
                }

                JSONArray records = dataObj.getJSONArray("records");
                if (ObjectUtil.isEmpty(records)) {
                    log.error("全量同步 records 为空");
                    return;
                }

                faceDetailService.batchImportFromRemote(records);
                feature = dataObj.getString("feature");
                pageNum++;
            } while ((pageNum - 1) * pageSize < total);

            // 保存功能码
            saveFeature(feature, params.get("projectKey"));

            // 初始化人脸库 - 本地
            // if (!SysParameterUtil.isBaiduFaceEnabled()) {
            //     faceGroupService.initDefaultGroup();
            // }
        } catch (Exception e) {
            log.error("全量同步异常", e);
        } finally {
            globalSyncStateCache.getSyncState().setGlobalState(false);
        }
    }

    // 差分同步
    @Async("kylandTaskExecutor")
    public void syncDiffFace() {
        log.info("开始同步人脸数据, {} 开始执行...", "SyncDiffFace");
        globalSyncStateCache.getSyncState().setGlobalState(true);

        try {
            Map<String, String> headers = buildHeaders();
            Map<String, String> params = buildParams();
            if (headers == null || params == null) return;

            JSONObject dataObj = requestApi(apiProperties.getDifferentUrl(), params, headers);
            if (dataObj == null) return;

            JSONArray removeList = dataObj.getJSONArray("removeList");
            if (ObjectUtil.isNotEmpty(removeList)) {
                List<String> needToRMList = removeList.toJavaList(String.class);
                faceDetailService.batchDeleteFaceDetail(String.join(StrUtil.COMMA, needToRMList));
            }

            JSONArray addList = dataObj.getJSONArray("addList");
            if (ObjectUtil.isNotEmpty(addList)) {
                faceDetailService.batchImportFromRemote(addList);
            }

            saveFeature(dataObj.getString("feature"), params.get("projectKey"));

            FaceGroupDo faceGroupDo = faceGroupService.getOrCreateDefaultGroup();
            if (SysParameterUtil.isBaiduFaceEnabled()) {
                if (faceGroupDo == null) {
                    log.error("默认人员组不存在");
                    return;
                }
                faceDetailService.syncBaiduYun(faceGroupDo.getGroupId());
            }

            DeviceHeart.sendHeartbeat();
        } catch (Exception e) {
            log.error("差分同步异常: {}", e.getMessage(), e);
        } finally {
            globalSyncStateCache.getSyncState().setGlobalState(false);
        }
    }

    private Map<String, String> buildHeaders() {
        String token = getToken();
        if (StrUtil.isBlank(token)) {
            log.error("token 为空");
            return null;
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        return headers;
    }

    private Map<String, String> buildParams() {
        String projectCode = SysParameterUtil.getProjectCode();
        if (StrUtil.isBlank(projectCode)) {
            log.error("项目编码为空");
            return null;
        }
        Map<String, String> params = new HashMap<>();
        params.put("projectKey", projectCode);
        return params;
    }

    private JSONObject requestApi(String url, Map<String, String> params, Map<String, String> headers) throws Exception {
        String responseStr = HttpClientUtil.requestGet(url, params, headers);
        if (ObjectUtil.isEmpty(responseStr)) {
            log.error("请求 {} 结果为空", url);
            return null;
        }

        JSONObject responseJson = JSONObject.parseObject(responseStr);
        if (!Integer.valueOf(200).equals(responseJson.getInteger("code"))) {
            log.error("请求 {} 失败: {}", url, responseJson.getString("msg"));
            return null;
        }

        JSONObject dataObj = responseJson.getJSONObject("data");
        if (ObjectUtil.isEmpty(dataObj)) {
            log.error("请求 {} data 为空", url);
            return null;
        }
        return dataObj;
    }

    private void saveFeature(String feature, String projectKey) {
        FaceRemoteUpdateMsg remoteUpdateMsg = new FaceRemoteUpdateMsg();
        remoteUpdateMsg.setFeature(StrUtil.blankToDefault(feature, ""));
        remoteUpdateMsg.setProjectKey(projectKey);
        remoteUpdateMsg.setUpdateTime(new Date());
        faceRemoteUpdateMsgService.saveData(remoteUpdateMsg);
    }

    /**
     * 获取token
     */
    private String getToken() {
        // 先从redis中获取
        Object o = redisUtil.get(Const.CJZK_TOKEN_KEY);
        if (ObjectUtil.isNotEmpty(o)) return (String) o;

        // projectKey
        String projectCode = SysParameterUtil.getProjectCode();
        if (StrUtil.isBlank(projectCode)) {
            log.error("项目编码为空");
            return null;
        }

        String tokenUrl = apiProperties.getTokenUrl();
        String appKey = apiProperties.getAppKey();
        String appSecret = apiProperties.getAppSecret();
        if (ObjectUtil.hasEmpty(tokenUrl, appKey, appSecret)) {
            log.error("tokenUrl、appKey、appSecret不能为空");
            return null;
        }
        try {
            JSONObject params = new JSONObject();
            params.put("appKey", appKey);
            params.put("appSecret", appSecret);
            params.put("projectKey", projectCode);
            String response = HttpClientUtil.requestPostJson(tokenUrl, params);
            if (ObjectUtil.isEmpty(response)) {
                log.error("请求获取token失败, 返回结果为空");
                return null;
            }
            JSONObject responseJson = JSONObject.parseObject(response);
            Integer responseCode = responseJson.getInteger("code");
            if (responseCode == null || responseCode != 200) {
                log.error("token 接口返回信息：{}", responseJson.getString("msg"));
                return null;
            }
            JSONObject dataObj = responseJson.getJSONObject("data");
            if (ObjectUtil.isEmpty(dataObj)) {
                log.error("token 获取失败, data为空");
                return null;
            }
            String token = dataObj.getString("access_token");
            if (token == null) {
                log.error("token 获取失败, access_token为空");
                return null;
            }
            Long expiresIn = dataObj.getLong("expires_in");
            if (expiresIn == null) expiresIn = 720L;
            redisUtil.set(Const.CJZK_TOKEN_KEY, token, expiresIn * 60 - 10);
            return token;
        } catch (Exception e) {
            log.error("token 接口异常：{}", e.getMessage());
        }
        return null;
    }
}
