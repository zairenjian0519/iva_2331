/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.baidu;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/8/6
 */
@Slf4j
@Component
public class UserGroupService {

    // 组列表查询URL
    private static final String USER_GROUP_LIST_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/group/getlist";

    // 创建用户组
    private static final String USER_GROUP_ADD_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/group/add";

    // 组删除
    private static final String USER_GROUP_DELETE_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/group/delete";

    // 组内用户列表查询URL
    private static final String USER_GROUP_USER_LIST_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/group/getusers";

    // 删除用户组
    public Result<?> deleteUserGroup(String groupId) {
        if (StrUtil.isBlank(groupId)) {
            return Result.error("组ID为空");
        }
        String requestUrl = String.format("%s?access_token=%s", USER_GROUP_DELETE_URL, TokenUtils.getToken());
        JSONObject params = new JSONObject();
        params.put("group_id", groupId);
        try {
            String responseStr = HttpClientUtil.requestPostJson(requestUrl, params);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int code = responseObj.getIntValue("error_code");
            if (code != 0) return Result.error(responseObj.getString("error_msg"));
        } catch (Exception e) {
            return Result.error("删除人脸组失败");
        }
        return Result.OK();
    }

    // 创建用户组
    public void createUserGroup(String groupId) throws Exception {
        boolean exist = checkGroupExist(groupId);
        if (exist) return;
        // 组不存在则创建
        createUserGroupById(groupId);
    }

    // 获取用户组列表
    public Result<JSONObject> getUserGroupList(String groupId, int pageNo, int pageSize) {
        if (StrUtil.isBlank(groupId)) {
            return Result.error("组ID为空");
        }

        JSONObject params = new JSONObject();
        params.put("group_id", groupId);
        params.put("start", pageNo);
        params.put("length", pageSize == 0 ? 100 : pageSize);
        String requestUrl = String.format("%s?access_token=%s", USER_GROUP_USER_LIST_URL, TokenUtils.getToken());

        try {
            String responseStr = HttpClientUtil.requestPostJson(requestUrl, params);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int code = responseObj.getIntValue("error_code");
            if (code != 0) return Result.error(responseObj.getString("error_msg"));
            return Result.ok(responseObj.getJSONObject("result")); // { "user_id_list": [ "uid1","uid2" ] }
        } catch (Exception e) {
            log.error("获取用户组列表失败, {}", e.getMessage());
            return Result.error("获取人员列表失败");
        }
    }


    private void createUserGroupById(String groupId) throws Exception {
        JSONObject params = new JSONObject();
        params.put("group_id", groupId);
        String token = TokenUtils.getToken();
        if (StrUtil.isBlank(token)) throw new JeecgBootException("获取token失败");
        String requestUrl = String.format("%s?access_token=%s", USER_GROUP_ADD_URL, token);
        String responseStr = HttpClientUtil.requestPostJson(requestUrl, params);
        JSONObject responseObj = JSONObject.parseObject(responseStr);
        int code = responseObj.getIntValue("error_code");
        if (code != 0) throw new JeecgBootException(responseObj.getString("error_msg"));
    }

    private boolean checkGroupExist(String groupId) throws Exception {
        String token = TokenUtils.getToken();
        if (token == null) throw new JeecgBootException("获取token失败");
        String requestUrl = String.format("%s?access_token=%s", USER_GROUP_LIST_URL, token);
        String responseStr = HttpClientUtil.requestPostJson(requestUrl, null);
        JSONObject responseObj = JSONObject.parseObject(responseStr);
        int code = responseObj.getIntValue("error_code");
        if (code != 0) throw new JeecgBootException(responseObj.getString("error_msg"));
        JSONArray jsonArray = responseObj.getJSONObject("result").getJSONArray("group_id_list");
        for (int i = 0; i < jsonArray.size(); i++) {
            if (jsonArray.getString(i).equals(groupId)) {
                return true;
            }
        }
        return false;
    }
}
