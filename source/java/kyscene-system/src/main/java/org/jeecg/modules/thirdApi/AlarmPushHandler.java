package org.jeecg.modules.thirdApi;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.dataPush.DataPushVo;
import org.jeecg.modules.devwebsocket.DataPushWebSocket;
import org.jeecg.modules.system.util.CustomUtil;
import org.jeecg.modules.thirdApi.pojo.AlarmPushEntity;
import org.jeecg.modules.thirdApi.pojo.Constant;
import org.jeecg.modules.thirdApi.retry.AlarmRetryData;
import org.jeecg.modules.thirdApi.retry.service.AlarmRetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.jeecg.modules.dataPush.Constant.*;

/**
 * 报警信息推送
 */
@Component
@Slf4j
public class AlarmPushHandler {

    private final AlarmPushConf alarmPushConf;

    private final RedisUtil redisUtil;

    private AlarmRetryService alarmRetryService;

    private DataPushWebSocket dataPushWebSocket;

    @Autowired
    public void setDataPushWebSocket(DataPushWebSocket dataPushWebSocket) {
        this.dataPushWebSocket = dataPushWebSocket;
    }

    @Autowired
    public void setAlarmRetryService(AlarmRetryService alarmRetryService) {
        this.alarmRetryService = alarmRetryService;
    }

    @Autowired
    public AlarmPushHandler(AlarmPushConf alarmPushConf, RedisUtil redisUtil) {
        this.alarmPushConf = alarmPushConf;
        this.redisUtil = redisUtil;
    }

    /**
     * 推送数据
     */
    @Async
    public void pushData(AlarmPushEntity alarmPushEntity) {
        try {
            String pushUrl = alarmPushConf.getPushUrl();
            if (StrUtil.isBlank(pushUrl)) {
                log.error("告警推送地址为空");
                dataPushWebSocket.sendMessageToUser("数据推送失败，推送地址未设置");
                return;
            }
            DataPushVo dataPushVo = alarmPushConf.getDataPushVo();
            if (null == dataPushVo || StrUtil.isBlank(dataPushVo.getPushType())) {
                log.error("推送类型为空");
                dataPushWebSocket.sendMessageToUser("数据推送失败，推送类型未设置");
                return;
            }

            JSONObject data = CustomUtil.convertToJSONObject(alarmPushEntity);
            String groupNames = alarmPushEntity.getGroupNames();
            data.put("groupNames", StrUtil.split(groupNames, StrUtil.C_COMMA));

            String responseStr = null;
            String pushType = dataPushVo.getPushType();
            if (PUSH_TYPE_DIRECT.equals(pushType)) {
                responseStr = HttpClientUtil.requestPostJson(pushUrl, data);
            }

            if (PUSH_TYPE_TOKEN.equals(pushType)) {
                Result<String> tokenResult = getToken();
                if (!tokenResult.isSuccess()) {
                    dataPushWebSocket.sendMessageToUser(tokenResult.getMessage());
                    return;
                }
                String tokenType = alarmPushConf.getDataPushVo().getTokenType();
                if (TOKEN_TYPE_PARAM.equals(tokenType)) {
                    String paramName = alarmPushConf.getDataPushVo().getParamName();
                    if (StrUtil.isBlank(paramName)) {
                        log.error("token 传递的参数名未设置");
                        dataPushWebSocket.sendMessageToUser("数据推送失败，token 携带参数未设置");
                        return;
                    }
                    JSONObject targetData = new JSONObject();
                    targetData.putAll(data);
                    targetData.put(paramName, tokenResult.getResult());
                    //BeanUtil.copyProperties(data, targetData, CopyOptions.create().setIgnoreNullValue(false).ignoreError());
                    responseStr = HttpClientUtil.requestPostJson(pushUrl, targetData);
                }
                if (TOKEN_TYPE_HEADER.equals(tokenType)) {
                    String headerKey = alarmPushConf.getDataPushVo().getHeaderKey();
                    if (StrUtil.isBlank(headerKey)) {
                        log.error("携带token的请求头未设置");
                        dataPushWebSocket.sendMessageToUser("数据推送失败，携带token的请求头未设置");
                        return;
                    }
                    Map<String, String> headerMap = new HashMap<>();
                    headerMap.put(headerKey, tokenResult.getResult());
                    responseStr = HttpClientUtil.requestPostJsonWithHeader(pushUrl, data, headerMap);
                }
            }
            // 数据推送日志websocket
            dataPushWebSocket.sendMessageToUser("数据推送结果：" + responseStr);
            log.info("数据推送结果：{}", responseStr);
        } catch (Exception e) {
            // 推送失败，放入延迟队列
            //if (delayQueue.size() < 1000) delayQueue.put(new MyDelayed<>(alarmPushConf.getDelayTime(), data));
            log.error("告警数据推送失败，准备存入retry表, 异常信息：{}", e.getLocalizedMessage());
            // 推送失败，存库
            handlePushFailed(alarmPushEntity);
            dataPushWebSocket.sendMessageToUser("数据推送失败，内部异常：" + e.getMessage());
        }
    }

    /**
     * 推送失败流程
     */
    private void handlePushFailed(AlarmPushEntity alarmPushEntity) {
        AlarmRetryData alarmRetryData = new AlarmRetryData();
        CopyOptions copyOptions = CopyOptions.create()
                .ignoreNullValue()
                .setIgnoreProperties("alarmPictureBase64");
        BeanUtil.copyProperties(alarmPushEntity, alarmRetryData, copyOptions);
        alarmRetryService.saveData(alarmRetryData);
    }

    /**
     * 获取token
     */
    private Result<String> getToken() {
        Object o = redisUtil.get(Constant.CACHE_PUSH_TOKEN_KEY);
        if (ObjectUtil.isNotEmpty(o)) return Result.OK((String) o);
        Map<String, String> params = new HashMap<>();
        String username = alarmPushConf.getDataPushVo().getUsername();
        String password = alarmPushConf.getDataPushVo().getPassword();
        if (ObjectUtil.hasEmpty(username, password)) {
            log.error("获取token用户名或密码为空");
            return Result.error("获取token用户名或密码为空");
        }
        params.put("username", username);
        params.put("password", password);
        String tokenUrl = alarmPushConf.getDataPushVo().getTokenUrl();
        if (StrUtil.isBlank(tokenUrl)) {
            log.error("tokenUrl为空");
            return Result.error("tokenUrl为空");
        }
        String response;
        try {
            response = HttpClientUtil.requestPost(tokenUrl, params);
        } catch (Exception e) {
            log.error("请求获取token失败，{}", e.getMessage());
            return Result.error("请求获取token失败");
        }
        if (ObjectUtil.isEmpty(response)) return Result.error("请求获取token响应结果为空");
        JSONObject responseJson = JSONObject.parseObject(response);
        Boolean success = responseJson.getBoolean("success");
        if (success == null || !success) {
            log.error("token 接口返回信息：{}", responseJson.getString("msg"));
            return Result.error("token 接口返回信息：" + responseJson.getString("msg"));
        }
        JSONObject dataObj = responseJson.getJSONObject("data");
        if (ObjectUtil.isEmpty(dataObj)) return Result.error("token 接口返回信息：data为空");
        String token = dataObj.getString("token");
        if (token == null) return Result.error("token 获取失败, token为空");
        Long expire = dataObj.getLong("expire");
        // 单位 秒
        if (expire == null) expire = 30 * 60L;
        redisUtil.set(Constant.CACHE_PUSH_TOKEN_KEY, token, expire - 10);
        return Result.OK(token);
    }

}
