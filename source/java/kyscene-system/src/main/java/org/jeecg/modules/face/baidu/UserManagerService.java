/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/8/6
 */
@Slf4j
@Component
public class UserManagerService {

    // 人脸注册地址
    private static final String USER_CREATE_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/add";

    // 删除用户地址
    private static final String USER_DELETE_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/delete";

    // 人脸删除地址
    private static final String FACE_DELETE_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/face/delete";


    // 人脸注册
    public Result<?> createUser(String groupId, String userId, String imageBase64) {
        String token = TokenUtils.getToken();
        if (StrUtil.isBlank(token)) {
            return Result.error("获取人脸识别token失败");
        }
        String requestUrl = USER_CREATE_URL + "?access_token=" + token;
        JSONObject params = new JSONObject();
        params.put("image", imageBase64);
        params.put("image_type", "BASE64");
        params.put("group_id", groupId);
        params.put("user_id", userId);
        try {
            String responseStr = HttpClientUtil.requestPostJson(requestUrl, params);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int code = responseObj.getIntValue("error_code");
            if (code != 0) return Result.error(responseObj.getString("error_msg"));
            JSONObject addResult = responseObj.getJSONObject("result");
            JSONObject result = new JSONObject();
            result.put("face_token", addResult.getString("face_token"));
            result.put("log_id", responseObj.getString("log_id"));
            return Result.ok(result);
        } catch (Exception e) {
            log.error("人脸注册失败, {}", e.getMessage());
            return Result.error("人脸注册失败");
        }
    }

    // 删除用户
    public Result<?> deleteUser(String groupId, String userId) {
        String token = TokenUtils.getToken();
        if (StrUtil.isBlank(token)) {
            return Result.error("获取人脸识别token失败");
        }
        String requestUrl = USER_DELETE_URL + "?access_token=" + token;
        JSONObject params = new JSONObject();
        params.put("group_id", groupId);
        params.put("user_id", userId);
        try {
            String responseStr = HttpClientUtil.requestPostJson(requestUrl, params);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int code = responseObj.getIntValue("error_code");
            if (code != 0) return Result.error(responseObj.getString("error_msg"));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
        return Result.OK();
    }

    // 删除人脸
    public Result<?> deleteFace(String groupId, String userId, String faceToken, String logId) {
        String token = TokenUtils.getToken();
        if (StrUtil.isBlank(token)) {
            return Result.error("获取cloud face token失败");
        }
        if (StrUtil.hasBlank(groupId, userId, faceToken, logId)) {
            return Result.error("删除cloud face 必要参数不能为空");
        }

        String requestUrl = FACE_DELETE_URL + "?access_token=" + token;
        JSONObject params = new JSONObject();
        params.put("user_id", userId);
        params.put("group_id", groupId);
        params.put("log_id", logId);
        params.put("face_token", faceToken);
        try {
            String responseStr = HttpClientUtil.requestPostJson(requestUrl, params);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int code = responseObj.getIntValue("error_code");
            if (code != 0) return Result.error(responseObj.getString("error_msg"));
        } catch (Exception e) {
            log.error("删除人脸失败, {}", e.getMessage());
            return Result.error("删除人脸失败");
        }
        return Result.OK();
    }

}
