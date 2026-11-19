package org.jeecg.modules.mqtt.handler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.alarmAudit.entity.AlarmAudit;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.configjson.Const;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.devwebsocket.AlarmToWebSocket;
import org.jeecg.modules.devwebsocket.DataPushWebSocket;
import org.jeecg.modules.face.baidu.FaceDataQueue;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.mqtt.alarm.AlarmDataQueue;
import org.jeecg.modules.mqtt.alarm.AlarmFaceQueue;
import org.jeecg.modules.mqtt.alarm.AuditDataQueue;
import org.jeecg.modules.mqtt.alarm.OtherConsumer;
import org.jeecg.modules.system.service.ISysDictService;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.AlarmPushConf;
import org.jeecg.modules.thirdApi.AlarmPushHandler;
import org.jeecg.modules.thirdApi.pojo.AlarmPushEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Slf4j
public class AlarmMsgHandler {

    private RedisUtil redisUtil;

    private IDeviceManagementService deviceManagementService;

    private ICameraManagerService cameraManagerService;

    private AlarmToWebSocket alarmToWebSocket;

    private AlarmPushHandler alarmPushHandler;

    private IKyCategoryService categoryService;

    private AlarmPushConf alarmPushConf;

    private ISysDictService sysDictService;

    private DataPushWebSocket dataPushWebSocket;

    private AlarmDataQueue alarmDataQueue;

    private AuditDataQueue auditDataQueue;

    private FaceDataQueue faceDataQueue;

    private AlarmFaceQueue alarmFaceQueue;

    @Autowired
    public void setAlarmFaceQueue(AlarmFaceQueue alarmFaceQueue) {
        this.alarmFaceQueue = alarmFaceQueue;
    }

    @Autowired
    public void setFaceDataQueue(FaceDataQueue faceDataQueue){
        this.faceDataQueue = faceDataQueue;
    }

    @Autowired
    public void setAuditDataQueue(AuditDataQueue auditDataQueue) {
        this.auditDataQueue = auditDataQueue;
    }

    @Autowired
    public void setRedisUtil(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Autowired
    public void setAlarmDataQueue(AlarmDataQueue alarmDataQueue) {
        this.alarmDataQueue = alarmDataQueue;
    }

    @Autowired
    public void setDataPushWebSocket(DataPushWebSocket dataPushWebSocket) {
        this.dataPushWebSocket = dataPushWebSocket;
    }

    @Autowired
    public void setSysDictService(ISysDictService sysDictService) {
        this.sysDictService = sysDictService;
    }

    @Autowired
    public void setAlarmPushConf(AlarmPushConf alarmPushConf) {
        this.alarmPushConf = alarmPushConf;
    }

    @Autowired
    public void setCategoryService(IKyCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Autowired
    public void setAlarmPushHandler(AlarmPushHandler alarmPushHandler) {
        this.alarmPushHandler = alarmPushHandler;
    }

    @Autowired
    public void setAlarmToWebSocket(AlarmToWebSocket alarmToWebSocket) {
        this.alarmToWebSocket = alarmToWebSocket;
    }

    @Autowired
    public void setDeviceManagementService(IDeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }

    @Autowired
    public void setCameraManagerService(ICameraManagerService cameraManagerService) {
        this.cameraManagerService = cameraManagerService;
    }

    @Async
    public void handleMessage(String msg) {
        log.info("接收到原始告警信息：{}", msg);
        dataPushWebSocket.sendMessageToUser("原始告警信息：" + msg);

        List<JSONObject> jsonList = new ArrayList<>();

        try {
            JSONObject camera_alarms = JSON.parseObject(msg);
            String receiveCameraId = camera_alarms.getString("id");

            // 通过相机id获取设备信息
            Result<DeviceManagement> deviceManagementResult = getDeviceInfo(receiveCameraId);
            if (!deviceManagementResult.isSuccess()) {
                log.error(deviceManagementResult.getMessage());
                dataPushWebSocket.sendMessageToUser(deviceManagementResult.getMessage());
                return;
            }
            DeviceManagement deviceManagement = deviceManagementResult.getResult();

            // 获取相机信息
            Result<CameraManager> cameraInfo = getCameraInfo(deviceManagement.getId(), receiveCameraId);
            if (!cameraInfo.isSuccess()) {
                log.error(cameraInfo.getMessage());
                dataPushWebSocket.sendMessageToUser(cameraInfo.getMessage());
                return;
            }
            CameraManager cameraManager = cameraInfo.getResult();

            // 处理数据
            JSONArray receiveData = camera_alarms.getJSONArray("data");
            if (receiveData == null) {
                log.error("告警信息中，告警数据data为空");
                dataPushWebSocket.sendMessageToUser("告警信息中，告警数据data为空");
                return;
            }

            for (int i = 0; i < receiveData.size(); i++) {
                JSONObject alarm = receiveData.getJSONObject(i);
                log.info("处理告警: {}", alarm.toJSONString());

                // 如果是围栏缺失算法，直接调用大模型处理
                // boolean ok = fenceMissingAlarm(alarm, camera_alarms);
                // if (ok) continue;

                // 检查face_url, 如果不为空，丢给人脸识别检测队列
                if (checkFaceUrl(alarm, camera_alarms, cameraManager)) continue;

                // 组装AlarmDetail 对象， 添加到消费队列
                AlarmDetail alarmDetail =buildAlarmDetail(alarm, cameraManager, deviceManagement);

                // 判断face_name是不是文件路径，用于人脸识别, 不产生告警
                if (checkFaceName(alarmDetail)) continue;

                // 放入告警队列中，批量处理
                alarmDataQueue.addEvent(alarmDetail);

                // 组装 AlarmAudit 对象, 添加到消费队列
                // AlarmAudit alarmAudit = buildAlarmAudit(alarmDetail);
                // 单线程处理
                // auditDataQueue.addEvent(alarmAudit);

                //推送消息到前端（弹窗）
                pushAlarmMsgByWebSocket(alarmDetail);

                // 平台推送 async
                pushAlarmMsgToOtherServer(alarmDetail, cameraManager, deviceManagement);

                // 报警算法缓存数组，主要用于外设控制
                int algId = Integer.parseInt(alarmDetail.getAlarmType());
                JSONObject temp = new JSONObject();
                temp.put("cameraId", alarmDetail.getCameraId());
                temp.put("algorithmCode", algId);
                jsonList.add(temp);
            }

            // 外设控制
            OtherConsumer.run(jsonList);

            // 串口音柱 弃用
            // SendAlarmSystem sendAlarmSystem = SpringContextUtils.getBean(SendAlarmSystem.class);
            // if (sendAlarmSystem.getState().equals("1")) {
            //     log.info("暂不支持串口音柱");
            //     log.info("串口已启用，下发控制指令，当前算法：{}", objects);
            //     AlarmToSoundPillarHandle alarmToSoundPillarHandle = SpringContextUtils.getBean(AlarmToSoundPillarHandle.class);
            //     alarmToSoundPillarHandle.handleSoundPillarPlay(objects);
            // }

        } catch (Exception e) {
            log.error("处理告警信息失败：{}", e.getLocalizedMessage());
        }
    }

    /**
     * 生成网络图片URL
     */
    private String formatImageURL(String devIp, String picPath) {
        if (StrUtil.isEmpty(devIp)) return picPath;
        return "http://" + devIp + ":8038/alarmImg" + picPath;
    }

    /**
     * 根据算法code获取算法名称
     */
    private String formatAlarmType(String alarmType) {
        KyCategory kyCategory = categoryService.getOneCode(alarmType);
        if (ObjectUtil.isNotEmpty(kyCategory)) return kyCategory.getName();
        return alarmType;
    }

    /**
     * 根据告警level code 获取名称
     */
    private String formatAlarmLevel(String levelCode) {
        String alarmLevel = sysDictService.queryDictTextByKey("alarm_level", levelCode);
        if (StrUtil.isNotEmpty(alarmLevel)) return alarmLevel;
        return levelCode;
    }

    /**
     * 获取相机列表
     */
    private Result<CameraManager> getCameraInfo(String deviceId, String cameraId) {
        String redisKey = Const.DEVICE_LOCAL_CAMERA_LIST_KEY;
        Object cacheValue = redisUtil.get(redisKey);

        List<CameraManager> cameraManagerList = parseCameraList(cacheValue, deviceId, redisKey);
        if (cameraManagerList == null || cameraManagerList.isEmpty()) {
            return Result.error("未查询到任何相机信息");
        }

        return findCamera(cameraManagerList, cameraId)
                .map(Result::OK)
                .orElseGet(() -> {
                    // 缓存没有，则从数据库查单个
                    CameraManager dbCamera = cameraManagerService.getById(cameraId);
                    if (dbCamera != null) {
                        // 更新缓存为最新列表
                        List<CameraManager> newList = cameraManagerService.getListByDeviceId(deviceId);
                        redisUtil.set(redisKey, newList);
                        return Result.OK(dbCamera);
                    }
                    return Result.error("未在缓存或数据库中找到相机信息，请重新添加或编辑相机信息");
                });
    }

    /**
     * 根据 Redis 取回的数据反序列化并处理缓存更新
     */
    private List<CameraManager> parseCameraList(Object cacheValue, String deviceId, String redisKey) {
        if (cacheValue == null) {
            List<CameraManager> dbList = cameraManagerService.getListByDeviceId(deviceId);
            if (dbList != null && !dbList.isEmpty()) {
                redisUtil.set(redisKey, dbList);
            }
            return dbList;
        }

        if (cacheValue instanceof String) {
            return JSONArray.parseArray((String) cacheValue, CameraManager.class);
        }

        if (cacheValue instanceof List) {
            return (List<CameraManager>) cacheValue;
        }

        return null;
    }

    /**
     * 在列表中查找指定相机
     */
    private Optional<CameraManager> findCamera(List<CameraManager> list, String cameraId) {
        return list.stream()
                .filter(cm -> cameraId.equals(cm.getId()))
                .findFirst();
    }

    /**
     * 获取设备信息
     */
    private Result<DeviceManagement> getDeviceInfo(String cameraId) {
        Object deviceLocalInfo = redisUtil.get(Const.DEVICE_LOCAL_INFO_KEY);
        if (deviceLocalInfo == null) {
            DeviceManagement deviceManagement = deviceManagementService.getByCameraId(cameraId);
            redisUtil.set(Const.DEVICE_LOCAL_INFO_KEY, deviceManagement);
            return Result.OK(deviceManagement);
        }
        if (deviceLocalInfo instanceof DeviceManagement deviceManagement) {
            return Result.OK(deviceManagement);
        } else if (deviceLocalInfo instanceof String deviceInfoStr) {
            JSONObject devJson = JSONObject.parseObject(deviceInfoStr);
            return Result.OK(JSONObject.toJavaObject(devJson, DeviceManagement.class));
        } else {
            return Result.error("未在缓存中找到设备信息，请重新添加或编辑设备信息");
        }
    }

    /**
     * 组装AlarmDetail
     */
    private AlarmDetail buildAlarmDetail(JSONObject alarmData, CameraManager cameraManager, DeviceManagement deviceManagement) throws  Exception{

        AlarmDetail alarmDetail = new AlarmDetail();

        alarmDetail.setFaceName(StrUtil.equals(alarmData.getString("face_name"), "no_info")? "" : alarmData.getString("face_name"));

        alarmDetail.setId(IdUtil.getSnowflake(1, 1).nextIdStr());
        alarmDetail.setAlarmType(alarmData.getString("value"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        alarmDetail.setAlarmTime(sdf.parse(alarmData.getString("time")));

        alarmDetail.setCameraId(cameraManager.getId());
        alarmDetail.setCameraName(cameraManager.getCameraName());
        alarmDetail.setAlarmArea(cameraManager.getLocation());
        alarmDetail.setAlarmLevel(alarmData.getString("grade"));
        alarmDetail.setPicPath(alarmData.getString("picUrl"));
        alarmDetail.setThumbnailPath(alarmData.getString("picUrl"));

        //新增相机编码字段
        alarmDetail.setCameraNumber(cameraManager.getCameraNumber());
        alarmDetail.setTargetCount(alarmData.getInteger("targetCount"));
        alarmDetail.setCarColor(alarmData.getString("carColor"));
        alarmDetail.setCarNumber(alarmData.getString("carNumber"));
        alarmDetail.setCarType(alarmData.getString("carType"));

        //新增轮询或实时区分字段
        alarmDetail.setIsRotate(alarmData.getInteger("isRotate"));
        alarmDetail.setRemarks("0");

        //alarmDetail.setAlarmId(alarm.getString("key"));

        //将设备ip存入此字段为了删除相机后可以拼接图片地址
        alarmDetail.setAlarmId(deviceManagement.getDeviceIp());
        alarmDetail.setDeviceName(deviceManagement.getDeviceName());

        return alarmDetail;
    }

    /**
     * 组装AlarmAudit
     */
    private AlarmAudit buildAlarmAudit(AlarmDetail alarmDetail) throws  Exception{
        AlarmAudit audit = new AlarmAudit();
        audit.setAlarmId(alarmDetail.getId());
        audit.setCreateTime(new Date());
        audit.setAuditStatus("0");
        return audit;
    }

    /**
     * 通过websocket推送告警信息，前端弹窗使用
     */
    private void pushAlarmMsgByWebSocket(AlarmDetail alarmDetail){
        JSONObject alarmMsg = new JSONObject();
        alarmMsg.put("event", "alarm_received");
        alarmMsg.put("cameraId", alarmDetail.getCameraId());
        alarmMsg.put("cameraName", alarmDetail.getCameraName());
        alarmMsg.put("algorithmName", alarmDetail.getAlarmType());
        alarmMsg.put("eventImg", alarmDetail.getPicPath());
        alarmMsg.put("eventTime", alarmDetail.getAlarmTime());
        alarmToWebSocket.sendMessageToUser(alarmMsg.toJSONString());
    }

    /**
     * 推送告警信息到其他服务
     */
    private void pushAlarmMsgToOtherServer(AlarmDetail alarmDetail, CameraManager camera, DeviceManagement dev){
        if (!alarmPushConf.isEnable()) {
            log.info("推送告警信息到其他服务已关闭");
            return;
        }

        AlarmPushEntity alarmPushEntity = new AlarmPushEntity();
        String deviceIp = dev != null ? dev.getNetworkIp() : "";
        alarmPushEntity.setId(alarmDetail.getId());
        alarmPushEntity.setCameraId(alarmDetail.getCameraId());
        alarmPushEntity.setCameraSn(alarmDetail.getCameraNumber());
        alarmPushEntity.setCameraName(alarmDetail.getCameraName());
        alarmPushEntity.setCameraIp(camera.getIp());
        alarmPushEntity.setAlarmType(formatAlarmType(alarmDetail.getAlarmType()));
        alarmPushEntity.setAlarmTypeCode(alarmDetail.getAlarmType());
        alarmPushEntity.setAlarmLevel(formatAlarmLevel(alarmDetail.getAlarmLevel()));
        alarmPushEntity.setAlarmLevelCode(alarmDetail.getAlarmLevel());
        alarmPushEntity.setAlarmPictureUrl(formatImageURL(deviceIp, alarmDetail.getPicPath()));
        alarmPushEntity.setAlarmPictureBase64(AlarmHandleUtil.getAlarmImgBase64(alarmDetail.getPicPath()));
        alarmPushEntity.setGroupNames(AlarmHandleUtil.getGroupNames(camera.getLocation()));
        alarmPushEntity.setDeviceIp(deviceIp);
        alarmPushEntity.setDeviceName(dev != null ? dev.getDeviceName() : "");
        alarmPushEntity.setDeviceSn(SysParameterUtil.getDeviceSn());
        alarmPushEntity.setAlarmTime(alarmDetail.getAlarmTime());

        // 推送日志组合
        JSONObject pushLog = new JSONObject();
        pushLog.put("cameraId", alarmPushEntity.getCameraId());
        pushLog.put("cameraName", alarmPushEntity.getCameraName());
        pushLog.put("alarmType", alarmPushEntity.getAlarmType());
        pushLog.put("alarmTime", DateUtil.format(alarmPushEntity.getAlarmTime(), "yyyy-MM-dd HH:mm:ss"));
        dataPushWebSocket.sendMessageToUser("准备推送数据：" + pushLog.toJSONString());

        // 异步推送
        alarmPushHandler.pushData(alarmPushEntity);

    }

    private boolean checkFaceUrl(JSONObject alarm, JSONObject camera_alarms, CameraManager cameraManager) {
        if (!SysParameterUtil.isBaiduFaceEnabled()) return false;

        String facePath = alarm.getString("face_path");
        if (StrUtil.isNotBlank(facePath) && !StrUtil.equals("no_info", facePath)) {
            // 放入百度检测队列
            Map<String, Object> event = new HashMap<>();
            event.put("cameraId", camera_alarms.getString("id"));
            event.put("picUrl", alarm.getString("picUrl"));
            event.put("algorithmId", alarm.getString("value"));
            event.put("isRotate", alarm.getInteger("isRotate"));
            event.put("targetCount", alarm.getInteger("targetCount"));
            event.put("time", alarm.getString("time"));

            event.put("facePath", facePath);
            event.put("bindGroupId", StrUtil.isBlank(cameraManager.getFaceGroupId())? null : cameraManager.getFaceGroupId());
            faceDataQueue.addEvent(event);
            log.info("放入 face server 检测队列：{}", event);
            return true;
        }
        return false;
    }

    private boolean checkFaceName(AlarmDetail alarmDetail) {
        if (AlarmHandleUtil.isFilePath(alarmDetail.getFaceName())) {
            alarmDetail.setThumbnailPath(alarmDetail.getFaceName());
            alarmDetail.setPicPath(alarmDetail.getFaceName());
            alarmDetail.setFaceName("");
            alarmFaceQueue.addEvent(alarmDetail);
            return true;
        }
        return false;
    }
}
