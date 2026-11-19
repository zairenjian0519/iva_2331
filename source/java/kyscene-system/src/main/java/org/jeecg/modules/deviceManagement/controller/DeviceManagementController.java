package org.jeecg.modules.deviceManagement.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.utils.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.shiro.SecurityUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.constant.SymbolConstant;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.CommonUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.algorithmmanager.service.IAlgorithmManagerService;
import org.jeecg.modules.cameraFactory.entity.CameraFactory;
import org.jeecg.modules.cameraFactory.service.ICameraFactoryService;
import org.jeecg.modules.cameraGroup.entity.CameraGroup;
import org.jeecg.modules.cameraGroup.service.ICameraGroupService;
import org.jeecg.modules.cameraGroupAlgorithm.entity.CameraGroupAlgorithm;
import org.jeecg.modules.cameraGroupAlgorithm.service.ICameraGroupAlgorithmService;
import org.jeecg.modules.cameraGroupCamera.entity.CameraGroupCamera;
import org.jeecg.modules.cameraGroupCamera.service.ICameraGroupCameraService;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.configjson.Const;
import org.jeecg.modules.configjson.CameraDeploymentConfigService;
import org.jeecg.modules.configjson.CreateConfigJson;
import org.jeecg.modules.configjson.CreateConfigJsonService;
import org.jeecg.modules.deviceManagement.entity.CloudConfig;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.devicelog.entity.DeviceLog;
import org.jeecg.modules.devicelog.service.IDeviceLogService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.MqttConfigProperties;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.CmdUti;
import org.jeecg.modules.system.util.StorageUtil;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 设备管理
 * @Author: jeecg-boot
 * @Date: 2022-08-23
 * @Version: V1.0
 */
@Api(tags = "设备管理")
@RestController
@RequestMapping("/deviceManagement/deviceManagement")
@Slf4j
public class DeviceManagementController extends JeecgController<DeviceManagement, IDeviceManagementService> {
    //读取YML文件,修改设备的license的状态
    @Value("${deviceManagement.license_status}")
    private String license_status;

    @Value("${deviceManagement.ai_package_version: v1.0.6}")
    private String aiPackageVersion;

    @Autowired
    private IDeviceManagementService deviceManagementService;

    @Autowired
    private IDeviceLogService deviceLogService;

    @Autowired
    private ICameraManagerService cameraManagerService;

    @Autowired
    private IAlgorithmManagerService algorithmManagerService;

    @Autowired
    private CreateConfigJson createConfigJson;

    @Autowired
    private CreateConfigJsonService createConfigJsonService;

    @Autowired
    private CameraDeploymentConfigService cameraDeploymentConfigService;

    @Autowired
    private RedisUtil redisUtil;

    @Value(value = "${jeecg.path.upload}")
    private String fileUploadPath;

    @Autowired
    private ICameraGroupService cameraGroupService;
    @Autowired
    private ICameraGroupCameraService cameraGroupCameraService;
    @Autowired
    private ICameraGroupAlgorithmService cameraGroupAlgorithmService;
    @Autowired
    private IKyCategoryService kyCategoryService;
    @Autowired
    private ICameraFactoryService cameraFactoryService;
    @Autowired
    private IAlarmDetailService alarmDetailService;

    /**
     * 分页列表查询
     *
     * @param deviceManagement
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    //@AutoLog(value = "设备管理-分页列表查询")
    @ApiOperation(value = "设备管理-分页列表查询", notes = "设备管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<DeviceManagement>> queryPageList(DeviceManagement deviceManagement,
                                                         @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                         @RequestParam(name = "pageSize", defaultValue = "50") Integer pageSize,
                                                         HttpServletRequest req) {
        QueryWrapper<DeviceManagement> queryWrapper = QueryGenerator.initQueryWrapper(deviceManagement, req.getParameterMap());
        Page<DeviceManagement> page = new Page<DeviceManagement>(pageNo, pageSize);
        IPage<DeviceManagement> pageList = deviceManagementService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @ApiOperation(value = "设备管理-重启", notes = "设备管理-重启")
    @GetMapping(value = "/reboot")
    public Result<String> reboot(@RequestParam(name = "ip") String ip) {
        // try {
        //     MqttClientConnect clientConnect = MqttClientService.getInstance().getMqttClient(ip);
        //     clientConnect.pub("web_aireboot", "1");
        // } catch (MqttException e) {
        //     return Result.error("发送重启失败!");
        // }
        int restarted = CmdUti.restartAiServer();
        if (restarted != 0) {
            return Result.error("重启失败!");
        }
        return Result.OK("重启成功!");
    }

    @GetMapping(value = "/rebootDM")
    public Result<String> rebootDM() {
        try {
            String[] cmd = {"sudo", "/home/super/web/dm_restart.sh"};
            String result = CmdUti.executeCommand(cmd);
            if (StrUtil.isNotBlank(result)) return Result.error(result);
            return Result.OK("重启成功");
        } catch (Exception e) {
            return Result.error("发送重启失败!");
        }
    }

    /**
     * 添加
     *
     * @param deviceManagement
     * @return
     */
    @AutoLog(value = "设备管理-添加")
    @ApiOperation(value = "设备管理-添加", notes = "设备管理-添加")
//	@RequiresPermissions("org.jeecg.modules:device_management:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody DeviceManagement deviceManagement) {
        deviceManagement.setAlgorithmNum(9999); // TODO: 授权未做 先默认不限制
        deviceManagement.setCameraNum(999);
        deviceManagement.setDeviceCode("0000000000000"); //没有实际意义为了生成相机id
        deviceManagement.setLicenseStatus(license_status); //该字段为设备证书认证状态
        log.info("添加设备时配置文件的状态 : {}", license_status);
        if (license_status.equals("1")) {
            String[] split = deviceManagement.getCategory().split("-");
            deviceManagement.setAlgorithmCameraNum(Integer.parseInt(split[1]));
        } else {
            deviceManagement.setAlgorithmCameraNum(1); //算法提出 默认保留一台体验
        }
        deviceManagement.setAlgorithmCameraNum(1); //算法提出 默认保留一台体验
        DeviceManagement dev = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda()
                .eq(DeviceManagement::getDeviceIp, deviceManagement.getDeviceIp()));
        if (null != dev) {
            return Result.error("设备已存在，请检查设备IP.");
        }
        DeviceManagement checkNameDev = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda()
                .eq(DeviceManagement::getDeviceName, deviceManagement.getDeviceName()));
        if (null != checkNameDev && !(checkNameDev.getId().equals(deviceManagement.getId()))) {
            return Result.error("设备已存在，请检查设备名称.");
        }
        //资源编码隐藏 新增设备编码字段
//		DeviceManagement checkCodeDev = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda()
//				.eq(DeviceManagement::getDeviceCode, deviceManagement.getDeviceCode()));
//		if(null != checkCodeDev && !(checkCodeDev.getId().equals(deviceManagement.getId()))){
//			return Result.error("设备已存在，请检查设备资源编码.");
//		}
        //拼接指令
        String[] modifyIpCmd = {"sudo", "/home/super/kyland_tools/modify_ip " + deviceManagement.getDeviceIp()};
        log.info("获取到指令：{}", Arrays.toString(modifyIpCmd));
        try {
            String modifyResult = CmdUti.executeCommand(modifyIpCmd);
            if (StrUtil.isNotBlank(modifyResult)) {
                return Result.error("操作失败，请确认超脑IP是否可达");
            }
            //打印命令的退出码到控制台
            log.info("modify_ip脚本执行成功");
        } catch (IOException | InterruptedException e) {
            log.info("执行脚本modify_ip失败: {}", e.getLocalizedMessage());
            return Result.error("操作失败，请确认超脑IP是否可达");
        }
        deviceManagementService.save(deviceManagement);
        deviceManagementService.initMqtt();
        // 新增或编辑设备后生成系统文件
        createConfigJson.saveSystemConfig(deviceManagement.getId(), deviceManagement.getDeviceIp());
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "设备管理-编辑")
    @ApiOperation(value = "设备管理-编辑", notes = "设备管理-编辑")
//	@RequiresPermissions("org.jeecg.modules:device_management:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody DeviceManagement deviceManagement) {
        DeviceManagement dev = deviceManagementService.getById(deviceManagement.getId());
        DeviceManagement checkIpDev = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda()
                .eq(DeviceManagement::getDeviceIp, deviceManagement.getDeviceIp()));
        if (null != checkIpDev && !(checkIpDev.getId().equals(deviceManagement.getId()))) {
            return Result.error("编辑失败，设备IP已存在.");
        }
        DeviceManagement checkNameDev = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda()
                .eq(DeviceManagement::getDeviceName, deviceManagement.getDeviceName()));
        if (null != checkNameDev && !(checkNameDev.getId().equals(deviceManagement.getId()))) {
            return Result.error("编辑失败，设备名称已存在.");
        }

        if (!(dev.getDeviceIp().equals(deviceManagement.getDeviceIp()))) {
            alarmDetailService.update(new LambdaUpdateWrapper<AlarmDetail>().eq(AlarmDetail::getAlarmId, dev.getDeviceIp()).set(AlarmDetail::getAlarmId, deviceManagement.getDeviceIp()));
            //获取原来的设备的ip
            String deviceIp = dev.getDeviceIp();
            //关闭客户端
            try {
                MqttClientService.getInstance().closeMqttClient(deviceIp);
            } catch (MqttException e) {
                log.error("关闭MQTT客户端失败,关闭失败的设备ip: {}", deviceIp);
                return Result.error("编辑失败");
            }

            //拼接指令
            String[] modifyIpCmd = {"sudo", "/home/super/kyland_tools/modify_ip " + deviceManagement.getDeviceIp()};
            log.info("获取到指令：{}", String.join(",", modifyIpCmd));
            try {
                String modifyResult = CmdUti.executeCommand(modifyIpCmd);
                if (StrUtil.isNotBlank(modifyResult)) {
                    return Result.error("操作失败，请确认超脑IP是否可达");
                }
                //打印命令的退出码到控制台
                log.info("modify_ip脚本执行成功");
            } catch (IOException | InterruptedException e) {
                log.info("执行脚本modify_ip失败: {}", e.getLocalizedMessage());
                return Result.error("操作失败，请确认超脑IP是否可达");
            }
            try {
                Thread.sleep(1000);
                // 新增或编辑设备后生成系统文件
                createConfigJson.saveSystemConfig(deviceManagement.getId(), deviceManagement.getDeviceIp());
            } catch (InterruptedException e) {
                log.error("设备编辑执行Linux指令休眠异常: {}", e.getMessage());
                return Result.error("操作失败");
            }
        }

        // 更新
        BeanUtil.copyProperties(deviceManagement, dev, CopyOptions.create().ignoreNullValue());
        deviceManagementService.updateById(dev);

        // 存入缓存
        redisUtil.set(Const.DEVICE_LOCAL_INFO_KEY, dev);

        // 初始化mqtt
        deviceManagementService.initMqtt();
        return Result.OK("编辑成功!");
    }

    /**
     * 根据websocket的变化改变数据库中设备的状态码
     */
    @AutoLog(value = "设备改变状态")
    @ApiOperation(value = "设备改变状态", notes = "设备改变状态")
    @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> changeStatus(@RequestBody JSONObject jsonObject) {
        String ip = jsonObject.getString("ip");
        String status = jsonObject.getString("status");
        // 参数判断
        if (StringUtils.isEmpty(ip) || StringUtils.isEmpty(status)) {
            return Result.error("更新设备状态失败，参数错误");
        }
        return deviceManagementService.updateDeviceStatus(ip, status);
    }


    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "设备管理-通过id删除")
    @ApiOperation(value = "设备管理-通过id删除", notes = "设备管理-通过id删除")
//	@RequiresPermissions("org.jeecg.modules:device_management:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        List<String> deletedCameraIds = cameraManagerService.getListByDeviceId(id).stream()
                .map(CameraManager::getId).collect(Collectors.toList());
        DeviceManagement dev = deviceManagementService.getById(id);
        if (null != dev) {
            try {
                MqttClientService.getInstance().closeMqttClient(dev.getDeviceIp());
            } catch (MqttException e) {
                log.error(dev.getDeviceName() + " 关闭mqtt异常");
            }
        }
        deviceManagementService.removeById(id);
        deviceLogService.remove(new QueryWrapper<DeviceLog>().lambda().eq(DeviceLog::getDevid, id));
//		deviceManagementService.removeAlarmByDeviceId(id); 测试说删设备不删告警信息 2022.12.19
        cameraManagerService.removeByDeviceId(id);
        afterDeletedCameras(deletedCameraIds);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "设备管理-批量删除")
    @ApiOperation(value = "设备管理-批量删除", notes = "设备管理-批量删除")
//	@RequiresPermissions("org.jeecg.modules:device_management:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        List<String> deletedCameraIds = new ArrayList<>();
        for (String devId : ids.split(",")) {
            cameraManagerService.getListByDeviceId(devId).stream()
                    .map(CameraManager::getId).forEach(deletedCameraIds::add);
            DeviceManagement dev = deviceManagementService.getById(devId);
            if (null != dev) {
                try {
                    MqttClientService.getInstance().closeMqttClient(dev.getDeviceIp());
                } catch (MqttException e) {
                    log.error(dev.getDeviceName() + " 关闭mqtt异常");
                }
            }
            deviceLogService.remove(new QueryWrapper<DeviceLog>().lambda().eq(DeviceLog::getDevid, devId));
            deviceManagementService.removeAlarmByDeviceId(devId);
            cameraManagerService.removeByDeviceId(devId);
        }
        this.deviceManagementService.removeByIds(Arrays.asList(ids.split(",")));
        afterDeletedCameras(deletedCameraIds);
        return Result.OK("批量删除成功!");
    }

    private void afterDeletedCameras(Collection<String> ids) {
        try {
            cameraDeploymentConfigService.removeAssignments(ids);
        } catch (IOException error) {
            log.error("删除设备后清理推理部署映射失败，后端下次启动将重试: {}", ids, error);
        }
        createConfigJsonService.initCameraConfigCache();
        createConfigJsonService.initAlgConfigCache();
        CmdUti.restartAiEngine();
    }

    @ApiOperation(value = "设备管理-导出样本图片", notes = "设备管理-导出样本图片")
    @RequestMapping(value = "/exportImg")
    public Result<String> exportXls(@RequestParam(name = "id") String id, HttpServletResponse response) {
        try {
            DeviceManagement deviceManagement = deviceManagementService.getById(id);
            if (deviceManagement == null) {
                return Result.error("查询设备信息失败");
            }
            MqttClientConnect clientConnect = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
            if (null != clientConnect && clientConnect.getMqttClient().isConnected()) {
                JSONObject json = new JSONObject();
                redisUtil.set(deviceManagement.getDeviceIp() + "_webPicZip", "");
                json.put("opt", "getpic");
                clientConnect.pub("web_upsoft", json.toJSONString(), 2);
                Thread.sleep(500);
                String zipName = (String) redisUtil.get(deviceManagement.getDeviceIp() + "_webPicZip");
                int count = 0;
                while (zipName.equals("") && count < 6) {
                    zipName = (String) redisUtil.get(deviceManagement.getDeviceIp() + "_webPicZip");
                    count++;
                    Thread.sleep(500);
                }
                if (zipName.equals("")) {
                    return Result.error("未获取到要下载的文件");
                }
                return Result.ok(zipName);
            }
//				redisUtil.set(deviceManagement.getDeviceIp()+"_deviceFtp", 0);
//				JSONObject getFtpJson = new JSONObject();
//				getFtpJson.put("opt","getftp");
//				getFtpJson.put("sysID",3);
//				getFtpJson.put("fileName","");
//				clientConnect.pub("web_upsoft", getFtpJson.toJSONString(),1);
//				Object obj = redisUtil.get(deviceManagement.getDeviceIp()+"_deviceFtp");
//				for(int i=0; i<5 ;i++){ //发送获取ftp信息的mqtt消息后等待10s接收返回数据
//					if(obj instanceof Integer){
//						Thread.sleep(500);
//						obj = redisUtil.get(deviceManagement.getDeviceIp()+"_deviceFtp");
//					}else{
//						break;
//					}
//				}
//				if(obj instanceof Integer){
//					return;
//				}
//				FTPClient ftpClient = new FTPClient();
//				try{
//					JSONObject ftpInfo = (JSONObject) obj;
//					ftpClient.connect(ftpInfo.getString("ip"), ftpInfo.getInteger("port"));
//					ftpClient.login(ftpInfo.getString("user"), ftpInfo.getString("pwd"));
//					if(ftpClient.isConnected()){
//						ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
//						File localFile = new File(fileUploadPath+"devImage.zip");
//						FileOutputStream fis = new FileOutputStream(localFile);
//						boolean ftpRet = ftpClient.retrieveFile(zipName , fis);
//						if(ftpRet){
//							ftpClient.logout();
//							OutputStream outStream = response.getOutputStream();
//							FileInputStream in = new FileInputStream(localFile);
//							response.setContentType("application/octet-stream; charset=utf-8");
//							int len;
//							byte[] bs = new byte[1024];
//							while ((len = in.read(bs)) != -1){
//								outStream.write(bs, 0, len);
//							}
//							outStream.flush();
//							outStream.close();
//							clientConnect.pub("web_pic", "ok",1);
//						}
//						fis.close();
//					}
//				}catch (Exception e){
//					log.error(e.getMessage());
//				}finally{
//					if(ftpClient.isConnected()){
//						try {
//							ftpClient.disconnect();
//						}catch (Exception e){
//							e.printStackTrace();
//						}
//					}
//				}
//			}
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok();
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "设备管理-通过id查询")
    @ApiOperation(value = "设备管理-通过id查询", notes = "设备管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<DeviceManagement> queryById(@RequestParam(name = "id", required = true) String id) {
        DeviceManagement deviceManagement = deviceManagementService.getById(id);
        if (deviceManagement == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(deviceManagement);
    }

    @GetMapping(value = "/deviceInfo")
    public Result<JSONObject> getDeviceInfo() {
        List<DeviceManagement> list = deviceManagementService.list();
        JSONObject devInfo = new JSONObject();
        JSONArray devList = new JSONArray();
        if (!list.isEmpty()) {
            for (DeviceManagement dev : list) {
                if (StringUtils.isNotEmpty(dev.getDeviceIp())) {
                    JSONObject devMqtt = new JSONObject();
                    try {
                        MqttClientConnect clientConnect = MqttClientService.getInstance().getMqttClient(dev.getDeviceIp());
                        if (null != clientConnect && clientConnect.getMqttClient().isConnected()) {
                            devMqtt.put("status", "1");
                            dev.setStatus("1");
                            log.info("设备：{} 状态为：在线", dev.getDeviceIp());
                        } else {
                            devMqtt.put("status", "0");
                            dev.setStatus("0");
                            log.info("设备：{} 状态为：离线", dev.getDeviceIp());
                        }
                    } catch (MqttException e) {
                        devMqtt.put("status", "0");
                        dev.setStatus("0");
                        log.info("设备：{} 状态为：离线", dev.getDeviceIp());
                        log.info(e.getMessage());
                    }

                    devMqtt.put("useAlgorithmCameraNum", algorithmManagerService.getCameraLicense(dev.getId())); //可配算法的相机数量授权
                    devMqtt.put("useAlgorithmNum", deviceManagementService.getUsedAlgNum(dev.getId()));
                    devMqtt.put("useCameraNum", deviceManagementService.getUsedCameraNum(dev.getId()));
                    devMqtt.put("devIp", dev.getDeviceIp());
                    devMqtt.putAll(StorageUtil.getDiskUsage());
                    devMqtt.put("aiVer", aiPackageVersion);
                    deviceManagementService.updateById(dev);
                    devList.add(devMqtt);
                }
            }
        }
        devInfo.put("devList", devList);
        return Result.OK(devInfo);
    }

    @ApiOperation(value = "设备管理-查询设备基本信息", notes = "设备管理-查询设备基本信息")
    @GetMapping(value = "/devInfo")
    public Result<JSONObject> getDevInfo() {
        List<DeviceManagement> list = deviceManagementService.list();
//		 initMqtt(list);
        JSONObject devInfo = new JSONObject();
        JSONArray devList = new JSONArray();
        if (list.size() > 0) {
            for (DeviceManagement dev : list) {
                if (StringUtils.isNotEmpty(dev.getDeviceIp())) {
                    JSONObject devMqtt = new JSONObject();
                    JSONObject devInfoRedis = (JSONObject) redisUtil.get(dev.getDeviceIp() + "_deviceInfo");
//					log.info("devInfoRedis:"+devInfoRedis);
                    if (null != devInfoRedis) {
                        try {
                            MqttClientConnect clientConnect = MqttClientService.getInstance().getMqttClient(dev.getDeviceIp());
                            if (null != clientConnect && clientConnect.getMqttClient().isConnected()) {
                                devMqtt = devInfoRedis.clone();
                                devMqtt.put("status", "1");
                                dev.setStatus("1");
                                log.info("设备：" + dev.getDeviceIp() + "状态为：在线");
                            } else {
                                devMqtt.put("status", "0");
                                dev.setStatus("0");
                                log.info("设备：" + dev.getDeviceIp() + "状态为：离线");
                            }
                        } catch (MqttException e) {
                            devMqtt.put("status", "0");
                            dev.setStatus("0");
                            log.info("设备：" + dev.getDeviceIp() + "状态为：离线");
                            log.info(e.getMessage());
//							e.printStackTrace();
                        }
                    }
                    devMqtt.put("useAlgorithmCameraNum", algorithmManagerService.getCameraLicense(dev.getId())); //可配算法的相机数量授权
                    devMqtt.put("useAlgorithmNum", deviceManagementService.getUsedAlgNum(dev.getId()));
                    devMqtt.put("useCameraNum", deviceManagementService.getUsedCameraNum(dev.getId()));
                    devMqtt.put("devIp", dev.getDeviceIp());
                    deviceManagementService.updateById(dev);
                    devList.add(devMqtt);
                }
            }
        }
        devInfo.put("devList", devList);
        return Result.OK(devInfo);
    }

    @ApiOperation(value = "设备管理-查询设备算法包版本", notes = "设备管理-查询设备算法包版本")
    @GetMapping(value = "/devVerInfo")
    public Result<List<String>> getVerInfo(@RequestParam(name = "id") String deviceId) {
        List<String> list = new ArrayList<>();
        try {
            DeviceManagement deviceManagement = deviceManagementService.getById(deviceId);
            if (deviceManagement == null) {
                return Result.ok(list);
            }
            MqttClientConnect clientConnect = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
            if (null != clientConnect && clientConnect.getMqttClient().isConnected()) {
                JSONObject json = new JSONObject();
                redisUtil.set(deviceManagement.getDeviceIp() + "_deviceAIver", new JSONObject());
                json.put("opt", "getAIver");
                clientConnect.pub("web_upsoft", json.toJSONString(), 2);
                Thread.sleep(1000);
                Object obj = redisUtil.get(deviceManagement.getDeviceIp() + "_deviceAIver");
                for (Object o : ((JSONObject) obj).getJSONArray("verList")) {
                    list.add((String) o);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.OK(list);
    }

    @ApiOperation(value = "设备管理-查询设备算法包版本", notes = "设备管理-设置设备算法包版本")
    @GetMapping(value = "/setDevVerInfo")
    public Result<String> setDevVerInfo(@RequestParam(name = "id") String deviceId, @RequestParam(name = "ver") String ver) {
        try {
            DeviceManagement deviceManagement = deviceManagementService.getById(deviceId);
            if (deviceManagement == null) {
                return Result.error("设备不存在");
            }
            MqttClientConnect clientConnect = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
            if (null != clientConnect && clientConnect.getMqttClient().isConnected()) {
                clientConnect.pub("web_setaiver", "\"" + ver + "\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
        return Result.OK("切换成功");
    }

    @ApiOperation(value = "设备管理-上传算法包文件", notes = "设备管理-上传算法包文件")
    @PostMapping(value = "/uploadAlgFile")
    public Result<?> uploadAlgFile(HttpServletRequest request, HttpServletResponse response) {
        Result<?> result = new Result<>();
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile mf = multipartRequest.getFile("file");
        String retSavePath = "";
        try {
            String ctxPath = fileUploadPath;
            String fileName = null;
            File file = new File(ctxPath + File.separator + File.separator);
            if (!file.exists()) {
                // 创建文件根目录
                file.mkdirs();
            }
            // 获取文件名
            String orgName = mf.getOriginalFilename();
            fileName = CommonUtils.getFileName(orgName);
            String savePath = file.getPath() + File.separator + fileName;
            File savefile = new File(savePath);
            FileCopyUtils.copy(mf.getBytes(), savefile);
            retSavePath = fileName;
            if (retSavePath.contains(SymbolConstant.DOUBLE_BACKSLASH)) {
                retSavePath = retSavePath.replace(SymbolConstant.DOUBLE_BACKSLASH, SymbolConstant.SINGLE_SLASH);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        if (oConvertUtils.isNotEmpty(retSavePath)) {
            result.setMessage(retSavePath);
            result.setSuccess(true);
        } else {
            result.setMessage("上传失败！");
            result.setSuccess(false);
        }
        return result;
    }

    @ApiOperation(value = "设备管理-升级算法包", notes = "设备管理-升级算法包")
    @GetMapping(value = "/updateAlg")
    public Result<String> updateAlg(@RequestParam(name = "id") String deviceId, @RequestParam(name = "path") String path, @RequestParam(name = "type") String upType) {
        String msg = "上传成功";
        DeviceLog devLog = new DeviceLog();
        try {
            DeviceManagement deviceManagement = deviceManagementService.getById(deviceId);
            System.out.println("deviceManagement = " + deviceManagement);
            if (deviceManagement == null) {
                return Result.error("未找到设备信息");
            }
            devLog.setDevid(deviceId);
            LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            devLog.setUpPeople(user.getRealname());
            MqttClientConnect clientConnect = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
            if (null != clientConnect && clientConnect.getMqttClient().isConnected()) {
                redisUtil.set(deviceManagement.getDeviceIp() + "_deviceFtp", 0);
                JSONObject getFtpJson = new JSONObject();
                getFtpJson.put("opt", "getftp");
                getFtpJson.put("sysID", upType);
                getFtpJson.put("fileName", "");
                clientConnect.pub("web_upsoft", getFtpJson.toJSONString(), 1);
                Object obj = redisUtil.get(deviceManagement.getDeviceIp() + "_deviceFtp");
                for (int i = 0; i < 5; i++) { //发送获取ftp信息的mqtt消息后等待10s接收返回数据
                    if (obj instanceof Integer) {
                        Thread.sleep(1000);
                        obj = redisUtil.get(deviceManagement.getDeviceIp() + "_deviceFtp");
                    } else {
                        break;
                    }
                }
                if (obj instanceof Integer) {
                    msg = "获取设备ftp信息失败";
                    updateDownFail(devLog, msg);
                    return Result.error(msg);
                } else {
                    FTPClient ftpClient = new FTPClient();
                    try {
                        JSONObject ftpInfo = (JSONObject) obj;
                        ftpClient.connect(ftpInfo.getString("ip"), ftpInfo.getInteger("port"));
                        ftpClient.login(ftpInfo.getString("user"), ftpInfo.getString("pwd"));
                        if (ftpClient.isConnected()) {
                            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                            File localFile = new File(fileUploadPath + "/" + path);
                            FileInputStream fis = new FileInputStream(localFile);
                            boolean ftpRet = ftpClient.storeFile(localFile.getName(), fis);
                            if (ftpRet) {
                                ftpClient.logout();
                                devLog.setDownStatus(1);
                                devLog.setDownTime(new Date());
                                devLog.setBackupStatus(1);
                                devLog.setBackupTime(new Date());
                                devLog.setUpStatus(1);
                                devLog.setUpTime(new Date());
                                JSONObject upJson = new JSONObject();
                                upJson.put("opt", "update");
                                upJson.put("sysID", upType);
                                upJson.put("fileName", path);
                                clientConnect.pub("web_upsoft", upJson.toJSONString(), 1);
                                devLog.setResultStatus(1);
                                devLog.setResultTime(new Date());
                                deviceLogService.save(devLog);
                            } else {
                                msg = "上传文件失败";
                                updateDownFail(devLog, msg);
                                return Result.error(msg);
                            }
                        }
                    } catch (Exception e) {
                        updateDownFail(devLog, e.getMessage());
                        return Result.error(e.getMessage());
                    } finally {
                        if (ftpClient.isConnected()) {
                            try {
                                ftpClient.disconnect();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else {
                msg = "连接设备失败";
                updateDownFail(devLog, msg);
                return Result.error(msg);
            }
        } catch (Exception e) {
            deviceLogService.save(devLog);
            return Result.error(e.getMessage());
        }
        return Result.OK(msg);
    }

    private void updateDownFail(DeviceLog devLog, String msg) {
        devLog.setDownTime(new Date());
        devLog.setDownStatus(0);
        devLog.setDownDes(msg);
        devLog.setUpStatus(0);
        devLog.setBackupStatus(0);
        devLog.setResultStatus(0);
        deviceLogService.save(devLog);
    }


    /**
     * 获取设备license
     *
     * @param deviceId
     * @return
     */
    @AutoLog(value = "获取设备机器码")
    @ApiOperation(value = "获取设备机器码", notes = "获取设备机器码")
    @GetMapping(value = "/getMachineId")
    public Result<?> getMachineId(@RequestParam(name = "devId", required = true) String deviceId) {
        DeviceManagement deviceManagement = deviceManagementService.getById(deviceId);
        String msg = "已获取设备机器码";
//		if (deviceManagement.getStatus().equals("0")){
//			return Result.error("该设备不在线，请检查连接状态！");
//		}
        try {
            MqttClientConnect client = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
            if (null != client) {
                JSONObject info = new JSONObject();
                info.put("opt", "authorize");
                info.put("action_id", "0");
                client.pub("authorize", info.toJSONString());
                log.info("mqtt发送信息：获取设备机器码" + info);
            }
        } catch (MqttException e) {
            e.printStackTrace();
            msg = "mqtt连接异常";
            return Result.error(msg);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("machineId", deviceManagement.getMachineId());
        return Result.OK(msg, jsonObject);
    }

    /**
     * 获取设备license
     *
     * @param deviceId
     * @return
     */
    @AutoLog(value = "获取设备认证信息")
    @ApiOperation(value = "获取设备认证信息", notes = "获取设备认证信息")
    @GetMapping(value = "/getAuthInfo")
    public Result<?> getAuthInfo(@RequestParam(name = "devId", required = true) String deviceId) {
        String msg = "已获取设备认证信息";
        DeviceManagement deviceManagement = deviceManagementService.getById(deviceId);
//		if (deviceManagement.getStatus().equals("0")){
//			return Result.error("该设备不在线，请检查连接状态！");
//		}
        try {
            MqttClientConnect client = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
            if (null != client) {
                JSONObject info = new JSONObject();
                info.put("opt", "authorize");
                info.put("action_id", "1");
                client.pub("authorize", info.toJSONString());
                log.info("mqtt发送信息：获取设备认证信息" + info);
            }
        } catch (MqttException e) {
            e.printStackTrace();
            msg = "mqtt连接异常";
            return Result.error(msg);
        }
        return Result.OK(msg);
    }

    @ApiOperation(value = "上传license文件", notes = "上传license文件")
    @GetMapping(value = "/uploadLicense")
    public Result<String> uploadLicense(@RequestParam(name = "id") String deviceId, @RequestParam(name = "path") String path) {
        String msg = "上传成功";
        DeviceLog devLog = new DeviceLog();
        try {
            DeviceManagement deviceManagement = deviceManagementService.getById(deviceId);
            if (deviceManagement == null) {
                return Result.error("未找到设备信息");
            }
            devLog.setDevid(deviceId);
            LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            devLog.setUpPeople(user.getRealname());
            MqttClientConnect clientConnect = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
            if (null != clientConnect && clientConnect.getMqttClient().isConnected()) {
                redisUtil.set(deviceManagement.getDeviceIp() + "_deviceFtp", 0);
                JSONObject getFtpJson = new JSONObject();
                getFtpJson.put("opt", "getftp");
                getFtpJson.put("fileName", "");
                clientConnect.pub("web_upsoft", getFtpJson.toJSONString(), 1);
                Object obj = redisUtil.get(deviceManagement.getDeviceIp() + "_deviceFtp");
                for (int i = 0; i < 5; i++) { //发送获取ftp信息的mqtt消息后等待10s接收返回数据
                    if (obj instanceof Integer) {
                        Thread.sleep(1000);
                        obj = redisUtil.get(deviceManagement.getDeviceIp() + "_deviceFtp");
                    } else {
                        break;
                    }
                }
                if (obj instanceof Integer) {
                    msg = "获取设备ftp信息失败";
                    updateDownFail(devLog, msg);
                    return Result.error(msg);
                } else {
                    FTPClient ftpClient = new FTPClient();
                    try {
                        JSONObject ftpInfo = (JSONObject) obj;
                        ftpClient.connect(ftpInfo.getString("ip"), ftpInfo.getInteger("port"));
                        ftpClient.login(ftpInfo.getString("user"), ftpInfo.getString("pwd"));
                        log.info("ftp设备ip：" + ftpInfo.getString("ip") + "，端口：" + ftpInfo.getInteger("port") + "，连接状态：" + ftpClient.isConnected());
                        if (ftpClient.isConnected()) {
                            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                            File localFile = new File(fileUploadPath + "/" + path);
                            FileInputStream fis = new FileInputStream(localFile);
//							ftpClient.setControlEncoding("UTF-8");
//							ftpClient.enterLocalPassiveMode();
                            boolean ftpRet = ftpClient.storeFile(localFile.getName(), fis);
                            log.info("ftpRet状态：" + ftpRet);
                            if (ftpRet) {
                                ftpClient.logout();
                                devLog.setDownStatus(1);
                                devLog.setDownTime(new Date());
                                devLog.setBackupStatus(1);
                                devLog.setBackupTime(new Date());
                                devLog.setUpStatus(1);
                                devLog.setUpTime(new Date());
                                JSONObject upJson = new JSONObject();
                                upJson.put("opt", "authorize");
                                upJson.put("action_id", "2");
                                clientConnect.pub("authorize", upJson.toJSONString(), 1);
                                devLog.setResultStatus(1);
                                devLog.setResultTime(new Date());
                                deviceLogService.save(devLog);
                                JSONObject checkJson = new JSONObject();
                                checkJson.put("opt", "authorize");
                                checkJson.put("action_id", "1");
                                clientConnect.pub("authorize", checkJson.toJSONString(), 1);
                                if (ftpClient.isConnected()) {
                                    try {
                                        ftpClient.disconnect();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                            } else {
                                msg = "上传文件失败";
                                updateDownFail(devLog, msg);
                                return Result.error(msg);
                            }
                        }
                    } catch (Exception e) {
                        updateDownFail(devLog, e.getMessage());
                        return Result.error("设备ftp端口异常：" + e.getMessage());
                    }
                    DeviceManagement dev = deviceManagementService.getById(deviceId);
//                    if (dev.getLicenseStatus().equals("0")) {
//                        msg = dev.getLicenseError();
//                        devLog.setResultDes(msg);
//                        devLog.setDownTime(new Date());
//                        devLog.setUpStatus(0);
//                        devLog.setUpDes(msg);
//                        devLog.setBackupStatus(0);
//                        devLog.setResultStatus(0);
//                        deviceLogService.updateById(devLog);
//                        return Result.error("认证错误：" + dev.getLicenseError());
                    //ERROR_NO "ERROR_NO" /* 没有错误 */
                    //ERROR_INTERNAL "ERROR_INTERNAL" /* 程序内部出错，申请内存失败或加载公钥失败 */
                    //ERROR_OPENFILE "ERROR_OPENFILE" /* 打开文件失败，文件不存在或无读权限 */
                    //ERROR_FILEFORMAT "ERROR_FILEFORMAT" /* 文件格式错误 */
                    //ERROR_MACHINEID "ERROR_MACHINEID" /* 当前机器ID与证书机器ID不符 */
                    //ERROR_TIMERANGE "ERROR_TIMERANGE" /* 时间范围错误，证书尚未生效或已经过期 */
                    //ERROR_SIGNATURE "ERROR_SIGNATURE" /* 签名错误，证书被篡改 *
//                    }
                }
            } else {
                msg = "连接设备失败";
                updateDownFail(devLog, msg);
                return Result.error(msg);
            }
        } catch (Exception e) {
            deviceLogService.save(devLog);
            return Result.error(e.getMessage());
        }
        System.out.println(msg);
        return Result.OK(msg);
    }

    @ApiOperation(value = "调用设备接口", notes = "调用设备接口")
    @GetMapping(value = "/devGetRequest")
    public Result<?> devGetRequest(@RequestParam(name = "devIp") String devIp) {
//		String requestUrl = "http://10.10.10.68:8081" +"/kyscene/deviceManagement/deviceManagement/getConfigInfo?devIp="+devIp;
        String requestUrl = "http://" + devIp + ":8088" + "/kyscene/deviceManagement/deviceManagement/getConfigInfo?devIp=" + devIp;
        System.out.println("requestUrl = " + requestUrl);
        String msg = "";
        try {
            // 创建 URL 对象
            URL url = new URL(requestUrl);
            // 创建 HttpURLConnection 对象
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置请求方法为 GET
            connection.setRequestMethod("GET");
            // 发送 GET 请求
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            // 读取响应内容
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            // 打印响应内容
//			System.out.println("Response Body: " + response.toString());
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
            CloudConfig cloudConfig = gson.fromJson(response.toString(), CloudConfig.class);
//			System.out.println("cloudConfig = " + cloudConfig);
            msg = this.configSync(cloudConfig).getMessage();
            // 关闭连接
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(msg);
        }
        return Result.OK(msg);
    }


    @ApiOperation(value = "向下同步接口", notes = "向下同步接口")
    @GetMapping(value = "/devPostRequest")
    public Result<?> devPostRequest(@RequestParam(name = "devIp") String devIp) {
        CloudConfig cloudConfig = (CloudConfig) getConfigInfo(devIp);
//		System.out.println(cloudConfig);
        log.info("向下同步获取到的数据：" + cloudConfig);
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        String json = gson.toJson(cloudConfig);
//		String requestUrl = "http://10.10.10.68:8081"+"/kyscene/deviceManagement/deviceManagement/devSync" ;
        String requestUrl = "http://" + devIp + ":8088" + "/kyscene/deviceManagement/deviceManagement/devSync";
        String msg = "";

        try {
            // 创建 URL 对象
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            // 将 JSON 字符串作为请求的正文内容发送
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
//			outputStream.write(json.getBytes());
            outputStream.flush();
            outputStream.close();

            // 发起请求
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // 打印响应结果
                JSONObject jsonObject = JSONObject.parseObject(response.toString());
                msg = jsonObject.getString("message");
                System.out.println("msg:" + msg);
            } else {
                System.out.println("POST request failed.");
            }
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(msg);
        }
        return Result.OK(msg);
    }

    // TODO: 2023/7/7  云端同步方法测试
    @ApiOperation(value = "获取所有需要同步的信息", notes = "获取所有需要同步的信息")
    @GetMapping(value = "/getConfigInfo")
    public Object getConfigInfo(@RequestParam(name = "devIp") String devIp) {
        CloudConfig cloudConfig = new CloudConfig();
        System.out.println("传来的ip：" + devIp);
        DeviceManagement deviceManagement = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda().eq(DeviceManagement::getDeviceIp, devIp));
        if (deviceManagement == null) {
            log.info("此设备云端与边端IP不一致，请检查");
            return Result.error("此设备云端与边端IP不一致，请检查");
        }
        String devId = deviceManagement.getId();
        System.out.println("设备id：" + devId);
        List<KyCategory> areaList = kyCategoryService.list().stream().filter(ky -> ky.getCategory().equals("1")).collect(Collectors.toList());
        List<String> cameraLocationIdList = cameraManagerService.getListByDeviceId(devId).stream().map(camera -> camera.getLocation()).distinct().collect(Collectors.toList());
        List<KyCategory> cameraLocationList = new ArrayList<>();
        for (String locationId : cameraLocationIdList) {
            List<KyCategory> kyCategories = kyCategoryService.getTreeLocationById(locationId);
            for (KyCategory ky : kyCategories) {
                cameraLocationList.add(ky);
            }
        }
        List<KyCategory> alarmTypeList = kyCategoryService.list().stream().filter(ky -> ky.getCategory().equals("0")).collect(Collectors.toList());
        List<CameraFactory> cameraFactoryList = cameraFactoryService.list();
        List<CameraManager> cameraManagerList = cameraManagerService.list();
        List<CameraManager> cameraByDeviceIdList = cameraManagerService.getListByDeviceId(devId);
        List<AlgorithmManager> algorithmManagerList = algorithmManagerService.alarmTypeInfoList(); //特殊接口所有算法信息外加入算法的中文名，去匹配云端算法的id，放入算法表
        List<AlgorithmManager> cloudAlgorithmManagerList = algorithmManagerService.getListByDevId(devId);
        List<CameraGroup> cameraGroupList = cameraGroupService.list();
        List<CameraGroupCamera> cameraGroupCameraList = cameraGroupCameraService.list();
        List<CameraGroupCamera> cameraGroupCameraByDeviceIdList = cameraGroupCameraService.getByDeviceId(devId);
        List<CameraGroupAlgorithm> cameraGroupAlgorithmList = cameraGroupAlgorithmService.alarmTypeInfoList();
        cloudConfig.setAreaList(areaList);
        cloudConfig.setAlarmTypeList(alarmTypeList);
        cloudConfig.setCameraFactoryList(cameraFactoryList);
        cloudConfig.setCameraManagerList(cameraManagerList);
        cloudConfig.setAlgorithmManagerList(algorithmManagerList);
        cloudConfig.setCameraGroupList(cameraGroupList);
        cloudConfig.setCameraGroupCameraList(cameraGroupCameraList);
        cloudConfig.setCameraGroupAlgorithmList(cameraGroupAlgorithmList);
        cloudConfig.setDeviceIp(devIp);
        cloudConfig.setDeviceId(devId);
        cloudConfig.setCameraLocationList(cameraLocationList);
        cloudConfig.setCameraByDeviceIdList(cameraByDeviceIdList);
        cloudConfig.setCameraGroupCameraByDeviceIdList(cameraGroupCameraByDeviceIdList);
        cloudConfig.setCloudAlgorithmManagerList(cloudAlgorithmManagerList);
//		configSync(cloudConfig);
        return cloudConfig;
    }

    @ApiOperation(value = "云端同步方法", notes = "云端同步方法")
    public Result<?> configSync(CloudConfig cloudConfig) {
        String msg = "同步成功";
        System.out.println("进");
//		System.out.println(cloudConfig);
        log.info("云端同步获取到的设备上数据：" + cloudConfig);
//		CloudConfig cloudConfig = map.get("configInfo");
        String deviceIp = cloudConfig.getDeviceIp();
        DeviceManagement cloudDeviceInfo = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda().eq(DeviceManagement::getDeviceIp, deviceIp));
        String deviceId = cloudDeviceInfo.getId();
        System.out.println("deviceIp = " + deviceIp);
        if (deviceIp == null) {
            return Result.error("此设备云端与边端IP不一致，请检查");
        }
        try {
            //设备上需要同步的信息列表
            List<KyCategory> deviceAreaList = cloudConfig.getAreaList();
            List<KyCategory> deviceAlarmTypeList = cloudConfig.getAlarmTypeList();
            List<CameraFactory> deviceCameraFactoryList = cloudConfig.getCameraFactoryList();
            List<CameraManager> deviceCameraManagerList = cloudConfig.getCameraManagerList();
            List<AlgorithmManager> deviceAlgorithmManagerList = cloudConfig.getAlgorithmManagerList();
            List<CameraGroup> deviceCameraGroupList = cloudConfig.getCameraGroupList();
            List<CameraGroupCamera> deviceCameraGroupCameraList = cloudConfig.getCameraGroupCameraList();
            List<CameraGroupAlgorithm> deviceCameraGroupAlgorithmList = cloudConfig.getCameraGroupAlgorithmList();
            //云端上需要比对的信息列表
            //告警类型id列表
            List<String> alarmTypeIdList = kyCategoryService.list().stream()
                    .filter(ky -> ky.getCategory().equals("0"))
                    .map(KyCategory::getId)
                    .collect(Collectors.toList());
            //告警类型name列表
            List<String> alarmTypeNameList = kyCategoryService.list().stream()
                    .filter(ky -> ky.getCategory().equals("0"))
                    .map(KyCategory::getName)
                    .collect(Collectors.toList());
            //自定义分组id列表
            List<String> areaIdList = kyCategoryService.list().stream()
                    .filter(ky -> ky.getCategory().equals("1"))
                    .map(KyCategory::getId)
                    .collect(Collectors.toList());
            //自定义分组name列表
            List<String> areaNameList = kyCategoryService.list().stream()
                    .filter(ky -> ky.getCategory().equals("1"))
                    .map(KyCategory::getName)
                    .collect(Collectors.toList());
            //相机基础信息型号列表
            List<String> cameraFactoryTypeList = cameraFactoryService.list().stream()
                    .map(CameraFactory::getCameraType)
                    .collect(Collectors.toList());
            //相机基础信息id列表
            List<String> cameraFactoryIdList = cameraFactoryService.list().stream()
                    .map(CameraFactory::getId)
                    .collect(Collectors.toList());
            //相机id列表
            List<String> cameraManagerIdList = cameraManagerService.list().stream()
                    .map(CameraManager::getId)
                    .collect(Collectors.toList());
            //相机name列表
            List<String> cameraManagerNameList = cameraManagerService.list().stream()
                    .map(CameraManager::getCameraName)
                    .collect(Collectors.toList());
            //算法id列表
            List<String> algorithmManagerIdList = algorithmManagerService.list().stream()
                    .map(AlgorithmManager::getId)
                    .collect(Collectors.toList());
            //相机组id列表
            List<String> cameraGroupIdList = cameraGroupService.list().stream()
                    .map(CameraGroup::getId)
                    .collect(Collectors.toList());
            //相机组name列表
            List<String> cameraGroupNameList = cameraGroupService.list().stream()
                    .map(CameraGroup::getGroupName)
                    .collect(Collectors.toList());
            //相机组相机id列表
            List<String> cameraGroupCameraIdList = cameraGroupCameraService.list().stream()
                    .map(CameraGroupCamera::getId)
                    .collect(Collectors.toList());
            //相机组算法id列表
            List<String> cameraGroupAlgorithmIdList = cameraGroupAlgorithmService.list().stream()
                    .map(CameraGroupAlgorithm::getId)
                    .collect(Collectors.toList());

            //根据ip同步设备id
//		DeviceManagement cloudDevice = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda().eq(DeviceManagement::getDeviceIp,deviceIp));
//		if (cloudDevice!=null) {
//			deviceManagementService.updateIdByIp(deviceIp,deviceId);
//		}
            //1.比对设备上的自定义分组id是否存在云端，如果不存在则新增，并且分组名后加上对应设备的ip，存在则不进行任何操作
//			List<String> deviceAreaIdList = deviceAreaList.stream().map(area->area.getId()).collect(Collectors.toList());
//			List<String> areaReduceList = areaIdList.stream().filter(item -> !deviceAreaIdList.contains(item)).collect(Collectors.toList()); //差集
//			System.out.println("云area："+areaIdList);
//			System.out.println("设备area："+deviceAreaIdList);
//			System.out.println("差集area = " + areaReduceList);
            //不能删除 无法确定是哪台设备的信息
            for (KyCategory deviceArea : deviceAreaList) {
                if (!areaIdList.contains(deviceArea.getId())) {
                    if (areaNameList.contains(deviceArea.getName())) {
                        deviceArea.setName(deviceArea.getName() + "_" + deviceIp);
                    }
                    kyCategoryService.save(deviceArea);
                } else {
                    kyCategoryService.updateById(deviceArea);
                }
            }

            //比对设备上的告警类型，id和name同时不存在才新增
            for (KyCategory deviceAlarmType : deviceAlarmTypeList) {
                if (!alarmTypeIdList.contains(deviceAlarmType.getId())) {
                    if (!alarmTypeNameList.contains(deviceAlarmType.getName())) {
                        kyCategoryService.save(deviceAlarmType);
                    }
                }
            }
            //2.先比对设备上的相机基础信息id，再比对型号名称CameraType是否存在云端，如果不存在则新增，存在则更新
            for (CameraFactory cameraFactory : deviceCameraFactoryList) {
                if (!cameraFactoryIdList.contains(cameraFactory.getId())) { //id不存在且相机型号不存在
                    if (!cameraFactoryTypeList.contains(cameraFactory.getCameraType())) {
                        cameraFactoryService.save(cameraFactory);
                    }
                } else {
                    cameraFactoryService.updateById(cameraFactory); //id存在则更新
                }
            }
            //3.比对相机列表 id不同名称相同 做处理
            //云端同设备下的相机列表id和设备的相机列表id，比差删除云端相机
            List<String> cameraManagerList = cameraManagerService.getListByDeviceId(deviceId).stream().map(camera -> camera.getId()).collect(Collectors.toList());
            List<String> deviceCameraIdList = deviceCameraManagerList.stream().map(camera -> camera.getId()).collect(Collectors.toList());
            List<String> cameraReduceList = cameraManagerList.stream().filter(item -> !deviceCameraIdList.contains(item)).collect(Collectors.toList()); //差集
//		System.out.println("云："+cameraManagerList);
//		System.out.println("设备："+deviceCameraIdList);
//		System.out.println("差集："+cameraReduceList);
            for (CameraManager camera : deviceCameraManagerList) {
//				System.out.println(camera.getCameraOwnership());
                //特殊处理：a.相机类型不需要处理，b.相机归属，需要根据ip获得云端所同步设备的id。
                DeviceManagement deviceManagement = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>()
                        .lambda()
                        .eq(DeviceManagement::getDeviceIp, deviceIp));
//				System.out.println("名字："+cameraManagerNameList);
                if (deviceManagement != null) {
                    camera.setCameraOwnership(deviceManagement.getId());
                }
                //比差删除云端相机
                if (!cameraManagerIdList.contains(camera.getId())) {
                    if (cameraManagerNameList.contains(camera.getCameraName())) {
                        camera.setCameraName(camera.getCameraName() + '_' + deviceIp);
                    }
                    cameraManagerService.save(camera);
                } else {
                    //方式update覆盖已经加了设备ip的名字，直接采用删除后校验再添加
                    cameraManagerService.removeById(camera);
                    cameraManagerNameList = cameraManagerService.list().stream()
                            .map(CameraManager::getCameraName)
                            .collect(Collectors.toList());
                    if (cameraManagerNameList.contains(camera.getCameraName())) {
                        camera.setCameraName(camera.getCameraName() + '_' + deviceIp);
                    }
                    cameraManagerService.save(camera);
                }
            }
            for (String cameraId : cameraReduceList) {
                cameraManagerService.removeById(cameraId);
            }

            //4.比对轮询组列表 轮询组根据另外两个表存ids所以不操作删除 只置为空
            List<String> groupIdList = cameraGroupService.list().stream().map(group -> group.getId()).collect(Collectors.toList());
            List<String> deviceGroupIpList = deviceCameraGroupList.stream().map(group -> group.getId()).collect(Collectors.toList());
//		List<String> groupReduceList = groupIdList.stream().filter(item -> !deviceGroupIpList.contains(item)).collect(Collectors.toList()); //差集
            for (CameraGroup group : deviceCameraGroupList) {
                if (!cameraGroupIdList.contains(group.getId())) {
                    if (cameraGroupNameList.contains(group.getGroupName())) {
                        group.setGroupName(group.getGroupName() + "_" + deviceIp);
                    }
                    cameraGroupService.save(group);
                } else {
                    cameraGroupService.removeById(group);
                    cameraGroupNameList = cameraGroupService.list().stream()
                            .map(CameraGroup::getGroupName)
                            .collect(Collectors.toList());
                    if (cameraGroupNameList.contains(group.getGroupName())) {
                        group.setGroupName(group.getGroupName() + "_" + deviceIp);
                    }
                    cameraGroupService.save(group);
                }
            }

            //5.比对轮询组相机列表
            List<String> groupCameraIdList = cameraGroupCameraService.list().stream().map(camera -> camera.getId()).collect(Collectors.toList());
            List<String> deviceGroupCameraIdList = deviceCameraGroupCameraList.stream().map(camera -> camera.getId()).collect(Collectors.toList());
            List<String> groupCameraReduceList = groupCameraIdList.stream().filter(item -> !deviceGroupCameraIdList.contains(item)).collect(Collectors.toList()); //差集

            for (CameraGroupCamera groupCamera : deviceCameraGroupCameraList) {
                if (!cameraGroupCameraIdList.contains(groupCamera.getId())) {
                    cameraGroupCameraService.save(groupCamera);
                } else {
                    cameraGroupCameraService.updateById(groupCamera);
                }
            }
            for (String groupCameraId : groupCameraReduceList) {
                CameraGroupCamera camera = cameraGroupCameraService.getById(groupCameraId);
                DeviceManagement device = deviceManagementService.getByCameraId(camera.getCameraId());
                if (device != null) {
                    if (device.getDeviceIp().equals(deviceIp)) {
                        cameraGroupCameraService.removeById(groupCameraId);
                    }
                }
            }
            //6.比对轮询组算法列表
            List<String> groupAlgIdList = cameraGroupAlgorithmService.list().stream().map(alg -> alg.getId()).collect(Collectors.toList());
            List<String> deviceGroupAlgIdList = deviceCameraGroupAlgorithmList.stream().map(alg -> alg.getId()).collect(Collectors.toList());
            List<String> groupAlgReduceList = groupAlgIdList.stream().filter(item -> !deviceGroupAlgIdList.contains(item)).collect(Collectors.toList()); //差集
//		System.out.println("云："+groupAlgIdList);
//		System.out.println("设备："+deviceGroupAlgIdList);
//		System.out.println("差集："+groupAlgReduceList);
            for (CameraGroupAlgorithm groupAlgorithm : deviceCameraGroupAlgorithmList) {
                KyCategory kyCategory = kyCategoryService.getOne(new QueryWrapper<KyCategory>()
                        .lambda().eq(KyCategory::getId, groupAlgorithm.getModelName()));
//				System.out.println(kyCategory);
                if (kyCategory != null) {
                    groupAlgorithm.setModelName(kyCategory.getId());
                    groupAlgorithm.setAlarmType(kyCategory.getId());
                    groupAlgorithm.setAlgorithmId(kyCategory.getId());
                }
                if (!cameraGroupAlgorithmIdList.contains(groupAlgorithm.getId())) {
                    cameraGroupAlgorithmService.save(groupAlgorithm);
                } else {
                    cameraGroupAlgorithmService.updateById(groupAlgorithm);
                }
            }
            //比对差集并且数据所同步设备的相机下的算法
            for (String groupAlgId : groupAlgReduceList) {
                CameraGroupAlgorithm cameraGroupAlgorithm = cameraGroupAlgorithmService.getById(groupAlgId);
                List<String> algorithmManagerList = algorithmManagerService
                        .getByAlgGroupId(cameraGroupAlgorithm.getGroupId(), cameraGroupAlgorithm.getModelName()).stream().map(alg -> alg.getBelongTo()).collect(Collectors.toList());
//				System.out.println("algorithmManagerList = " + algorithmManagerList);
                for (String belong : algorithmManagerList) {
                    DeviceManagement device = deviceManagementService.getByCameraId(belong);
//				System.out.println("device.getDeviceIp() = " + device.getDeviceIp());
                    if (device != null) {
                        if (device.getDeviceIp().equals(deviceIp)) {
                            cameraGroupAlgorithmService.removeById(groupAlgId);
                        }
                    }
                }
            }

            //最后操作算法
            //7.比对算法列表 根据算法的中文名，去匹配云端算法的id，放入算法表
            //比对差集并且数据所同步设备的相机下的算法
            List<String> algIdList = algorithmManagerService.list().stream().map(alg -> alg.getId()).collect(Collectors.toList());
            List<String> deviceAlgIdList = deviceAlgorithmManagerList.stream().map(alg -> alg.getId()).collect(Collectors.toList());
            List<String> algReduceList = algIdList.stream().filter(item -> !deviceAlgIdList.contains(item)).collect(Collectors.toList()); //差集

            for (AlgorithmManager alg : deviceAlgorithmManagerList) {
                KyCategory kyCategory = kyCategoryService.getOne(new QueryWrapper<KyCategory>()
                        .lambda().eq(KyCategory::getId, alg.getModelName()));
                if (kyCategory != null) {
                    alg.setModelName(kyCategory.getId());
                    alg.setAlarmType(kyCategory.getId());
                }
                if (!algorithmManagerIdList.contains(alg.getId())) {
                    algorithmManagerService.save(alg);
                } else {
                    algorithmManagerService.updateById(alg);
                }
            }
            //比对差集并且数据所同步设备的相机下的算法
            for (String algId : algReduceList) {
                DeviceManagement device = deviceManagementService.getByAlgorithmId(algId);
                if (device != null) {
                    if (device.getDeviceIp().equals(deviceIp)) {
                        algorithmManagerService.removeById(algId);
                    }
                }
            }


            //遍历轮询组
            List<CameraGroup> cameraGroupList = cameraGroupService.list();
            for (CameraGroup group : cameraGroupList) {
                String cameraIds = cameraGroupCameraService.getCameraIds(group.getId());
//			System.out.println("cameraIds = " + cameraIds);
                String algIds = cameraGroupCameraService.getAlgIds(group.getId());
//			System.out.println("algIds = " + algIds);
                if (cameraIds == null) {
                    cameraIds = "";
                }
                if (algIds == null) {
                    algIds = "";
                }
                group.setCameraIds(cameraIds);
                group.setAlgorithmIds(algIds);
                if (group.getCameraIds().equals("") || group.getAlgorithmIds().equals("")) {
                    group.setRunStatus(0);
                }
                cameraGroupService.updateById(group);
                if (group.getCameraIds().equals("") && group.getAlgorithmIds().equals("")) {
                    cameraGroupService.removeById(group);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage(), e);
            msg = "同步失败";
            return Result.error(msg);
        }
        return Result.OK(msg);
    }

    @ApiOperation(value = "设备同步方法", notes = "设备同步方法")
    @PostMapping(value = "/devSync")
    public Result<?> devSync(@RequestBody CloudConfig cloudConfig) {
        String msg = "同步成功";
        System.out.println("进向下同步");
//		System.out.println(cloudConfig);
//		CloudConfig cloudConfig = map.get("configInfo");
        String deviceIp = cloudConfig.getDeviceIp();
        DeviceManagement cloudDeviceInfo = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda().eq(DeviceManagement::getDeviceIp, deviceIp));
        String deviceId = cloudDeviceInfo.getId();
        System.out.println("deviceIp = " + deviceIp);
        if (deviceIp == null) {
            return Result.error("此设备云端与边端IP不一致，请检查");
        }
        try {
            //设备上需要同步的信息列表
            List<KyCategory> deviceAreaList = cloudConfig.getAreaList();
            List<KyCategory> deviceAlarmTypeList = cloudConfig.getAlarmTypeList();
            List<CameraFactory> deviceCameraFactoryList = cloudConfig.getCameraFactoryList();
            List<CameraManager> deviceCameraManagerList = cloudConfig.getCameraManagerList();
            List<AlgorithmManager> deviceAlgorithmManagerList = cloudConfig.getAlgorithmManagerList();
            List<CameraGroup> deviceCameraGroupList = cloudConfig.getCameraGroupList();
            List<CameraGroupCamera> deviceCameraGroupCameraList = cloudConfig.getCameraGroupCameraList();
            List<CameraGroupAlgorithm> deviceCameraGroupAlgorithmList = cloudConfig.getCameraGroupAlgorithmList();

            //云端上需要比对的信息列表
            //告警类型id列表
            List<String> alarmTypeIdList = kyCategoryService.list().stream()
                    .filter(ky -> ky.getCategory().equals("0"))
                    .map(KyCategory::getId)
                    .collect(Collectors.toList());
            //告警类型name列表
            List<String> alarmTypeNameList = kyCategoryService.list().stream()
                    .filter(ky -> ky.getCategory().equals("0"))
                    .map(KyCategory::getName)
                    .collect(Collectors.toList());
            //自定义分组id列表
            List<String> areaIdList = kyCategoryService.list().stream()
                    .filter(ky -> ky.getCategory().equals("1"))
                    .map(KyCategory::getId)
                    .collect(Collectors.toList());
            //自定义分组name列表
            List<String> areaNameList = kyCategoryService.list().stream()
                    .filter(ky -> ky.getCategory().equals("1"))
                    .map(KyCategory::getName)
                    .collect(Collectors.toList());
            //相机基础信息型号列表
            List<String> cameraFactoryTypeList = cameraFactoryService.list().stream()
                    .map(CameraFactory::getCameraType)
                    .collect(Collectors.toList());
            //相机基础信息id列表
            List<String> cameraFactoryIdList = cameraFactoryService.list().stream()
                    .map(CameraFactory::getId)
                    .collect(Collectors.toList());
            //相机id列表
            List<String> cameraManagerIdList = cameraManagerService.list().stream()
                    .map(CameraManager::getId)
                    .collect(Collectors.toList());
            //相机name列表
            List<String> cameraManagerNameList = cameraManagerService.list().stream()
                    .map(CameraManager::getCameraName)
                    .collect(Collectors.toList());
            //算法id列表
            List<String> algorithmManagerIdList = algorithmManagerService.list().stream()
                    .map(AlgorithmManager::getId)
                    .collect(Collectors.toList());
            //相机组id列表
            List<String> cameraGroupIdList = cameraGroupService.list().stream()
                    .map(CameraGroup::getId)
                    .collect(Collectors.toList());
            //相机组name列表
            List<String> cameraGroupNameList = cameraGroupService.list().stream()
                    .map(CameraGroup::getGroupName)
                    .collect(Collectors.toList());
            //相机组相机id列表
            List<String> cameraGroupCameraIdList = cameraGroupCameraService.list().stream()
                    .map(CameraGroupCamera::getId)
                    .collect(Collectors.toList());
            //相机组算法id列表
            List<String> cameraGroupAlgorithmIdList = cameraGroupAlgorithmService.list().stream()
                    .map(CameraGroupAlgorithm::getId)
                    .collect(Collectors.toList());

            //根据ip同步设备id
//		DeviceManagement cloudDevice = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda().eq(DeviceManagement::getDeviceIp,deviceIp));
//		if (cloudDevice!=null) {
//			deviceManagementService.updateIdByIp(deviceIp,deviceId);
//		}
            //1.比对设备上的自定义分组id是否存在云端，如果不存在则新增，并且分组名后加上对应设备的ip，存在则不进行任何操作
            List<KyCategory> cameraLocationList = cloudConfig.getCameraLocationList();
            List<String> cameraLocationIdList = cameraLocationList.stream().map(ky -> ky.getId()).distinct().collect(Collectors.toList());
            List<String> areaReduceList = areaIdList.stream().filter(item -> !cameraLocationIdList.contains(item)).collect(Collectors.toList()); //差集
//			System.out.println("云端分组 = " + cameraLocationIdList);
//			System.out.println("当前分组 = " + areaIdList);

            //不能删除 无法确定是哪台设备的信息
            for (KyCategory deviceArea : cameraLocationList) {
                if (!areaIdList.contains(deviceArea.getId())) {
                    if (areaNameList.contains(deviceArea.getName())) {
                        deviceArea.setName(deviceArea.getName() + "_" + deviceIp);
                    }
                    kyCategoryService.save(deviceArea);
                } else {
                    kyCategoryService.updateById(deviceArea);
                }
            }

            //2.先比对设备上的相机基础信息id，再比对型号名称CameraType是否存在云端，如果不存在则新增，存在则更新
            for (CameraFactory cameraFactory : deviceCameraFactoryList) {
                if (!cameraFactoryIdList.contains(cameraFactory.getId())) { //id不存在且相机型号不存在
                    if (!cameraFactoryTypeList.contains(cameraFactory.getCameraType())) {
                        cameraFactoryService.save(cameraFactory);
                    }
                } else {
                    cameraFactoryService.updateById(cameraFactory); //id存在则更新
                }
            }
            //3.比对相机列表 id不同名称相同 做处理
            //云端传下来的相机
            List<CameraManager> cameraByDeviceIdList = cloudConfig.getCameraByDeviceIdList();
            List<String> deviceCameraIdList = new ArrayList<>();
            for (CameraManager cam : cameraByDeviceIdList) {
                deviceCameraIdList.add(cam.getId());
            }
            List<String> cameraReduceList = cameraManagerIdList.stream().filter(item -> !deviceCameraIdList.contains(item)).collect(Collectors.toList()); //差集
            for (CameraManager camera : cameraByDeviceIdList) {
                //特殊处理：a.相机类型不需要处理，b.相机归属，需要根据ip获得云端所同步设备的id。
                DeviceManagement deviceManagement = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>()
                        .lambda()
                        .eq(DeviceManagement::getDeviceIp, deviceIp));
//				System.out.println("名字："+cameraManagerNameList);
                if (deviceManagement != null) {
                    camera.setCameraOwnership(deviceManagement.getId());
                }
                //比差删除相机
                if (!cameraManagerIdList.contains(camera.getId())) {
                    if (cameraManagerNameList.contains(camera.getCameraName())) {
                        camera.setCameraName(camera.getCameraName() + '_' + deviceIp);
                    }
                    cameraManagerService.save(camera);
                } else {
                    //方式update覆盖已经加了设备ip的名字，直接采用删除后校验再添加
                    cameraManagerService.removeById(camera);
                    cameraManagerNameList = cameraManagerService.list().stream()
                            .map(CameraManager::getCameraName)
                            .collect(Collectors.toList());
                    if (cameraManagerNameList.contains(camera.getCameraName())) {
                        camera.setCameraName(camera.getCameraName() + '_' + deviceIp);
                    }
                    cameraManagerService.save(camera);
                }
            }
            for (String cameraId : cameraReduceList) {
                cameraManagerService.removeById(cameraId);
            }

            //4.比对轮询组列表 轮询组根据另外两个表存ids所以不操作删除 只置为空
            List<String> groupIdList = cameraGroupService.list().stream().map(group -> group.getId()).collect(Collectors.toList());
            List<String> deviceGroupIpList = deviceCameraGroupList.stream().map(group -> group.getId()).collect(Collectors.toList());
//		List<String> groupReduceList = groupIdList.stream().filter(item -> !deviceGroupIpList.contains(item)).collect(Collectors.toList()); //差集
            for (CameraGroup group : deviceCameraGroupList) {
                if (!cameraGroupIdList.contains(group.getId())) {
                    if (cameraGroupNameList.contains(group.getGroupName())) {
                        group.setGroupName(group.getGroupName() + "_" + deviceIp);
                    }
                    cameraGroupService.save(group);
                } else {
                    cameraGroupService.removeById(group);
                    cameraGroupNameList = cameraGroupService.list().stream()
                            .map(CameraGroup::getGroupName)
                            .collect(Collectors.toList());
                    if (cameraGroupNameList.contains(group.getGroupName())) {
                        group.setGroupName(group.getGroupName() + "_" + deviceIp);
                    }
                    cameraGroupService.save(group);
                }
            }

            //5.比对轮询组相机列表
            List<CameraGroupCamera> cameraGroupCameraByDeviceIdList = cloudConfig.getCameraGroupCameraByDeviceIdList();
            List<String> deviceGroupCameraIdlist = new ArrayList();
            for (CameraGroupCamera groupCamera : cameraGroupCameraByDeviceIdList) {
                deviceGroupCameraIdlist.add(groupCamera.getId());
            }
            List<String> groupCameraReduceList = cameraGroupCameraIdList.stream().filter(item -> !deviceGroupCameraIdlist.contains(item)).collect(Collectors.toList()); //差集
//			System.out.println("云："+deviceGroupCameraIdlist);
//			System.out.println("设备："+cameraGroupCameraIdList);
//			System.out.println("差集："+groupCameraReduceList);
            for (CameraGroupCamera groupCamera : cameraGroupCameraByDeviceIdList) {
                if (!cameraGroupCameraIdList.contains(groupCamera.getId())) {
                    cameraGroupCameraService.save(groupCamera);
                } else {
                    cameraGroupCameraService.updateById(groupCamera);
                }
            }
            for (String groupCameraId : groupCameraReduceList) {
                cameraGroupCameraService.removeById(groupCameraId);
            }


            //6.比对轮询组算法列表
            List<String> groupAlgIdList = cameraGroupAlgorithmService.list().stream().map(alg -> alg.getId()).collect(Collectors.toList());
            List<String> deviceGroupAlgIdList = deviceCameraGroupAlgorithmList.stream().map(alg -> alg.getId()).collect(Collectors.toList());
            List<String> groupAlgReduceList = groupAlgIdList.stream().filter(item -> !deviceGroupAlgIdList.contains(item)).collect(Collectors.toList()); //差集
//		System.out.println("设备："+groupAlgIdList);
//		System.out.println("云："+deviceGroupAlgIdList);
//		System.out.println("差集："+groupAlgReduceList);
            for (CameraGroupAlgorithm groupAlgorithm : deviceCameraGroupAlgorithmList) {
                KyCategory kyCategory = kyCategoryService.getOne(new QueryWrapper<KyCategory>()
                        .lambda().eq(KyCategory::getId, groupAlgorithm.getModelName()));
//				System.out.println(kyCategory);
                if (kyCategory != null) {
                    groupAlgorithm.setModelName(kyCategory.getId());
                    groupAlgorithm.setAlarmType(kyCategory.getId());
                    groupAlgorithm.setAlgorithmId(kyCategory.getId());
                }
                if (!cameraGroupAlgorithmIdList.contains(groupAlgorithm.getId())) {
                    cameraGroupAlgorithmService.save(groupAlgorithm);
                } else {
                    cameraGroupAlgorithmService.updateById(groupAlgorithm);
                }
            }

            //比对差集并且数据所同步设备的相机下的算法
            for (String groupAlgId : groupAlgReduceList) {
                cameraGroupAlgorithmService.removeById(groupAlgId);
            }

            //最后操作算法
            //7.比对算法列表 根据算法的中文名，去匹配云端算法的id，放入算法表
            //比对差集并且数据所同步设备的相机下的算法

            //云端对应此设备的算法
            List<AlgorithmManager> cloudAlgorithmManagerList = cloudConfig.getCloudAlgorithmManagerList();
            List<String> cloudAlgIdList = new ArrayList<>();
            for (AlgorithmManager alg : cloudAlgorithmManagerList) {
                cloudAlgIdList.add(alg.getId());
            }
            List<String> algReduceList = algorithmManagerIdList.stream().filter(item -> !cloudAlgIdList.contains(item)).collect(Collectors.toList()); //差集
            for (AlgorithmManager alg : cloudAlgorithmManagerList) {
                KyCategory kyCategory = kyCategoryService.getOne(new QueryWrapper<KyCategory>()
                        .lambda().eq(KyCategory::getId, alg.getModelName()));
                if (kyCategory != null) {
                    alg.setModelName(kyCategory.getId());
                    alg.setAlarmType(kyCategory.getId());
                }
                if (!algorithmManagerIdList.contains(alg.getId())) {
                    algorithmManagerService.save(alg);
                } else {
                    algorithmManagerService.updateById(alg);
                }
            }
            //比对差集并且数据所同步设备的相机下的算法
            for (String algId : algReduceList) {
                algorithmManagerService.removeById(algId);
            }
            //遍历轮询组
            List<CameraGroup> cameraGroupList = cameraGroupService.list();
            for (CameraGroup group : cameraGroupList) {
                String cameraIds = cameraGroupCameraService.getCameraIds(group.getId());
                String algIds = algorithmManagerService.getAlgIdsByGroupId(group.getId()); //从算法总表里取算法ids
                if (cameraIds == null) {
                    cameraIds = "";
                }
                if (algIds == null) {
                    algIds = "";
                }
                group.setCameraIds(cameraIds);
                group.setAlgorithmIds(algIds);
                if (group.getCameraIds().equals("") || group.getAlgorithmIds().equals("")) {
                    group.setRunStatus(0);
                }
                cameraGroupService.updateById(group);
                if (group.getCameraIds().equals("") && group.getAlgorithmIds().equals("")) {
                    cameraGroupService.removeById(group);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage(), e);
            msg = "同步失败";
            return Result.error(msg);
        }
        return Result.OK(msg);
    }

    /**
     * 获取设备sn码
     */
    @GetMapping(value = "/getDeviceSn")
    public Result<?> getDeviceSn() {
        JSONObject result = new JSONObject();
        String deviceSn = SysParameterUtil.getDeviceSn();
        if (StrUtil.isNotBlank(deviceSn)) {
            result.put("deviceSn", deviceSn);
            return Result.OK(result);
        }
        return Result.error("获取设备sn码失败，请手动配置");
    }

    /**
     * 获取项目key
     */
    @GetMapping(value = "/getProjectKey")
    public Result<?> getProjectKey() {
        JSONObject result = new JSONObject();
        String projectKey = SysParameterUtil.getProjectCode();
        if (StrUtil.isNotBlank(projectKey)) {
            result.put("projectKey", projectKey);
            return Result.OK(result);
        }
        return Result.error("获取项目key失败，请手动配置");
    }

    /**
     * 项目编码改变后，重启cjzk mqtt
     */
    @GetMapping("/restartCustomMqtt")
    public Result<?> restartCustomMqtt() {
        String projectKey = SysParameterUtil.getProjectCode();
        if (StrUtil.isNotBlank(projectKey)) {
            try {
                MqttConfigProperties mqttConfigProperties = SpringContextUtils.getBean(MqttConfigProperties.class);
                MqttClientService.getInstance().closeMqttClient(mqttConfigProperties.getHost());

                // 5秒后重启
                Timer  timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        deviceManagementService.initCustomMqtt();
                    }
                }, 5000);
                return Result.OK();
            }catch (Exception e) {
                return Result.error("重启失败");
            }
        }
        return Result.OK();
    }
}
