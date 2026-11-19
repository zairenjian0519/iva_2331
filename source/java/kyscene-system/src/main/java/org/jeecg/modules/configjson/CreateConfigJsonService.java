package org.jeecg.modules.configjson;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.algorithmmanager.service.IAlgorithmManagerService;
import org.jeecg.modules.cameraGroup.entity.CameraGroup;
import org.jeecg.modules.cameraGroup.service.ICameraGroupService;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.event.CameraEventMsg;
import org.jeecg.modules.cameraManager.event.CameraEventOptions;
import org.jeecg.modules.cameraManager.event.CameraEventPublisher;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecg.modules.face.service.FaceGroupService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.system.util.CmdUti;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateConfigJsonService {

    @Value("${face.base-face-path}")
    private String baseFacePath;

    private final ICameraManagerService cameraManagerService;

    private final IAlgorithmManagerService algorithmManagerService;

    private final IKyCategoryService categoryService;

    private final IDeviceManagementService deviceManagementService;

    private final ICameraGroupService cameraGroupService;

    private final RedisUtil redisUtil;

    private final CameraEventPublisher cameraEventPublisher;

    private final FaceGroupService faceGroupService;

    // 利用mqtt远程同步摄像机配置
    public Result<String> saveCameraConfig(String deviceId, String ip) {
        JSONArray cameraAry = createCameraJson(deviceId);
        redisUtil.set(Const.CAMERA_LIST_INFO_KEY, JSON.toJSONString(cameraAry));

        // 发布相机状态事件
        // DeviceManagement deviceManagement = deviceManagementService.getById(deviceId);
        // asyncCameraStatus(deviceManagement);
        return Result.ok("生成相机配置成功");
    }

    // 利用mqtt远程同步算法识别任务
    public Result<String> updateAiServerDetectConfig(String deviceId, String ip) {
        JSONArray ary = createAlgJson(deviceId);
        redisUtil.set(Const.ALGORITHM_LIST_INFO_KEY, JSON.toJSONString(ary));
        return Result.ok("生成算法配置成功");
    }

    // 根据相机id生成相机配置文件和识别任务配置文件
    public Result<String> updateAiServerConfigByCameraId(String cameraId) {
        DeviceManagement deviceManagement = deviceManagementService.getByCameraId(cameraId);
        if (null != deviceManagement) {
            Result<String> result = sendMqttConfig(deviceManagement, "algorithm_config");
            if (result.getCode() == 200 && CmdUti.restartAiEngine() != 0) {
                return Result.error("安排AI引擎重启失败");
            }
            return result;
        }
        return Result.error("未找到相机相关信息");
    }

    // 根据设备id生成相机配置文件和识别任务配置文件
    public Result<String> updateAiServerConfigByDeviceId(String deviceId) {
        DeviceManagement deviceManagement = deviceManagementService.getById(deviceId);
        if (null != deviceManagement) {
            return sendMqttConfig(deviceManagement);
        }
        return Result.error("未找到设备相关信息");
    }

    private Result<String> sendMqttConfig(DeviceManagement deviceManagement) {
        return sendMqttConfig(deviceManagement, "normal_job");
    }

    private Result<String> sendMqttConfig(DeviceManagement deviceManagement, String eventType) {
        Result<String> sysRet = saveCameraConfig(deviceManagement.getId(), deviceManagement.getDeviceIp());
        if (sysRet.getCode() == 200) {
            sysRet = updateAiServerDetectConfig(deviceManagement.getId(), deviceManagement.getDeviceIp());
        }
        // 发布相机状态事件
        asyncCameraStatus(deviceManagement, eventType);
        return sysRet;
    }

    public Result<String> saveGroupAll() {
        List<DeviceManagement> deviceList = deviceManagementService.list();
        for (DeviceManagement device : deviceList) {
            sendMqttGroupConfig(device);
        }
        return Result.ok("生成配置文件成功");
    }

    private void sendMqttGroupConfig(DeviceManagement deviceManagement) {
        saveGroupAlgConfig(deviceManagement.getId(), deviceManagement.getDeviceIp());
    }

    public Result<String> saveGroupAlgConfig(String deviceId, String ip) {
        JSONObject object = createGroupAlgJson(deviceId);
        redisUtil.set(Const.GROUP_ALGORITHM_LIST_INFO_KEY, JSON.toJSONString(object));
        return Result.ok("生成算法配置成功");
    }

    // 生成系统配置文件
    public Result<String> updateAiServerSystemConfig(String deviceId, String ip) {
        try {
            JSONArray sysAry = new JSONArray();
            JSONObject sysObj = new JSONObject();
            PropertiesUtil util = new PropertiesUtil("sysConfig.properties");
            sysObj.put("grab_file_path", util.getStringProperty("grab_file_path")); //采集图像时 图像保存路径
            sysObj.put("config_file_path", util.getStringProperty("config_file_path")); //web生成的配置文件保存路径
            sysObj.put("alarm_file_path", util.getStringProperty("alarm_file_path")); //告警图片存放路径
            sysObj.put("local_ip", ip); //本机ip
            sysObj.put("local_port", util.getStringProperty("local_port")); //本机端口
            sysObj.put("mqtt_ip", util.getStringProperty("mqtt_ip")); // mqtt方式对外交互数据,设备连接的mqtt IP
            sysObj.put("mqtt_port", util.getStringProperty("mqtt_port")); //mqtt方式对外交互数据,设备连接的mqtt端口
            sysObj.put("http_url", util.getStringProperty("http_url")); //http方式对外交互数据,http服务器的url
            sysObj.put("model_path", util.getStringProperty("model_path")); //算法模型路径
            sysObj.put("model_name", util.getStringProperty("model_name")); //算法模型名称
            sysObj.put("height_name", util.getStringProperty("height_name")); //算法权重名称
            sysObj.put("class_name", util.getStringProperty("class_name")); //算法分类名称
            sysObj.put("business_system_path", util.getStringProperty("business_system_path")); //业务系统路径
            sysObj.put("business_system_name", util.getStringProperty("business_system_name")); //业务系统名称
            sysObj.put("test_file_path", util.getStringProperty("test_file_path")); //测试路径
            sysAry.add(sysObj);
            redisUtil.set(Const.SYSTEM_LIST_INFO_KEY, JSON.toJSONString(sysAry));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
        return Result.ok("生成系统配置成功");
    }

    // 生成相机配置文件json数据
    private JSONArray createCameraJson(String deviceId) {
        JSONArray cameraAry = new JSONArray();
        List<CameraManager> cameraList = cameraManagerService.getListByDeviceId(deviceId);

        for (CameraManager camera : cameraList) {
            // 离线相机直接跳过
            if (StrUtil.equals(camera.getCameraStatus(), "4")) continue;
            cameraAry.add(createCameraObj(camera));
        }
        return cameraAry;
    }

    private JSONObject createCameraObj(CameraManager camera) {
        JSONObject cameraObj = new JSONObject();

        // 使用默认值方法简化空值处理
        cameraObj.put("camera_id", getValueOrDefault(camera.getId(), ""));
        cameraObj.put("camera_name", getValueOrDefault(camera.getCameraName(), "0000"));
        cameraObj.put("camera_duty", getValueOrDefault(camera.getCameraName(), "0000"));
        cameraObj.put("username", getValueOrDefault(camera.getUsername(), "默认"));
        cameraObj.put("password", getValueOrDefault(camera.getPassword(), "0000"));
        cameraObj.put("ip", getValueOrDefault(camera.getIp(), ""));
        cameraObj.put("port", getValueOrDefault(camera.getPort(), "0000"));
        cameraObj.put("rtsp_url", getValueOrDefault(camera.getStreamAddress(), ""));
        cameraObj.put("sub_type", getValueOrDefault(camera.getStreamType(), ""));
        cameraObj.put("push_url", getValueOrDefault(camera.getStreamAddressPush(), ""));
        cameraObj.put("grab_switch", "0");
        cameraObj.put("task_state", "1");
        cameraObj.put("camera_number", getValueOrDefault(camera.getCameraNumber(), ""));
        cameraObj.put("factory", getValueOrDefault(camera.getFactory(), ""));
        return cameraObj;
    }

    // 辅助方法：提供默认值
    private <T> T getValueOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    // 生成识别任务配置文件json数据
    private JSONArray createAlgJson(String deviceId) {
        // specialAlgAry 比如 跑冒滴漏 皮带松动
        JSONArray specialAlgAry = new JSONArray();
        // specialCameraAry 比如 跑冒滴漏 皮带送到
        List<CameraManager> specialCameraList = new ArrayList<>();

        JSONArray ary = new JSONArray();
        // 批量获取设备相关的所有摄像头信息
        List<CameraManager> cameraList = cameraManagerService.getListByDeviceId(deviceId);
        if (cameraList.isEmpty()) return ary;

        // 提取所有摄像头 ID，用于批量查询算法和位置信息
        List<String> cameraIds = cameraList.stream().map(CameraManager::getId).toList();

        // 批量获取所有算法信息（过滤 isRotate 不等于 1 的算法）
        Map<String, List<AlgorithmManager>> algMap = algorithmManagerService.getListByCameraIds(cameraIds).stream()
                .filter(alg -> !alg.getIsRotate().equals(1))
                .collect(Collectors.groupingBy(AlgorithmManager::getBelongTo));

        // 批量获取所有位置信息
        List<String> locationIds = cameraList.stream()
                .map(CameraManager::getLocation)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, KyCategory> locationMap = categoryService.listByIds(locationIds).stream()
                .collect(Collectors.toMap(KyCategory::getId, Function.identity()));

        // 构建 JSON 数据
        for (CameraManager camera : cameraList) {
            // 离线相机直接跳过
            if (StrUtil.equals(camera.getCameraStatus(), "4")) continue;

            // 两个 cameraObj，互不影响
            JSONObject normalCameraObj = new JSONObject();
            JSONObject specialCameraObj = new JSONObject();

            JSONArray algAry = new JSONArray();
            // 特殊算法不流入通用算法集合
            JSONArray specialAlgObjAry = new JSONArray();

            // 获取当前摄像头的算法列表
            List<AlgorithmManager> algList = algMap.getOrDefault(camera.getId(), Collections.emptyList());

            for (AlgorithmManager alg : algList) {
                JSONObject algObj = new JSONObject();
                JSONObject validTime = parseValidTime(alg.getCheckTime());

                // 获取算法对应的类别信息
                KyCategory kyCategory = categoryService.getById(alg.getModelName());
                algObj.put("operator", kyCategory != null ? new int[]{Integer.parseInt(kyCategory.getCode())} : alg.getModelName());
                algObj.put("valid_time", validTime);
                algObj.put("camera_roi", JSON.parseArray(alg.getCheckArea()));
                algObj.put("invalid_roi", JSON.parseArray(alg.getInvalidArea()));
                algObj.put("alarm_type", "0");
                algObj.put("hit_count", String.valueOf(alg.getHitCount()));
                algObj.put("alarm_period", String.valueOf(alg.getTimeSpace()));
                algObj.put("alarm_Interval_period", String.valueOf(alg.getAlarmCycle()));
                algObj.put("false_detect_period", String.valueOf(alg.getSensitivity()));
                algObj.put("alarm_grade", alg.getAlarmLevel());
                algObj.put("algorithm_threshold", String.valueOf(alg.getThreshold()));
                algObj.put("target_threshold", String.valueOf(alg.getTargetThreshold()));
                algObj.put("iou_threshold", String.valueOf(alg.getIouThreshold()));

                // 特殊算法单独放
                if (kyCategory != null && StrUtil.equalsAny(kyCategory.getName(), "跑冒滴漏", "皮带松动", "入侵检测")) {
                    algObj.put("frame_interval", alg.getFrameInterval());
                    // 添加到specialAlgAry
                    specialAlgObjAry.add(algObj);
                } else {
                    algAry.add(algObj);
                }

            }

            // ===== 填充摄像头基础信息（两份都要）=====
            fillCameraBaseInfo(normalCameraObj, camera, locationMap);
            fillCameraBaseInfo(specialCameraObj, camera, locationMap);

            // ===== 普通算法 =====
            if (!algAry.isEmpty()) {
                normalCameraObj.put("algorithm_cfg", algAry);
                ary.add(normalCameraObj);
            }

            // ===== 特殊算法 =====
            if (!specialAlgObjAry.isEmpty()) {
                specialCameraObj.put("algorithm_cfg", specialAlgObjAry);
                specialAlgAry.add(specialCameraObj);
                specialCameraList.add(camera);
            }
        }

        redisUtil.set(Const.ALGORITHM_LIST_INFO_KEY + "_special", JSON.toJSONString(specialAlgAry));

        JSONArray specialCameraAry = new JSONArray();
        for (CameraManager camera : specialCameraList) {
            specialCameraAry.add(createCameraObj(camera));
        }

        redisUtil.set(Const.CAMERA_LIST_INFO_KEY + "_special", JSON.toJSONString(specialCameraAry));
        return ary;
    }

    private void fillCameraBaseInfo(JSONObject cameraObj,
                                    CameraManager camera,
                                    Map<String, KyCategory> locationMap) {

        cameraObj.put("camera_id", camera.getId());
        cameraObj.put("camera_name", camera.getCameraName());

        KyCategory location = locationMap.get(camera.getLocation());
        cameraObj.put("camera_duty",
                location != null ? location.getName() : camera.getLocation());

        cameraObj.put("stream_type", camera.getStreamType());

        cameraObj.put("face_group_path", "");
        if (StrUtil.isNotEmpty(camera.getFaceGroupId())) {
            FaceGroupDo faceGroupDo = faceGroupService.getById(camera.getFaceGroupId());
            if (faceGroupDo != null) {
                cameraObj.put("face_group_path",
                        baseFacePath + faceGroupDo.getGroupPath());
            }
        }

        cameraObj.put("face_check_threshold",
                camera.getFaceCheckThreshold() == null ? 75 : camera.getFaceCheckThreshold());

        cameraObj.put("face_similarity_score",
                camera.getFaceSimilarityScore() == null ? 80 : camera.getFaceSimilarityScore());
    }

    // 解析 valid_time 字段
    private JSONObject parseValidTime(String checkTime) {
        JSONObject validTime = new JSONObject();
        String[] weekdaysTime = checkTime.split(";");
        for (int i = 0; i < weekdaysTime.length; i++) {
            validTime.put(String.valueOf((i + 1) % 7), weekdaysTime[i].split(","));
        }
        return validTime;
    }

    // 生成组算法配置json
    private JSONObject createGroupAlgJson(String deviceId) {
        JSONObject groupObj = new JSONObject();
        List<CameraGroup> cameraGroupList = cameraGroupService.list();
        if (cameraGroupList.isEmpty()) return groupObj;

        // 提取所有组 ID 和设备 ID，用于批量查询相机和算法信息
        List<String> groupIds = cameraGroupList.stream().map(CameraGroup::getId).toList();
        Map<String, List<CameraManager>> cameraMap = cameraManagerService.getCameraByGroups(deviceId, groupIds)
                .stream()
                .collect(Collectors.groupingBy(CameraManager::getGroupId));

        // 批量获取所有算法信息
        List<String> cameraIds = cameraMap.values().stream()
                .flatMap(List::stream)
                .map(CameraManager::getId)
                .distinct()
                .toList();
        if (cameraIds.isEmpty()) return groupObj;
        List<AlgorithmManager> algList = algorithmManagerService.getListByCameraIds(cameraIds).stream()
                .filter(alg -> alg.getIsRotate().equals(1))
                .filter(alg -> groupIds.contains(alg.getGroupId()))
                .toList();

        // 批量获取所有算法类别信息
        List<String> modelNames = algList.stream()
                .map(AlgorithmManager::getModelName)
                .distinct()
                .toList();
        Map<String, KyCategory> categoryMap = categoryService.listByIds(modelNames).stream()
                .collect(Collectors.toMap(KyCategory::getId, Function.identity()));

        // 构建 JSON 数据
        for (CameraGroup cameraGroup : cameraGroupList) {
            JSONObject obj = new JSONObject();
            JSONObject cameraInfoObj = new JSONObject();
            groupObj.put(cameraGroup.getId(), obj);

            obj.put("run_status", cameraGroup.getRunStatus());
            obj.put("polling_period", cameraGroup.getPollingPeriod());

            String groupId = cameraGroup.getId();
            List<CameraManager> cameraList = cameraMap.getOrDefault(groupId, Collections.emptyList());
            for (CameraManager camera : cameraList) {
                JSONObject roiObj = new JSONObject();
                JSONObject algObj = new JSONObject();

                // 过滤当前相机和组的算法
                List<AlgorithmManager> filteredAlgs = algList.stream()
                        .filter(alg -> alg.getBelongTo().equals(camera.getId()) && alg.getGroupId().equals(groupId))
                        .toList();

                for (AlgorithmManager alg : filteredAlgs) {
                    JSONArray cameraRoiAry = JSON.parseArray(alg.getCheckArea() != null ? alg.getCheckArea() : "[]");
                    JSONArray invalidRoiAry = JSON.parseArray(alg.getInvalidArea() != null ? alg.getInvalidArea() : "[]");

                    JSONObject allRoiObj = new JSONObject();
                    allRoiObj.put("camera_roi", cameraRoiAry);
                    allRoiObj.put("invalid_roi", invalidRoiAry);

                    JSONObject cfgObj = new JSONObject();
                    cfgObj.put("valid_time", parseValidTime(alg.getCheckTime()));
                    cfgObj.put("alarm_grade", alg.getAlarmLevel());
                    cfgObj.put("alarm_type", "0"); // 暂留字段
                    cfgObj.put("alarm_period", "0"); // 暂留字段
                    cfgObj.put("false_detect_period", "0"); // 暂留字段
                    cfgObj.put("alarm_Interval_period", "0"); // 暂留字段
                    cfgObj.put("hit_count", "0"); // 暂留字段
                    cfgObj.put("algorithm_threshold", String.valueOf(alg.getThreshold()));
                    cfgObj.put("target_threshold", String.valueOf(alg.getTargetThreshold()));
                    cfgObj.put("iou_threshold", String.valueOf(alg.getIouThreshold()));

                    KyCategory algCode = categoryMap.get(alg.getModelName());
                    if (algCode != null) {
                        algObj.put(algCode.getCode(), cfgObj);
                        roiObj.put(algCode.getCode(), allRoiObj);
                    }
                }

                cameraInfoObj.put(camera.getId(), roiObj);
                obj.put("algorithm_cfg", algObj);
            }

            obj.put("camera_info", cameraInfoObj);
        }

        return groupObj;
    }

    // 初始化相机配置缓存
    public void initCameraConfigCache() {
        List<DeviceManagement> deviceList = deviceManagementService.list();
        if (deviceList.isEmpty()) {
            redisUtil.set(Const.CAMERA_LIST_INFO_KEY, "[]");
            return;
        }
        for (DeviceManagement device : deviceList) {
            JSONArray cameraAry = createCameraJson(device.getId());
            redisUtil.set(Const.CAMERA_LIST_INFO_KEY, JSON.toJSONString(cameraAry));
        }
    }

    // 初始化算法配置缓存
    public void initAlgConfigCache() {
        List<DeviceManagement> deviceList = deviceManagementService.list();
        if (deviceList.isEmpty()) {
            redisUtil.set(Const.ALGORITHM_LIST_INFO_KEY, "[]");
            return;
        }
        for (DeviceManagement device : deviceList) {
            JSONArray ary = createAlgJson(device.getId());
            redisUtil.set(Const.ALGORITHM_LIST_INFO_KEY, JSON.toJSONString(ary));
        }
    }

    // 初始化组算法配置缓存
    public void initGroupAlgConfigCache() {
        List<DeviceManagement> deviceList = deviceManagementService.list();
        for (DeviceManagement device : deviceList) {
            JSONObject object = createGroupAlgJson(device.getId());
            redisUtil.set(Const.GROUP_ALGORITHM_LIST_INFO_KEY, JSON.toJSONString(object));
        }
    }

    // 初始化系统配置缓存
    public void initSystemConfigCache() {
        List<DeviceManagement> deviceList = deviceManagementService.list();
        for (DeviceManagement device : deviceList) {
            updateAiServerSystemConfig(device.getId(), device.getDeviceIp());
        }
    }

    // 发布相机状态事件
    private void asyncCameraStatus(DeviceManagement deviceManagement, String eventType) {
        List<CameraManager> cameraList = cameraManagerService.getListByDeviceId(deviceManagement.getId());
        // 未查询到相机的话，可能删除了所有相机，必须重启一次ai服务
        if (ObjectUtil.isEmpty(cameraList)) {
            log.info("未查询到相机，直接重启ai服务");
            CmdUti.restartAiServer();
            return;
        }
        List<CameraEventMsg> cameraEventMsgList = new ArrayList<>();
        for (CameraManager cameraManager : cameraList) {
            CameraEventMsg cameraEventMsg = new CameraEventMsg();
            cameraEventMsg.setCameraId(cameraManager.getId());
            cameraEventMsg.setStreamAddress(cameraManager.getStreamAddress());
            cameraEventMsgList.add(cameraEventMsg);
        }
        CameraEventOptions cameraEventOptions = new CameraEventOptions();
        if (StrUtil.isNotEmpty(deviceManagement.getDeviceIp()))
            cameraEventOptions.setDeviceIp(deviceManagement.getDeviceIp());
        cameraEventOptions.setEventType(eventType);
        cameraEventOptions.setCameraOriginalList(cameraList);
        cameraEventPublisher.publishCameraEvent(cameraEventMsgList, cameraEventOptions);
    }
}
