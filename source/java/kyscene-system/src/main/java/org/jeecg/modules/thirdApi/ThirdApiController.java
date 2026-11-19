package org.jeecg.modules.thirdApi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.vo.DictModel;
import org.jeecg.modules.alarmAudit.entity.AlarmAudit;
import org.jeecg.modules.alarmAudit.service.IAlarmAuditService;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.devwebsocket.VieWebSocket;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.system.service.ISysDictService;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.pojo.WebSocketmsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Elmer on 2022/10/24.
 */
@Slf4j
@RestController
@RequestMapping("/thirdApi")
public class ThirdApiController {

    @Autowired
    IAlarmAuditService auditService;

    @Autowired
    ICameraManagerService cameraManagerService;

    @Autowired
    IDeviceManagementService deviceManagementService;

    @Autowired
    IKyCategoryService categoryService;

    @Autowired
    ISysDictService dictService;

    @Autowired
    private VieWebSocket viWebSocket;

    @Autowired
    private CameraStreamHandler cameraStreamHandler;

    @PostMapping("/pushAlarm")
    public JSONObject testPush(@RequestBody JSONObject params) {
        params.put("alarmPictureBase64", "base64");
        log.info("接收到告警推送消息：" + params);
        JSONObject result = new JSONObject();
        //result.put("resultType", 5);
        result.put("code", 200);
        result.put("success", true);
        result.put("msg", "success");
        result.put("data", null);
        return result;
    }

    @RequestMapping(value = "/vieWebsocketSendMsg", method = RequestMethod.POST)
    public Object sendMessage(@RequestBody WebSocketmsg webSocketmsg) {
        String msg = webSocketmsg.getMsg();
        String cameraId = webSocketmsg.getCamera_id();
        int typeId = webSocketmsg.getType_id();
        log.info("发送的消息内容" + webSocketmsg);
        JSONObject jsonMsg = new JSONObject();
        jsonMsg.put("camera_id", cameraId);
        jsonMsg.put("msg", msg);
        jsonMsg.put("type_id", typeId);
        if (msg != null && "" != msg) {
            log.info("收到发送的消息 : " + msg);
            viWebSocket.sendMessageToUser(jsonMsg.toJSONString());
        }
        JSONObject data = new JSONObject();
        data.put("code", 2000);
        data.put("msg", "发送成功");
        log.info("jsonString : " + data);
        return data;
    }

    @RequestMapping("/auditResult")
    public JSONObject saveAuditResult(HttpServletRequest request) {
        try {
            request.setCharacterEncoding("utf-8");
            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
            StringBuilder result = new StringBuilder();
            String jsonStr;
            try {
                while ((jsonStr = reader.readLine()) != null) {
                    result.append(jsonStr);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
                return fail(e.getMessage());
            }
            reader.close();
            JSONObject reqParsers = JSONObject.parseObject(result.toString());
            String alarm_id = reqParsers.getString("alarm_id");
            if (null != alarm_id) {
                AlarmAudit auditEntity = auditService.getOne(new QueryWrapper<AlarmAudit>().lambda().eq(AlarmAudit::getAlarmId, alarm_id));
                if (null != auditEntity) {
                    auditEntity.setAuditTime(new Date());
                    auditEntity.setAuditStatus("1");
                    auditEntity.setAuditResult(reqParsers.getString("audit_result"));
                    auditEntity.setAuditReason(reqParsers.getString("audit_reason"));
                    auditEntity.setAuditPeople(reqParsers.getString("audit_people"));
                    auditService.saveOrUpdate(auditEntity);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return fail(e.getMessage());
        }
        JSONObject result = new JSONObject();
        result.put("deviceSn", SysParameterUtil.getDeviceSn());
        return success(result);
    }

    @RequestMapping("/getCameraList")
    public JSONObject getCameraList(HttpServletRequest request) {
        JSONArray array = new JSONArray();
        try {
            List<CameraManager> cameraList = cameraManagerService.list();
            for (CameraManager camera : cameraList) {
                JSONObject json = new JSONObject();
                DeviceManagement dev = deviceManagementService.getByCameraId(camera.getId());
                json.put("cameraId", camera.getId());
                json.put("cameraIp", camera.getIp());
                json.put("cameraMfr", camera.getFactory());
                json.put("cameraName", camera.getCameraName());
                json.put("cameraRtsp", camera.getStreamAddress());
                json.put("cameraSubType", camera.getStreamType());
                json.put("cameraStatus", camera.getCameraStatus());
                List<KyCategory> kyCategories = categoryService.getTreeLocationById(camera.getLocation());
                List<String> collect = kyCategories.stream().map(KyCategory::getName).collect(Collectors.toList());
                json.put("groupNames", collect);
                json.put("deviceIp", dev.getDeviceIp());
                array.add(json);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return fail(e.getMessage());
        }
        JSONObject result = new JSONObject();
        result.put("deviceSn", SysParameterUtil.getDeviceSn());
        result.put("payload", array);
        return success(result);
    }

    @RequestMapping("/getAlarmDictList")
    public JSONObject getAlarmList(HttpServletRequest request) {
        JSONArray array = new JSONArray();
        try {
            List<KyCategory> lists = categoryService.list(new QueryWrapper<KyCategory>().lambda().eq(KyCategory::getCategory, "0"));
            for (KyCategory category : lists) {
                JSONObject json = new JSONObject();
                json.put("code", category.getCode());
                json.put("name", category.getName());
                array.add(json);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return fail(e.getMessage());
        }
        JSONObject result = new JSONObject();
        result.put("payload", array);
        result.put("deviceSn", SysParameterUtil.getDeviceSn());
        return success(result);
    }

    @RequestMapping("/getAlarmLevelDict")
    public JSONObject getAlarmLevel(HttpServletRequest request) {
        JSONArray array = new JSONArray();
        try {
            List<DictModel> lists = dictService.getDictItems("alarm_level");
            for (DictModel dict : lists) {
                JSONObject json = new JSONObject();
                json.put("code", dict.getValue());
                json.put("name", dict.getText());
                array.add(json);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return fail(e.getMessage());
        }
        JSONObject result = new JSONObject();
        result.put("payload", array);
        result.put("deviceSn", SysParameterUtil.getDeviceSn());
        return success(result);
    }

    @RequestMapping("/getAlgorithmByCamera")
    public JSONObject getAlgorithmByCamera(HttpServletRequest request) {
        JSONArray array = new JSONArray();
        try {
            List<CameraManager> lists = cameraManagerService.getAlgorithm();
            for (CameraManager cameraManager : lists) {
                JSONObject json = new JSONObject();
                json.put("cameraId", cameraManager.getId());
                json.put("cameraName", cameraManager.getCameraName());
                json.put("alarmType", cameraManager.getAlarmType());
                json.put("alarmTypeName", cameraManager.getCategorys());
                array.add(json);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return fail(e.getMessage());
        }
        JSONObject result = new JSONObject();
        result.put("payload", array);
        result.put("deviceSn", SysParameterUtil.getDeviceSn());
        return success(result);
    }

    /**
     * 开启视频流
     */
    @PostMapping("/startCameraStream")
    public JSONObject startCameraStream(@RequestBody JSONObject params) {
        String deviceIp = params.getString("deviceIp");
        if (StrUtil.isEmpty(deviceIp)) {
            return fail("deviceIp不能为空");
        }

        String cameraId = params.getString("cameraId");
        if (StrUtil.isEmpty(cameraId)) {
            return fail("cameraId不能为空");
        }

        // 先组装webrtc流地址
        Result<?> result1 = cameraStreamHandler.getWebRtcUrl(deviceIp, cameraId);
        if (ObjectUtil.notEqual(result1.getCode(), CommonConstant.SC_OK_200)) {
            return fail(result1.getMessage());
        }

        JSONObject temp = new JSONObject();
        temp.put("payload", result1.getResult());
        temp.put("deviceSn", SysParameterUtil.getDeviceSn());

        // 不存在，开启视频流
        Result<?> result = cameraStreamHandler.startCameraStream(params);
        if (ObjectUtil.notEqual(result.getCode(), CommonConstant.SC_OK_200)) return fail(result.getMessage());
        return success(temp);
    }


    /**
     * 关闭视频流
     */
    @PostMapping("/stopCameraStream")
    public JSONObject closeCameraStream(@RequestBody JSONObject params) {
        Result<?> ret = cameraStreamHandler.stopCameraStream(params);
        if (ObjectUtil.notEqual(ret.getCode(), CommonConstant.SC_OK_200)) return fail(ret.getMessage());
        JSONObject tempRes = new JSONObject();
        tempRes.put("deviceSn", SysParameterUtil.getDeviceSn());
        return success(tempRes);
    }

    /**
     * 关闭所有视频流
     */
    @PostMapping("/closeAllStream")
    public JSONObject closeAllStream() {
        Result<?> result = cameraStreamHandler.closeAllStream();
        if (ObjectUtil.notEqual(result.getCode(), CommonConstant.SC_OK_200)) return fail(result.getMessage());
        JSONObject resultData = new JSONObject();
        resultData.put("payload", null);
        resultData.put("deviceSn", SysParameterUtil.getDeviceSn());
        JSONObject success = success(resultData);
        success.put("message", result.getMessage());
        return success;
    }

    /**
     * 获取超脑信息
     */
    @GetMapping("/deviceInfo")
    public JSONObject getDevInfo(@RequestParam String deviceIp) {
        JSONObject result = new JSONObject();
        result.put("payload", deviceManagementService.getDeviceInfo(deviceIp));
        result.put("deviceSn", SysParameterUtil.getDeviceSn());
        return success(result);
    }


    private JSONObject success(Object data) {
        JSONObject ret = new JSONObject();
        ret.put("success", true);
        ret.put("code", 200);
        ret.put("data", data);
        ret.put("message", "请求成功");
        return ret;
    }

    private JSONObject fail(String msg) {
        JSONObject ret = new JSONObject();
        ret.put("success", false);
        ret.put("code", 500);
        ret.put("data", null);
        ret.put("message", msg);
        return ret;
    }

}
