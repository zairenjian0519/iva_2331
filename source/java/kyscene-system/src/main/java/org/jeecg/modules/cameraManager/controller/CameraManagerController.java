package org.jeecg.modules.cameraManager.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.vo.DictModel;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.algorithmmanager.service.IAlgorithmManagerService;
import org.jeecg.modules.cameraFactory.entity.CameraFactory;
import org.jeecg.modules.cameraFactory.service.ICameraFactoryService;
import org.jeecg.modules.cameraGroup.entity.CameraGroup;
import org.jeecg.modules.cameraGroup.service.ICameraGroupService;
import org.jeecg.modules.cameraManager.entity.*;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.cameraManager.service.impl.AsyncCameraManagerService;
import org.jeecg.modules.cameraManager.utils.CameraStatusCheckerNew;
import org.jeecg.modules.configjson.Const;
import org.jeecg.modules.configjson.CameraDeploymentConfigService;
import org.jeecg.modules.configjson.CreateConfigJson;
import org.jeecg.modules.configjson.CreateConfigJsonService;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.iot.DeviceEnum;
import org.jeecg.modules.iot.IotMsgEntity;
import org.jeecg.modules.iot.mqtt.IotMqttCustomOptions;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.mqtt.handler.AlarmHandleUtil;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.service.ISysDictService;
import org.jeecg.modules.system.util.CmdUti;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Description: 相机管理
 * @Author: jeecg-boot
 * @Date: 2022-08-23
 * @Version: V1.0
 */
@Api(tags = "相机管理")
@RestController
@RequestMapping("/cameraManager/cameraManager")
@Slf4j
public class CameraManagerController extends JeecgController<CameraManager, ICameraManagerService> {
    @Autowired
    private ICameraManagerService cameraManagerService;

    @Autowired
    private IKyCategoryService kyCategoryService;

    @Autowired
    private IDeviceManagementService deviceManagementService;

    @Autowired
    private IAlarmDetailService alarmDetailService;

    @Autowired
    private IAlgorithmManagerService algorithmManagerService;

    @Autowired
    private ISysDictService sysDictService;

    @Autowired
    private ICameraFactoryService cameraFactoryService;

    @Autowired
    private CreateConfigJson createConfigJson;

    @Autowired
    private CreateConfigJsonService createConfigJsonService;

    @Autowired
    private CameraDeploymentConfigService cameraDeploymentConfigService;

    @Autowired
    private ICameraGroupService cameraGroupService;

    @Autowired
    private AsyncCameraManagerService asyncCameraManagerService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 分页列表查询
     *
     * @param cameraManager
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    //@AutoLog(value = "相机管理-分页列表查询")
    @ApiOperation(value = "相机管理-分页列表查询", notes = "相机管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(CameraManager cameraManager,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        Page<CameraManager> page = new Page<CameraManager>(pageNo, pageSize);
        IPage<CameraManager> pageList = cameraManagerService.pageList(page, cameraManager);
        for (CameraManager camera : pageList.getRecords()) {
            String areaString = kyCategoryService.getAreaListByAreaId(camera.getLocation());
            camera.setLocationDict(areaString);
        }
        return Result.OK(pageList);
    }

    @ApiOperation(value = "查询可配算法的相机数量", notes = "查询可配算法的相机数量")
    @GetMapping(value = "/getAlgLicenseNum")
    public Result<Object> getCameraAlgLicenseNum(CameraManager cameraManager) {
        DeviceManagement dev = null;
        /* 算法实例页面 传相机id查设备信息*/
        if (cameraManager.getId() != null) {
            dev = deviceManagementService.getByCameraId(cameraManager.getId());
        }
        /* 相机配置页面 传相机归属id查设备信息*/
        if (cameraManager.getCameraOwnership() != null) {
            dev = deviceManagementService.getById(cameraManager.getCameraOwnership());
        }
        JSONObject jsonObject = new JSONObject();
        if (dev != null) {
            int canUseNum = dev.getAlgorithmCameraNum();
            int num = algorithmManagerService.getCameraLicense(dev.getId());
            jsonObject.put("allNum", canUseNum);
            jsonObject.put("usedNum", num);
            jsonObject.put("canUseNum", canUseNum - num);
        }
        return Result.OK(jsonObject);
    }

    @GetMapping(value = "/getOnLineCameraCount")
    public Result<Integer> getOnLineCameraCount(CameraManager cameraManager) {
        return Result.OK(cameraManagerService.onLineCameraCount(cameraManager));
    }

    @GetMapping(value = "/checkLicense")
    public Result<Boolean> checkLicense(@RequestParam(name = "id", required = true) String id) {
        DeviceManagement d = deviceManagementService.getById(id);
        if (d.getCameraNum() == null) {
            return Result.ok(false);
        }
        List<CameraManager> list = cameraManagerService.getListByDeviceId(id);
        if (d.getCameraNum() > list.size()) {
            return Result.ok(true);
        }
        return Result.ok(false);
    }


    @ApiOperation(value = "相机管理-区域相机树", notes = "相机管理-区域相机树")
    @GetMapping(value = "/cameraTree")
    public Result<?> getTree() {
        LinkedList<LocationTreeVo> treeList = new LinkedList();
        HashMap<String, LocationTreeVo> locationTreeVoMap = new HashMap<String, LocationTreeVo>();
        String defaultPid = "0";

        // 创建图标
        IconInfoVo factoryIconInfoVo = new IconInfoVo();
        factoryIconInfoVo.setIcon("location_parent_icon_info");

        IconInfoVo shopIconInfoVo = new IconInfoVo();
        shopIconInfoVo.setIcon("device_icon_info");

        IconInfoVo deviceIconInfoVo = new IconInfoVo();
        deviceIconInfoVo.setIcon("device_icon_info");

        IconInfoVo cameraIconInfoVo = new IconInfoVo();
        cameraIconInfoVo.setIcon("camera_icon_info");


        // 创建默认分组
        LocationTreeVo defaultLocationTreeVo = new LocationTreeVo();
        defaultLocationTreeVo.setKey(defaultPid);
        defaultLocationTreeVo.setTitle("默认厂区");
        defaultLocationTreeVo.setSlots(factoryIconInfoVo);
        defaultLocationTreeVo.setType("area");
        locationTreeVoMap.put(defaultPid, defaultLocationTreeVo);

        //获取位置分类 位置分类有多级 递归查询
        List<KyCategory> record = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda()
                .eq(KyCategory::getCategory, 1));

        for (KyCategory kyCategory : record) {
            LocationTreeVo locationTreeVo = new LocationTreeVo();
            locationTreeVo.setKey(kyCategory.getId());
            locationTreeVo.setTitle(kyCategory.getName());
            locationTreeVo.setType("area");
            //获取树信息 查询数量
            List<String> treeIds = cameraManagerService.getTreeId(kyCategory.getId());
            //对应地点的相机数量
            Integer cameraCount = cameraManagerService.getCountNum(treeIds);
            locationTreeVo.setCameraCount(cameraCount);
            //对应设备的相机数量
            int offLineCount = 0;
            List<CameraManager> cameraManagerList = cameraManagerService.list(new QueryWrapper<CameraManager>().lambda()
                    .eq(CameraManager::getLocation, kyCategory.getId()));
            //计算相机离线数量
            for (CameraManager camera : cameraManagerList) {
                if (StringUtils.isEmpty(camera.getCameraStatus()) || !camera.getCameraStatus().equals("1")) {
                    ++offLineCount;
                }
            }
            locationTreeVo.setOffLineCount(offLineCount);
            locationTreeVoMap.put(kyCategory.getId(), locationTreeVo);

            if (kyCategory.getPid().equals("0")) {
                locationTreeVo.setSlots(factoryIconInfoVo);
                treeList.add(locationTreeVo);
                continue;
            }
            LocationTreeVo parent = locationTreeVoMap.get(kyCategory.getPid());
            locationTreeVo.setParentNode(parent.getTitle());
            locationTreeVo.setParentKey(parent.getKey());
            locationTreeVo.setSlots(shopIconInfoVo);
            this.getLocationChildren(parent).add(locationTreeVo);
        }

        //获取设备分类 由于设备最高只到二级 无需递归
        List<DeviceTreeVo> deviceTreeList = new LinkedList<>();
        Map<String, DeviceTreeVo> deviceTreeMap = new LinkedHashMap<>();
        List<DeviceManagement> deviceList = deviceManagementService.list();
        for (DeviceManagement device : deviceList) {
            DeviceTreeVo deviceTreeVo = new DeviceTreeVo();
            deviceTreeVo.setTitle(device.getDeviceName());
            deviceTreeVo.setKey(device.getId());
            deviceTreeVo.setSlots(deviceIconInfoVo);
            deviceTreeVo.setType("device");
            //离线数量
            int offLineCount = 0;
            List<CameraManager> cameraManagerList = cameraManagerService.list(new QueryWrapper<CameraManager>().lambda()
                    .eq(CameraManager::getCameraOwnership, device.getId()));
            //计算相机离线数量
            for (CameraManager camera : cameraManagerList) {
                if (StringUtils.isEmpty(camera.getCameraStatus()) || !camera.getCameraStatus().equals("1")) {
                    ++offLineCount;
                }
            }
            //设备对应相机的总数量
            deviceTreeVo.setCameraCount(cameraManagerList.size());
            deviceTreeVo.setOffLineCount(offLineCount);
            //相机新告警数 在设备树上按类别显示数量
            int alarmDeviceCount = alarmDetailService.getAlarmDeviceList(device.getId());
            deviceTreeVo.setAlarmCount(alarmDeviceCount);
            if (alarmDeviceCount == 0) {
                deviceTreeVo.setHaveAlarm(false);
            } else {
                deviceTreeVo.setHaveAlarm(true);
            }
            deviceTreeList.add(deviceTreeVo);
            deviceTreeMap.put(device.getId(), deviceTreeVo);
        }


        List<CameraManager> cameraList = cameraManagerService.list();
        //获取设备对应的相机列表
        for (CameraManager cameraManager : cameraList) {
            DeviceManagement deviceManagement = deviceManagementService.getByCameraId(cameraManager.getId());
            // 按位置分组
            LocationTreeVo location = this.getCameraLocation(locationTreeVoMap, cameraManager.getLocation());
            if (location != null) {
                LocationTreeVo locationTreeVo = new LocationTreeVo();
                locationTreeVo.setKey(cameraManager.getId());
                locationTreeVo.setDeviceIp(deviceManagement.getDeviceIp());
                locationTreeVo.setType("camera");
                locationTreeVo.setSlots(cameraIconInfoVo);
                locationTreeVo.setTitle(cameraManager.getCameraName());
                locationTreeVo.setParentNode(location.getTitle());
                locationTreeVo.setParentKey(location.getKey());
                locationTreeVo.setStreamAddress(cameraManager.getStreamAddress());
                locationTreeVo.setRunStatus(cameraManager.getCameraStatus()); //runStatus改为cameraStatus 相机状态locationTreeVo
                String picPath = cameraManager.getPic();
                locationTreeVo.setPic(picPath);
                this.getLocationChildren(location).add(locationTreeVo);
            }

            // 按设备分组
            DeviceTreeVo device = deviceTreeMap.get(cameraManager.getCameraOwnership());
            if (device != null) {
                DeviceTreeVo deviceTreeVo = new DeviceTreeVo();
                deviceTreeVo.setDeviceIp(deviceManagement.getDeviceIp());
                deviceTreeVo.setKey(cameraManager.getId());
                deviceTreeVo.setType("camera");
                deviceTreeVo.setSlots(cameraIconInfoVo);
                deviceTreeVo.setParentNode(device.getTitle());
                deviceTreeVo.setParentKey(device.getKey());
                deviceTreeVo.setRunStatus(cameraManager.getCameraStatus()); //runStatus改为cameraStatus 相机状态
                deviceTreeVo.setTitle(cameraManager.getCameraName());
                deviceTreeVo.setStreamAddress(cameraManager.getStreamAddress());
                String picPath = cameraManager.getPic();
                deviceTreeVo.setPic(picPath);
                if (device.getChildren() == null) {
                    device.setChildren(new LinkedList<>());
                }
                this.getDeviceChildren(device).add(deviceTreeVo);
            }
        }
        for (LocationTreeVo location : treeList) {
            //相机新告警数 在区域树上按类别显示数量
            int alarmAreaCount = alarmDetailService.getAlarmAreaList(location.getKey());
            location.setAlarmCount(alarmAreaCount);
            if (alarmAreaCount == 0) {
                location.setHaveAlarm(false);
            } else {
                location.setHaveAlarm(true);
            }
        }
        if (defaultLocationTreeVo.getChildren() != null) {
            treeList.addFirst(defaultLocationTreeVo);
        }
        //归整返回
        CameraTreeVo cameraTreeVo = new CameraTreeVo();
        cameraTreeVo.setLocationTree(treeList);
        cameraTreeVo.setDeviceTree(deviceTreeList);
        return Result.OK(cameraTreeVo);
    }

    //相机配置模块 需要各级数据 不要相机
    @ApiOperation(value = "相机管理-无相机树", notes = "相机管理-无相机树")
    @GetMapping(value = "/cameraGroup")
    public Result<?> cameraGroup() {
        LinkedList<LocationTreeVo> treeList = new LinkedList();
        HashMap<String, LocationTreeVo> locationTreeVoMap = new HashMap<String, LocationTreeVo>();
        String defaultPid = "0";
        // 创建图标
        IconInfoVo factoryIconInfoVo = new IconInfoVo();
        factoryIconInfoVo.setIcon("location_parent_icon_info");

        IconInfoVo shopIconInfoVo = new IconInfoVo();
        shopIconInfoVo.setIcon("device_icon_info");

        IconInfoVo deviceIconInfoVo = new IconInfoVo();
        deviceIconInfoVo.setIcon("device_icon_info");


        // 创建默认分组
        LocationTreeVo defaultLocationTreeVo = new LocationTreeVo();
        defaultLocationTreeVo.setKey(defaultPid);
        defaultLocationTreeVo.setTitle("默认厂区");
        defaultLocationTreeVo.setSlots(factoryIconInfoVo);
        defaultLocationTreeVo.setType("area");
        locationTreeVoMap.put(defaultPid, defaultLocationTreeVo);

        //获取区域分组
        List<KyCategory> record = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda()
                .eq(KyCategory::getCategory, 1));
        for (KyCategory kyCategory : record) {
            LocationTreeVo locationTreeVo = new LocationTreeVo();
            locationTreeVo.setKey(kyCategory.getId());
            locationTreeVo.setTitle(kyCategory.getName());
            locationTreeVo.setType("area");
            //对应地点的相机数量
            //获取树信息 查询数量
            List<String> treeIds = cameraManagerService.getTreeId(kyCategory.getId());
            //对应地点的相机数量
            Integer cameraCount = cameraManagerService.getCountNum(treeIds);
            locationTreeVo.setCameraCount(cameraCount);
            locationTreeVoMap.put(kyCategory.getId(), locationTreeVo);
            if (kyCategory.getPid().equals("0")) {
                locationTreeVo.setSlots(factoryIconInfoVo);
                treeList.add(locationTreeVo);
                continue;
            }
            LocationTreeVo parent = locationTreeVoMap.get(kyCategory.getPid());
            locationTreeVo.setParentNode(parent.getTitle());
            locationTreeVo.setParentKey(parent.getKey());
            locationTreeVo.setSlots(shopIconInfoVo);
            this.getLocationChildren(parent).add(locationTreeVo);
        }

        //获取设备分类 由于设备最高只到二级 无需递归
        List<DeviceTreeVo> deviceTreeList = new LinkedList<>();
        Map<String, DeviceTreeVo> deviceTreeMap = new LinkedHashMap<>();
        List<DeviceManagement> deviceList = deviceManagementService.list();
        int allCount = 0; //计算相机总数
        for (DeviceManagement device : deviceList) {
            DeviceTreeVo deviceTreeVo = new DeviceTreeVo();
            deviceTreeVo.setTitle(device.getDeviceName());
            deviceTreeVo.setKey(device.getId());
            deviceTreeVo.setSlots(deviceIconInfoVo);
            deviceTreeVo.setType("device");
            //对应设备的相机数量
            int offLineCount = 0;
            List<CameraManager> cameraManagerList = cameraManagerService.list(new QueryWrapper<CameraManager>().lambda()
                    .eq(CameraManager::getCameraOwnership, device.getId()));
            //计算相机离线数量
            for (CameraManager camera : cameraManagerList) {
                if (StringUtils.isEmpty(camera.getCameraStatus()) || !camera.getCameraStatus().equals("1")) {
                    ++offLineCount;
                }
            }
            allCount += cameraManagerList.size();
            deviceTreeVo.setCameraCount(cameraManagerList.size());
            deviceTreeVo.setCameraNum(device.getCameraNum());
            deviceTreeVo.setOffLineCount(offLineCount);
            deviceTreeList.add(deviceTreeVo);
            deviceTreeMap.put(device.getId(), deviceTreeVo);
        }

        if (defaultLocationTreeVo.getChildren() != null) {
            treeList.addFirst(defaultLocationTreeVo);
        }
        //归整返回
        CameraTreeVo cameraTreeVo = new CameraTreeVo();
        cameraTreeVo.setLocationTree(treeList);
        cameraTreeVo.setDeviceTree(deviceTreeList);
        cameraTreeVo.setAllCameraCount(allCount);
        return Result.OK(cameraTreeVo);
    }

    private LinkedList<LocationTreeVo> getLocationChildren(LocationTreeVo location) {
        if (location.getChildren() == null) {
            location.setChildren(new LinkedList<>());
        }
        return location.getChildren();
    }

    private LinkedList<DeviceTreeVo> getDeviceChildren(DeviceTreeVo location) {
        if (location.getChildren() == null) {
            location.setChildren(new LinkedList<>());
        }
        return location.getChildren();
    }

    private LocationTreeVo getCameraLocation(HashMap<String, LocationTreeVo> locationTreeVoMap, String locationID) {
        // 子级直接返回
        if (locationID == null || locationID == "") {
            return locationTreeVoMap.get("0");
        }
        LocationTreeVo location = locationTreeVoMap.get(locationID);
        return location;
    }
    // 暂时保留
    // if (location.getParentKey() != null) {
    //     return location;
    // }
    // // 获取默认分组
    // LocationTreeVo defaultLocation = locationTreeVoMap.get("0" + locationID);
    // if (defaultLocation != null) {
    //     return defaultLocation;
    // }
    // // 若不存在创建默认组
    // defaultLocation = new LocationTreeVo();
    // defaultLocation.setKey("0" + location.getKey());
    // defaultLocation.setTitle("默认车间");
    // defaultLocation.setType("area");
    // IconInfoVo shopIconInfoVo = new IconInfoVo();
    // shopIconInfoVo.setIcon("device_icon_info");
    // defaultLocation.setSlots(shopIconInfoVo);
    // getLocationChildren(location).addFirst(defaultLocation);
    // locationTreeVoMap.put(defaultLocation.getKey(), defaultLocation);
    // return defaultLocation;
//	}

    //获取位置的子节点
//	private LinkedList<LocationTreeVo> getLocationChild(String id, List<KyCategory> record, String title, String key, List<CameraManager> cameraList) {
//		LinkedList<LocationTreeVo> childrenList = new LinkedList();
//		for (KyCategory ky : record) {
//			if (id.equals(ky.getPid())) {
//				LocationTreeVo locationTreeVo = new LocationTreeVo();
//				IconInfoVo iconInfoVo = new IconInfoVo();
//				iconInfoVo.setIcon("location_icon_info");
//				locationTreeVo.setKey(ky.getId());
//				locationTreeVo.setTitle(ky.getName());
//				locationTreeVo.setSlots(iconInfoVo);
//				locationTreeVo.setType("area");
//				locationTreeVo.setParentNode(title);
//				locationTreeVo.setParentKey(key);
//				//相机新告警数 在区域树上按类别显示数量
//				List<AlarmDetail> alarmAreaList = alarmDetailService.getAlarmAreaList(ky.getId());
//				locationTreeVo.setAlarmCount(alarmAreaList.size());
//				if (alarmAreaList.size() == 0) {
//					locationTreeVo.setHaveAlarm(false);
//				} else {
//					locationTreeVo.setHaveAlarm(true);
//				}
//				locationTreeVo.setChildren(getLocationChild(ky.getId(), record, title, key, cameraList));
//				childrenList.add(locationTreeVo);
//			}
//		}
//		//获取位置子节点对应的相机列表
////		List<CameraManager> cameraList = cameraManagerService.list();
//		for (LocationTreeVo vo : childrenList) {
//			LinkedList<LocationTreeVo> list = new LinkedList();
//			for (CameraManager cameraManager : cameraList) {
//				if (vo.getKey().equals(cameraManager.getLocation())) {
//					LocationTreeVo locationTreeVo = new LocationTreeVo();
//					IconInfoVo iconInfoVo = new IconInfoVo();
//					iconInfoVo.setIcon("camera_icon_info");
//					locationTreeVo.setKey(cameraManager.getId());
//					locationTreeVo.setType("camera");
//					locationTreeVo.setParentNode(vo.getTitle());
//					locationTreeVo.setParentKey(vo.getKey());
//					locationTreeVo.setSlots(iconInfoVo);
//					locationTreeVo.setTitle(cameraManager.getCameraName());
//					list.add(locationTreeVo);
//					vo.setChildren(list);
//				}
//			}
//		}
//		return childrenList;
//	}


//	//该接口树没有相机数据（暂时保留）
//	@ApiOperation(value = "相机管理-设备、区域下拉框", notes = "相机管理-设备、区域下拉框")
//	@GetMapping(value = "/cameraGroup")
//	public Result<?> cameraGroup() {
//		List<CameraManager> cameraList = cameraManagerService.list();
//		List<LocationTreeVo> treeList = new LinkedList();
//		//获取告警分类 告警分类有多级 递归查询
//		List<KyCategory> record = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda()
//				.eq(KyCategory::getCategory, 1));
//		for (KyCategory kyCategory : record) {
//			if (kyCategory.getPid().equals("0")) {
//				LocationTreeVo locationTreeVo = new LocationTreeVo();
//				IconInfoVo iconInfoVo = new IconInfoVo();
//				iconInfoVo.setIcon("location_parent_icon_info");
//				locationTreeVo.setKey(kyCategory.getId());
//				locationTreeVo.setTitle(kyCategory.getName());
//				locationTreeVo.setSlots(iconInfoVo);
//				locationTreeVo.setType("area");
//				locationTreeVo.setChildren(getChild(kyCategory.getId(), record, locationTreeVo.getTitle(), locationTreeVo.getKey()));
//				treeList.add(locationTreeVo);
//			}
//		}
//
//		//获取设备分类 由于设备最高只到二级 无需递归
//		List<DeviceTreeVo> deviceTreeList = new LinkedList<>();
//		List<DeviceManagement> deviceList = deviceManagementService.list();
//		for (DeviceManagement device : deviceList) {
//			DeviceTreeVo deviceTreeVo = new DeviceTreeVo();
//			IconInfoVo iconInfoVo = new IconInfoVo();
//			iconInfoVo.setIcon("device_icon_info");
//			deviceTreeVo.setTitle(device.getDeviceName());
//			deviceTreeVo.setKey(device.getId());
//			deviceTreeVo.setSlots(iconInfoVo);
//			deviceTreeVo.setType("device");
//			//对应设备的相机数量
//			List<CameraManager> cameraManagerList = cameraManagerService.list(new QueryWrapper<CameraManager>().lambda()
//					.eq(CameraManager::getCameraOwnership, device.getId()));
//			deviceTreeVo.setCameraCount(cameraManagerList.size());
//			deviceTreeList.add(deviceTreeVo);
//		}
//		//归整返回
//		CameraTreeVo cameraTreeVo = new CameraTreeVo();
//		cameraTreeVo.setLocationTree(treeList);
//		cameraTreeVo.setDeviceTree(deviceTreeList);
//		return Result.OK(cameraTreeVo);
//	}
//
//	//获取位置的子节点
//	private LinkedList<LocationTreeVo> getChild(String id, List<KyCategory> record, String title, String key) {
//		LinkedList<LocationTreeVo> childrenList = new LinkedList();
//		for (KyCategory ky : record) {
//			if (id.equals(ky.getPid())) {
//				LocationTreeVo locationTreeVo = new LocationTreeVo();
//				IconInfoVo iconInfoVo = new IconInfoVo();
//				iconInfoVo.setIcon("location_icon_info");
//				locationTreeVo.setKey(ky.getId());
//				locationTreeVo.setTitle(ky.getName());
//				locationTreeVo.setSlots(iconInfoVo);
//				locationTreeVo.setType("area");
//				locationTreeVo.setParentNode(title);
//				locationTreeVo.setParentKey(key);
//				//相机新告警数 在区域树上按类别显示数量
//				List<CameraManager> cameraManagerList = cameraManagerService.list(new QueryWrapper<CameraManager>().lambda()
//						.eq(CameraManager::getLocation, ky.getId()));
//				locationTreeVo.setCameraCount(cameraManagerList.size());
//				locationTreeVo.setChildren(getChild(ky.getId(), record, title, key));
//				childrenList.add(locationTreeVo);
//			}
//		}
//		return childrenList;
//	}


    /**
     * 添加相机
     *
     * @param cameraManager 相机信息
     * @return Result
     */
    @AutoLog(value = "相机管理-添加")
    @ApiOperation(value = "相机管理-添加", notes = "相机管理-添加")
//	@RequiresPermissions("camera:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody CameraManager cameraManager) {
        if (StrUtil.isBlank(cameraManager.getLocation())) cameraManager.setLocation("0");
        //相机配置页面不允许配置相同ip的相机
        // if (StrUtil.isNotBlank(cameraManager.getIp())) { //手动输入地址时 相机ip为空
        //     CameraFactory cameraFactory = cameraFactoryService.getOne(Wrappers.lambdaQuery(CameraFactory.class).eq(CameraFactory::getId, cameraManager.getCameraType()), false);
        //     if (!"NVR".equals(cameraFactory.getCameraType())) {
        //         List<CameraManager> cameraManagerList = cameraManagerService.list(new QueryWrapper<CameraManager>().lambda().eq(CameraManager::getIp, cameraManager.getIp()));
        //         if (!cameraManagerList.isEmpty()) {
        //             return Result.error("已存在相同ip的相机，请重新输入。");
        //         }
        //     }
        // }

        long count = cameraManagerService.count(Wrappers.lambdaQuery(CameraManager.class).eq(CameraManager::getStreamAddress, cameraManager.getStreamAddress()));
        if (count > 0) {
            return Result.error("已经存在同样的视频流地址，不要重复添加。");
        }

        //不在按固定规则拼接生成id，会出现问题：删除id为001的相机后再次创建还是001，按相机查询告警时数据会让用户混淆。
        //cameraManager.setId(cameraManagerService.createIdByDevice(cameraManager.getCameraOwnership()));
        int random = (int) (Math.random() * 9 + 1);
        String valueOf = String.valueOf(random);
        int hashCode = UUID.randomUUID().toString().hashCode();
        if (hashCode < 0) {
            hashCode = -hashCode;
        }
        String id = valueOf + String.format("%015d", hashCode);

        cameraManager.setId(id);
        cameraManager.setCameraStatus("4");
        cameraManager.setRunStatus("Y");
        cameraManager.setPic("/cameraImg/" + cameraManager.getId() + ".jpg");
        cameraManager.setStreamAddressPush(cameraManager.getStreamAddressPush());
        cameraManager.setStreamTypePush(cameraManager.getStreamTypePush());
        cameraManager.setStreamFormatPush(cameraManager.getStreamFormatPush());
        // 手动输入不录入ip和端口 需要匹配出来存入
        if (cameraManager.getFactory().equals("other")) {
            String str = cameraManager.getStreamAddress();
            String pattern = "(\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+)";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(str);
            if (m.find()) {
                log.info("手动输入地址，匹配到的 IP 和端口为：" + m.group());
                String[] result = m.group().split(":");
                String ip = result[0];
                Integer port = Integer.parseInt(result[1]);
                cameraManager.setIp(ip);
                cameraManager.setPort(port);
            } else {
                log.error("手动输入的地址没有匹配到 IP 和端口");
                return Result.error("手动输入的地址没有匹配到 IP 和端口");
            }
        }
        if (StrUtil.isBlank(cameraManager.getStreamType())) cameraManager.setStreamType("");
        if (StrUtil.isBlank(cameraManager.getStreamFormat())) cameraManager.setStreamFormat("");

        cameraManagerService.save(cameraManager);
        cacheCameraList();
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param cameraManager 相机信息
     * @return Result
     */
    @AutoLog(value = "相机管理-编辑")
    @ApiOperation(value = "相机管理-编辑", notes = "相机管理-编辑")
//	@RequiresPermissions("camera:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody CameraManager cameraManager) {
        // 手动输入不录入ip和端口 需要匹配出来存入
        if (cameraManager.getFactory().equals("other")) {
            String str = cameraManager.getStreamAddress();
            String pattern = "(\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+)";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(str);
            if (m.find()) {
                log.info("手动输入地址，匹配到的 IP 和端口为：" + m.group());
                String[] result = m.group().split(":");
                String ip = result[0];
                Integer port = Integer.parseInt(result[1]);
                cameraManager.setIp(ip);
                cameraManager.setPort(port);
            } else {
                log.error("手动输入的地址没有匹配到 IP 和端口");
                return Result.error("手动输入的地址没有匹配到 IP 和端口");
            }
        }
        if (StrUtil.isBlank(cameraManager.getStreamType())) cameraManager.setStreamType("");
        if (StrUtil.isBlank(cameraManager.getStreamFormat())) cameraManager.setStreamFormat("");

        cameraManager.setStreamAddressPush(cameraManager.getStreamAddressPush());
        cameraManager.setStreamTypePush(cameraManager.getStreamTypePush());
        cameraManager.setStreamFormatPush(cameraManager.getStreamFormatPush());

        // alarm_details 数据量过大的时候：需要解决 接口响应速度 和 内存问题
        asyncCameraManagerService.updateAlarmDetailsAsync(
                cameraManager.getId(),
                cameraManager.getCameraName(),
                cameraManager.getLocation()
        );
        cameraManagerService.updateById(cameraManager);
        cacheCameraList();
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "相机管理-通过id删除")
    @ApiOperation(value = "相机管理-通过id删除", notes = "相机管理-通过id删除")
//	@RequiresPermissions("camera:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        CameraManager cameraManager = cameraManagerService.getById(id);
        //查询轮训组是否存在此相机
        List<CameraGroup> cameraGroupList = cameraGroupService.list();
        //获取所有相机租的camera_ids
        for (CameraGroup cameraGroup : cameraGroupList) {
            String cameraIds = cameraGroup.getCameraIds();
            String[] split = cameraIds.split(",");
            for (String s : split) {
                if (cameraManager.getId().equals(s)) {
                    return Result.error("轮训配置存在此相机,请先删除轮训组配置");
                }
            }
        }/* 修改BUG删除相机,轮训配置显示未null的BUG*/
        cameraManagerService.removeById(id);
//		cameraManagerService.deleteAlarm(id); //删除相机的时候将对应相机的告警表、审核表信息也删除
        algorithmManagerService.remove(new QueryWrapper<AlgorithmManager>().lambda().eq(AlgorithmManager::getBelongTo, id));
        cacheCameraList();
        afterCameraDeletion(Collections.singleton(id));
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "相机管理-批量删除")
    @ApiOperation(value = "相机管理-批量删除", notes = "相机管理-批量删除")
//	@RequiresPermissions("camera:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        String[] split = ids.split(",");
        for (String id : split) {
            CameraManager cameraManager = cameraManagerService.getById(id);
            //查询轮训组是否存在此相机
            List<CameraGroup> cameraGroupList = cameraGroupService.list();
            //获取所有相机租的camera_ids
            for (CameraGroup cameraGroup : cameraGroupList) {
                String cameraIds = cameraGroup.getCameraIds();
                String[] splitCpoy = cameraIds.split(",");
                for (String s : splitCpoy) {
                    if (cameraManager.getId().equals(s)) {
                        return Result.error("轮训配置存在此相机,请先删除轮训组配置");
                    }
                }
            }/* 修改BUG删除相机,轮训配置显示未null的BUG*/
        }


        this.cameraManagerService.removeByIds(Arrays.asList(ids.split(",")));
        List<String> list = Arrays.asList(ids.split(","));
        for (String id : list) {
//			cameraManagerService.deleteAlarm(id); //删除相机的时候将对应相机的告警表、审核表信息也删除
            algorithmManagerService.remove(new QueryWrapper<AlgorithmManager>().lambda().eq(AlgorithmManager::getBelongTo, id));
        }
        cacheCameraList();
        afterCameraDeletion(list);
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "相机管理-通过id查询")
    @ApiOperation(value = "相机管理-通过id查询", notes = "相机管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<CameraManager> queryById(@RequestParam(name = "id", required = true) String id) {
        CameraManager cameraManager = cameraManagerService.getById(id);
        if (cameraManager == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(cameraManager);
    }

//	/**
//	 * 导出excel
//	 *
//	 * @param request
//	 * @param cameraManager
//	 */
//	@RequiresPermissions("camera:export")
//	@RequestMapping(value = "/exportXls")
//	public ModelAndView exportXls(HttpServletRequest request, CameraManager cameraManager) {
//		return super.exportXls(request, cameraManager, CameraManager.class, "相机管理");
//	}

    /**
     * 导出excel
     *
     * @param request
     * @param cameraManager
     */
//	@RequiresPermissions("camera:export")
    @RequestMapping(value = "/exportXls", method = RequestMethod.GET)
    public ModelAndView exportXls(HttpServletRequest request, CameraManager cameraManager, HttpServletResponse response) {
        //return super.exportXls(request, cameraManager, CameraManager.class, "相机配置");
        ModelAndView mv = new ModelAndView();
        log.info("进入导出方法=========================");
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            //本地环境
			//String path = classLoader.getResource("templates/ExportCameraTemplate.xlsx").toURI().getPath();
            ClassPathResource resource = new ClassPathResource("templates/ExportCameraTemplate.xlsx");
            // String path = exportXlsPath;
            //log.info("导出文件模板路径：" + path);
            //File file = new File(path);
            List<ExportCamera> list = this.cameraManagerService.exportCamera(cameraManager);
            //FileInputStream inputStream = new FileInputStream(file);
            InputStream inputStream = resource.getInputStream();
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
            XSSFRow xssfRow;
            XSSFCell xssfCell;
            for (int i = 0; i < list.size(); i++) {
                ExportCamera c = list.get(i);
                xssfRow = sheet.createRow(i + 1);
                xssfCell = xssfRow.createCell(0);
                xssfCell.setCellValue(c.getCameraName());

                xssfCell = xssfRow.createCell(1);
                xssfCell.setCellValue(c.getDeviceName());

                xssfCell = xssfRow.createCell(2);
                xssfCell.setCellValue(c.getCameraStatus());

                xssfCell = xssfRow.createCell(3);
                xssfCell.setCellValue(c.getLocationName());

                xssfCell = xssfRow.createCell(4);
                xssfCell.setCellValue(c.getUsername());

                xssfCell = xssfRow.createCell(5);
                xssfCell.setCellValue(c.getPassword());

                xssfCell = xssfRow.createCell(6);
                xssfCell.setCellValue(c.getFactoryName());

                xssfCell = xssfRow.createCell(7);
                xssfCell.setCellValue(c.getStreamAddress());

                xssfCell = xssfRow.createCell(8);
                xssfCell.setCellValue(c.getCameraType());

                xssfCell = xssfRow.createCell(9);
                xssfCell.setCellValue(c.getStreamType());

                xssfCell = xssfRow.createCell(10);
                xssfCell.setCellValue(c.getStreamFormat());

                xssfCell = xssfRow.createCell(11);
                if (c.getChannel() == null) {
                    xssfCell.setCellValue("");
                } else {
                    xssfCell.setCellValue(c.getChannel());
                }
                xssfCell = xssfRow.createCell(12);
                if (c.getIp() == null) {
                    xssfCell.setCellValue("");
                } else {
                    xssfCell.setCellValue(c.getIp());
                }

                xssfCell = xssfRow.createCell(13);
                if (c.getPort() == null) {
                    xssfCell.setCellValue("");
                } else {
                    xssfCell.setCellValue(c.getPort());
                }
                xssfCell = xssfRow.createCell(14);
                if (c.getCameraNumber() == null) {
                    xssfCell.setCellValue("");
                } else {
                    xssfCell.setCellValue(c.getCameraNumber());
                }
            }
            // 写入文件
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            xssfWorkbook.write(stream);
            byte[] bytes = stream.toByteArray();
            ByteArrayInputStream inputStr = new ByteArrayInputStream(bytes);
            IoUtil.copy(inputStr, response.getOutputStream());

            // // 设置HTTP响应头，指示返回的是一个Excel文件
            // response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            // response.setHeader("Content-Disposition", "attachment; filename=export_camera.xlsx");
            //
            // // 将Excel文件写入响应流
            // ByteArrayOutputStream stream = new ByteArrayOutputStream();
            // xssfWorkbook.write(stream);
            // byte[] bytes = stream.toByteArray();
            // ByteArrayInputStream inputStr = new ByteArrayInputStream(bytes);
            //
            // // 将文件内容直接写入响应的输出流
            // IoUtil.copy(inputStr, response.getOutputStream());
            // response.flushBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mv;
    }

    /**
     * 添加相机，批量
     *
     * @param cameraList 相机信息列表
     * @return Result
     */
    @AutoLog(value = "相机管理-NVR批量添加")
    @ApiOperation(value = "相机管理-NVR批量添加", notes = "相机管理-NVR批量添加")
//	@RequiresPermissions("camera:add")
    @PostMapping(value = "/addNvrCameras")
    public Result<String> add(@RequestBody List<CameraManager> cameraList) throws Exception {
        for (CameraManager cameraManager : cameraList) {
            String cameraName = cameraManager.getCameraName();
            if (StrUtil.isBlank(cameraName)) return Result.error("相机名称不能为空，请检查");
            if (cameraName.length() > 20) return Result.error(cameraName + " 相机名称过长，请小于20个汉字！");

            //生成id
            String uuId = UUID.randomUUID().toString().replace("-", "");
            cameraManager.setId(uuId);
            // 如果记录存在，使用数据库的id
            CameraManager ca = cameraManagerService.getOne(new QueryWrapper<CameraManager>().lambda().eq(CameraManager::getStreamAddress, cameraManager.getStreamAddress()));
            if (ObjectUtil.isNotEmpty(ca)) cameraManager.setId(ca.getId());

            //图片地址
            cameraManager.setPic("/cameraImg/" + cameraManager.getId() + ".jpg");

            //设置cameraType为cameraFactory的id
            CameraFactory cameraFactory = cameraFactoryService.getOne(Wrappers.lambdaQuery(CameraFactory.class)
                    .eq(CameraFactory::getCameraType, cameraManager.getCameraType())
                    .eq(CameraFactory::getFactoryName, cameraManager.getFactory()), false);
            cameraManager.setCameraType(cameraFactory.getId());
        }
        this.cameraManagerService.saveOrUpdateBatch(cameraList);
        Thread.sleep(1000);
        List<DeviceManagement> deviceManagementList = deviceManagementService.list();
        for (DeviceManagement dev : deviceManagementList) {
            createConfigJson.savaAllConfigByDev(dev.getId());
        }
        cacheCameraList();
        return Result.ok("操作成功！");
    }

    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, @RequestParam(name = "id") String id, HttpServletResponse response) {

        //这一行将HttpServletRequest对象强制转换为MultipartHttpServletRequest，表明请求中包含了多部分数据，可能是文件上传
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //获取请求中上传的文件映射。每个文件由一个MultipartFile对象表示。
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        //遍历上传的文件。在这种情况下，似乎假设只有一个文件，因为它没有区分多个文件。
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            // 获取上传文件对象,->获取当前迭代的MultipartFile对象。
            MultipartFile file = entity.getValue();
            log.info("上传的文件名 : " + file.getName());
            try {
                //声明一个XSSFWorkbook对象，用于表示Excel工作簿。
                XSSFWorkbook xssfWorkbook = null;
                //创建一个CameraManager对象的列表，用于存储解析后的相机配置信息。
                List<CameraManager> returnList = new ArrayList<>();
                //通过文件的输入流创建XSSFWorkbook对象，表示Excel工作簿。
                xssfWorkbook = new XSSFWorkbook(file.getInputStream());
                // 工作簿   获取工作簿的第一个工作表
                XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
                //获取表格的列数，假设表头在第一行。
                int columns = sheet.getRow(0).getPhysicalNumberOfCells();
                if (columns < 14) {//单位不为空解析列小于9 选择单位导入的模板检查
                    //模板错误不进行解析
                    return Result.error("文件导入模板数据错误！");
                }
                //获取工作表的行数。
                int rowNum = sheet.getPhysicalNumberOfRows();
                //遍历每一行，从第二行开始（跳过表头）。
                for (int i = 1; i < rowNum; i++) {
                    //获取当前行。
                    XSSFRow row = sheet.getRow(i);
                    //创建一个CameraManager对象，用于存储当前行的相机配置信息。
                    CameraManager cameraManager = new CameraManager();
                    //相机名称
                    if (row != null && !row.equals("null")) {
                        //获取当前行的第一个单元格。
                        XSSFCell cell = row.getCell(0);
                        if (cell != null) {
                            //将单元格的数据类型设置为字符串类型。
                            cell.setCellType(CellType.STRING);
                            if (StringUtils.isNotEmpty(cell.getStringCellValue())) {
                                //获取相机名称的长度。
                                Integer length = cell.getStringCellValue().length();
                                if (length > 20) {
                                    return Result.error("第" + i + "条数据相机名称过长，请小于20个汉字！");
                                }
                                CameraManager ca = cameraManagerService.getOne(new QueryWrapper<CameraManager>().lambda().eq(CameraManager::getCameraName, cell.getStringCellValue()));
                                if (ObjectUtil.isNotEmpty(ca)) {
                                    continue;
                                }
                                cameraManager.setCameraName(cell.getStringCellValue());
                            } else {
                                return Result.error("第" + i + "条数据相机名称为空，请检查！");
                            }
                            //归属设备
                            XSSFCell cell1 = row.getCell(1);
                            if (cell1 != null) {
                                cell1.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell1.getStringCellValue())) {
                                    Integer length = cell1.getStringCellValue().length();
                                    if (length > 50) {
                                        return Result.error("第" + i + "条数据归属设备名称过长，请小于50个汉字！");
                                    }
                                    String deviceName = cell1.getStringCellValue();
                                    DeviceManagement d = deviceManagementService.getOne(new QueryWrapper<DeviceManagement>().lambda().eq(DeviceManagement::getDeviceName, deviceName));
                                    if (ObjectUtil.isNotEmpty(d)) {
                                        cameraManager.setCameraOwnership(d.getId());
                                    } else {
                                        return Result.error("系统中没有第" + i + "条相机对应的设备名称，请检查后再导入！");
                                    }
                                } else {
                                    return Result.error("第" + i + "条数据归属设备为空，请检查！");
                                }
                            }
                            //相机状态
                            XSSFCell cell2 = row.getCell(2);
                            if (cell2 != null) {
                                cell2.setCellType(CellType.STRING);
                                String cameraStatus = cell2.getStringCellValue();
                                if (StringUtils.isNotEmpty(cameraStatus)) {
                                    if (cameraStatus.equals("在线")) {
                                        cameraManager.setCameraStatus("1");
                                    } else {
                                        cameraManager.setCameraStatus("4");
                                    }
                                } else {
                                    return Result.error("第" + i + "条数据相机状态为空，请检查！");
                                }
                            }
                            //归属区域
                            XSSFCell cell3 = row.getCell(3);
                            if (cell3 != null) {
                                cell3.setCellType(CellType.STRING);
                                String location = cell3.getStringCellValue();
                                if (StringUtils.isNotEmpty(location)) {
                                    Integer length = location.length();
                                    if (length > 12) {
                                        return Result.error("第" + i + "条归属区域名称过长，请小于12个汉字！");
                                    }
                                    KyCategory kyCategory = this.kyCategoryService.getOne(new QueryWrapper<KyCategory>().lambda().eq(KyCategory::getName, location));
                                    if (ObjectUtil.isNotEmpty(kyCategory)) {
                                        cameraManager.setLocation(kyCategory.getId());
                                    } else {
                                        return Result.error("系统中没有第" + i + "条相机对应的归属区域信息，请检查后再导入！");
                                    }
                                }
                            }


                            //用户名
                            XSSFCell cell4 = row.getCell(4);
                            if (cell4 != null) {
                                cell4.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell4.getStringCellValue())) {
//								String reg = "^[a-zA-Z][a-zA-Z0-9_]*$";
//								String userName = cell4.getStringCellValue();
//								if(userName.length()<2||userName.length()>8){
//									return Result.error("第" + i + "条数据用户名不符合规则，请检查！需以英文字母开头2-8位，只能包含英文字母、数字、下划线！");
//								}
//								if (!userName.matches(reg)){
//									return Result.error("第" + i + "条数据用户名不符合规则，请检查！需以英文字母开头2-8位，只能包含英文字母、数字、下划线！");
//								}
                                    cameraManager.setUsername(cell4.getStringCellValue());
                                }
                            }

                            //密码
                            XSSFCell cell5 = row.getCell(5);
                            if (cell5 != null) {
                                cell5.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell5.getStringCellValue())) {
//							String reg = "^[a-zA-Z0-9_-]{6,16}$";
//							String password = cell5.getStringCellValue();
//							if (!password.matches(reg)){
//								return Result.error("第" + i + "条数据密码不符合规则，请检查！需要6-16位，只能含有字母，数字，下划线！");
//							}
                                    cameraManager.setPassword(cell5.getStringCellValue());
                                }
                            }
                            //厂商
                            XSSFCell cell6 = row.getCell(6);
                            if (cell6 != null) {
                                cell6.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell6.getStringCellValue())) {
                                    String factory = cell6.getStringCellValue();
                                    List<DictModel> dictModels = this.sysDictService.getDictItems("factory_name");
                                    for (DictModel item : dictModels) {
                                        if (item.getText().equals(factory)) {
                                            cameraManager.setFactory(item.getValue());
                                            break;
                                        }
                                    }
                                    if (StrUtil.isNotBlank(cameraManager.getFactory())) {
                                        CameraFactory cameraFactory = cameraFactoryService.getOne(Wrappers.lambdaQuery(CameraFactory.class)
                                                .eq(CameraFactory::getFactoryName, cameraManager.getFactory())
                                                .ne(CameraFactory::getCameraType, "NVR"), false);
                                        if (ObjectUtil.isNotEmpty(cameraFactory)) {
                                            cameraManager.setCameraType(cameraFactory.getId());
                                        }
                                    }
                                } /*else {
									return Result.error("第" + i + "条数据厂商为空，请检查！");
								}*/

                            }
                            //码流地址
                            XSSFCell cell7 = row.getCell(7);
                            if (cell7 != null) {
                                cell7.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell7.getStringCellValue())) {

                                    cameraManager.setStreamAddress(cell7.getStringCellValue());
                                    cameraManager.setStreamAddressPush(cell7.getStringCellValue());
                                } /*else {
									return Result.error("第" + i + "条数据码流地址为空，请检查！");
								}*/
                            }
                            //相机型号
                            XSSFCell cell8 = row.getCell(8);
                            if (cell8 != null) {
                                cell8.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell8.getStringCellValue())) {
                                    CameraFactory factory = this.cameraFactoryService.getOne(new QueryWrapper<CameraFactory>().lambda().eq(CameraFactory::getCameraType, cell8.getStringCellValue()));
                                    if (ObjectUtil.isNotEmpty(factory)) {
                                        cameraManager.setCameraType(factory.getId());
                                    }
                                } /*else {
									return Result.error("第" + i + "条数据相机型号为空，请检查！");
								}*/
                            }
                            //码流
                            XSSFCell cell9 = row.getCell(9);
                            if (cell9 != null) {
                                cell9.setCellType(CellType.STRING);


                                if (StringUtils.isNotEmpty(cell9.getStringCellValue())) {
                                    String streamType = cell9.getStringCellValue();
                                    List<DictModel> dictModels = this.sysDictService.getDictItems("stream_type");
                                    for (DictModel item : dictModels) {
                                        if (item.getText().equals(streamType)) {

                                            cameraManager.setStreamType(item.getValue());
                                        }
                                    }
                                } /*else {

                                        return Result.error("第" + i + "条数据码流为空，请检查！");
                                    }*/

                            }
                            //码流格式
                            XSSFCell cell10 = row.getCell(10);
                            if (cell10 != null) {
                                cell10.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell10.getStringCellValue())) {
                                    String stream_format = cell10.getStringCellValue();
                                    List<DictModel> dictModels = this.sysDictService.getDictItems("stream_format");
                                    for (DictModel item : dictModels) {
                                        if (item.getText().equals(stream_format)) {
                                            cameraManager.setStreamFormat(item.getValue());
                                        }
                                    }
                                }/* else {
									return Result.error("第" + i + "条数据码流格式为空，请检查！");
								}*/
                            }
                            //通道号
                            XSSFCell cell11 = row.getCell(11);
                            if (cell11 != null) {
                                cell11.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell11.getStringCellValue())) {
                                    String channel = cell11.getStringCellValue();
                                    cameraManager.setChannel((int) Double.parseDouble(channel));
                                } /*else {
									return Result.error("第" + i + "条数据通道号为空，请检查！");
								}*/
                            }


                            //IP地址
                            XSSFCell cell12 = row.getCell(12);
                            if (cell12 != null) {
                                cell12.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell12.getStringCellValue())) {
                                    String reg = "((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))";
                                    String ip = cell12.getStringCellValue();
                                    if (!ip.matches(reg)) {
                                        return Result.error("第" + i + "条数据ip地址不符合规范，请检查！");
                                    }
                                    cameraManager.setIp(cell12.getStringCellValue());
                                } /*else {
									return Result.error("第" + i + "条数据ip地址为空，请检查！");
								}*/
                            }


                            //端口
                            XSSFCell cell13 = row.getCell(13);
                            if (cell13 != null) {
                                cell13.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell13.getStringCellValue())) {
                                    String reg = "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";
                                    String port = cell13.getStringCellValue().replace(".0", "");
                                    if (!port.matches(reg)) {
                                        return Result.error("第" + i + "条数据端口号不符合规范，请检查！");
                                    }
                                    cameraManager.setPort((int) Double.parseDouble(port));
                                } /*else {
									return Result.error("第" + i + "条数据端口为空，请检查！");
								}*/
                            }
                            //相机编码
                            XSSFCell cell14 = row.getCell(14);
                            if (cell14 != null) {
                                cell14.setCellType(CellType.STRING);
                                if (StringUtils.isNotEmpty(cell14.getStringCellValue())) {
                                    cameraManager.setCameraNumber(cell14.getStringCellValue());
                                }
                            }


                            //生成id
                            String uuId = UUID.randomUUID().toString().replace("-", "");
                            cameraManager.setId(uuId);

                            //图片地址
                            cameraManager.setPic("/cameraImg/" + cameraManager.getId() + ".jpg");
                            cameraManager.setIsConfig("1");
                            cameraManager.setStreamTypePush(cameraManager.getStreamType());
                            cameraManager.setStreamFormatPush(cameraManager.getStreamFormat());
                            returnList.add(cameraManager);
                        } else {
                            break;
                        }
                    }
                }
                this.cameraManagerService.saveBatch(returnList);
                Thread.sleep(1000);
                List<DeviceManagement> deviceManagementList = deviceManagementService.list();
                for (DeviceManagement dev : deviceManagementList) {
                    log.info("导入成功，重新生成配置文件");
                    createConfigJson.savaAllConfigByDev(dev.getId());

                }
                cacheCameraList();
                return Result.ok("相机配置导入成功！");

            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Result.error("文件导入失败！");


    }


    @GetMapping(value = "/checkCameraName")
    public Result<Boolean> checkCameraName(CameraManager cameraManager) {
        String cameraName = cameraManager.getCameraName();
        List<CameraManager> list = this.cameraManagerService.list(new QueryWrapper<CameraManager>().lambda().eq(CameraManager::getCameraName, cameraName));
        if (list.size() > 1) {
            return Result.OK(false);
        }
        if (list.size() == 0) {
            return Result.OK(true);
        }
        CameraManager c = list.get(0);
        if (StringUtils.isNotEmpty(cameraManager.getId())) {//编辑校验
            if (cameraManager.getId().equals(c.getId())) {
                return Result.OK(true);
            } else {
                return Result.OK(false);
            }
        }
        return Result.OK(false);
    }

    @GetMapping(value = "/checkCameraIp")
    public Result<Boolean> checkCameraIp(CameraManager cameraManager) {
        String cameraIp = cameraManager.getIp();
        List<CameraManager> list = this.cameraManagerService.list(new QueryWrapper<CameraManager>().lambda().eq(CameraManager::getIp, cameraIp));
        if (list.size() > 1) {
            return Result.OK(false);
        }
        if (list.size() == 0) {
            return Result.OK(true);
        }
        CameraManager c = list.get(0);
        if (StringUtils.isNotEmpty(cameraManager.getId())) {//编辑校验
            if (cameraManager.getId().equals(c.getId())) {
                return Result.OK(true);
            } else {
                return Result.OK(false);
            }
        }
        return Result.OK(false);
    }

    /**
     * 检测相机状态
     *
     * @param cameraManager 相机信息
     * @return Result <?> 返回结果
     */
    @PostMapping(value = "/checkCameraStatus")
    public Result<?> checkCameraStatus(@RequestBody CameraManager cameraManager) {
        if (StrUtil.isBlank(cameraManager.getStreamAddress())) {
            return Result.error("视频流地址不能为空！");
        }
        CameraManager originalCamera = cameraManagerService.getById(cameraManager.getId());
        if (originalCamera.getCameraStatus().equals("1")) return Result.error("相机在线，无需检测！");
        JSONObject cameraStatusJson = CameraStatusCheckerNew.check(cameraManager.getStreamAddress(), cameraManager.getId());
        if (cameraStatusJson == null) return Result.error("相机检测失败！");
        if (cameraStatusJson.getBoolean("success")) {
            cameraManager.setCameraStatus("1");
            cameraManager.setOfflineReason("");
            // 更新数据库相机状态
            cameraManagerService.updateById(cameraManager);
            // 更新缓存相机状态
            CreateConfigJsonService createConfigJsonService = SpringContextUtils.getBean(CreateConfigJsonService.class);
            createConfigJsonService.initCameraConfigCache();
            createConfigJsonService.initAlgConfigCache();
            // 重启ai
            CmdUti.restartAiServer();
            return Result.OK("相机状态检测成功！");
        } else {
            return Result.error(cameraStatusJson.getString("error"));
        }
    }

    /**
     * 重启AI服务
     *
     * @return Result <?> 返回结果
     */
    @GetMapping("/restartAiServer")
    public Result<?> restartAiServer() {
        int i = CmdUti.restartAiServer();
        if (i != 0) {
            return Result.error("AI服务重启失败！");
        }
        return Result.OK("AI服务重启成功！");
    }

    @GetMapping("/faceAlgSwitch")
    public Result<?> faceAlgSwitch(boolean enable) {
        KyCategory kyCategory = kyCategoryService.getOne(Wrappers.lambdaQuery(KyCategory.class)
                .like(KyCategory::getName, "人脸识别"), false);
        if (kyCategory == null) {
            return Result.error("未找到人脸识别算法！");
        }
        String algId = kyCategory.getId();
        List<CameraManager> cameraManagerList = cameraManagerService.list();
        // 获取所有相机ID
        List<String> cameraIds = cameraManagerList.stream().map(CameraManager::getId).toList();
        if (cameraIds.isEmpty()) return Result.error("未找到相机信息！");

        // 获取所有设备ID
        Set<String> deviceIds = cameraManagerList.stream().map(CameraManager::getCameraOwnership).collect(Collectors.toSet());

        if (enable) {
            // 查看当前相机列表中，除人脸识别算法外的所有算法信息
            List<AlgorithmManager> algorithmManagers = algorithmManagerService.list(Wrappers.lambdaQuery(AlgorithmManager.class)
                    .ne(AlgorithmManager::getAlarmType, algId).in(AlgorithmManager::getBelongTo, cameraIds));
            if (ObjectUtil.isEmpty(cameraManagerList)) {
                return Result.error("未找到相机信息！");
            }

            List<String> targetIds = algorithmManagers.stream().map(AlgorithmManager::getId).toList();
            // 如果除人脸识别算法外的所有算法信息为空，则直接返回
            if (ObjectUtil.isEmpty(targetIds)) {
                // 保存开启人脸识别算法状态
                JSONObject data = new JSONObject();
                data.put("enable", true);
                saveSystemParams(data.toJSONString());
                return Result.ok("操作成功");
            }

            // 删除算法
            algorithmManagerService.removeByIds(targetIds);
        } else {
            // 查询出包含人脸识别算法的信息
            List<AlgorithmManager> algorithmManagers = algorithmManagerService.list(Wrappers.lambdaQuery(AlgorithmManager.class)
                    .eq(AlgorithmManager::getAlarmType, algId).in(AlgorithmManager::getBelongTo, cameraIds));

            if (ObjectUtil.isEmpty(algorithmManagers)) {
                // 保存关闭人脸识别算法状态
                JSONObject data = new JSONObject();
                data.put("enable", false);
                saveSystemParams(data.toJSONString());
                return Result.ok("操作成功");
            }

            // 删除算法
            algorithmManagerService.removeByIds(algorithmManagers.stream().map(AlgorithmManager::getId).toList());
        }

        // 重新同步算法配置
        CreateConfigJsonService createConfigJsonService = SpringContextUtils.getBean(CreateConfigJsonService.class);
        for (String deviceId : deviceIds) {
            createConfigJsonService.updateAiServerConfigByDeviceId(deviceId);
        }

        // 重启ai服务
        CmdUti.restartAiServer();

        // 保存开启人脸识别算法状态
        JSONObject data = new JSONObject();
        data.put("enable", enable);
        saveSystemParams(data.toJSONString());
        return Result.ok("操作成功");
    }

    @GetMapping("/getFaceAlgStatus")
    public Result<?> getFaceAlgStatus() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter("EnableFaceAlgorithm");
        if (sysParameter != null) {
            JSONObject data = JSONObject.parseObject(sysParameter.getParamVal());
            return Result.OK(data.getBoolean("enable"));
        }
        return Result.OK(false);
    }

    @GetMapping("/saveFaceGroup")
    public Result<?> saveFaceGroup(@RequestParam String cameraId,
                                   @RequestParam String faceGroupId,
                                   @RequestParam Integer faceCheckThreshold,
                                   @RequestParam Integer faceSimilarityScore) {
        CameraManager cameraInfo = cameraManagerService.getById(cameraId);
        if (cameraInfo == null) {
            return Result.error("未找到相机信息！");
        }
        CameraManager cameraManager = new CameraManager();
        cameraManager.setId(cameraId);
        cameraManager.setFaceGroupId(faceGroupId);
        cameraManager.setFaceCheckThreshold(faceCheckThreshold);
        cameraManager.setFaceSimilarityScore(faceSimilarityScore);
        cameraManagerService.updateById(cameraManager);

        // 准备更新算法配置
        String deviceId = cameraInfo.getCameraOwnership();
        DeviceManagement deviceManagement = deviceManagementService.getById(deviceId);
        createConfigJson.saveAlgConfig(deviceId, deviceManagement.getDeviceIp());
        return Result.OK("操作成功");
    }

    @GetMapping("/getById")
    public Result<?> getById(@RequestParam String id) {
        CameraManager cameraManager = cameraManagerService.getById(id);
        if (cameraManager == null) {
            return Result.error("未找到相机信息！");
        }
        return Result.OK(cameraManager);
    }

    private void saveSystemParams(String value) {
        SysParameter sysParameter = new SysParameter();
        sysParameter.setParamCode("EnableFaceAlgorithm");
        sysParameter.setParamVal(value);
        sysParameter.setParamName("人脸识别算法开关");
        sysParameter.setDescription("人脸识别算法开关");
        sysParameter.setCreateTime(new Date());
        SysParameterUtil.addSystemParams(sysParameter);
    }


    private void afterCameraDeletion(Collection<String> ids) {
        try {
            cameraDeploymentConfigService.removeAssignments(ids);
        } catch (IOException error) {
            log.error("删除相机后清理推理部署映射失败，后端下次启动将重试: {}", ids, error);
        }
        createConfigJsonService.initCameraConfigCache();
        createConfigJsonService.initAlgConfigCache();
        CmdUti.restartAiEngine();
    }

    // 缓存相机列表; mqtt同步到第三方
    private void cacheCameraList() {
        ThreadUtil.execAsync(() -> {
            syncCameraInfo(cacheAllCameraInfo());
        });
    }

    private List<CameraManager> cacheAllCameraInfo() {
        List<CameraManager> cameraManagerList = cameraManagerService.list();
        redisUtil.set(Const.DEVICE_LOCAL_CAMERA_LIST_KEY, cameraManagerList);
        return cameraManagerList;
    }

    // 同步相机到云平台，mqtt协议
    private void syncCameraInfo(List<CameraManager> cameraManagerList) {
        List<CameraMqttInfo> cameraMqttInfoList = new ArrayList<>();
        for (CameraManager cameraManager : cameraManagerList) {
            CameraMqttInfo cameraMqttInfo = new CameraMqttInfo();
            cameraMqttInfo.setId(cameraManager.getId());
            cameraMqttInfo.setLocation(AlarmHandleUtil.getGroupNames(cameraManager.getLocation()));
            cameraMqttInfo.setIpAddress(cameraManager.getIp());
            cameraMqttInfo.setDeviceCode(cameraManager.getCameraNumber());
            cameraMqttInfo.setDeviceName(cameraManager.getCameraName());
            // if (cameraManager.getCameraStatus().equals("1")) {
            //     cameraMqttInfo.setOfflineReason("");
            //     cameraMqttInfo.setStatus(1);
            // } else {
            //     cameraMqttInfo.setStatus(0);
            //     cameraMqttInfo.setOfflineReason(cameraManager.getOfflineReason());
            // }
            cameraMqttInfo.setOfflineReason("");
            cameraMqttInfo.setStatus(1);
            cameraMqttInfo.setCreateBy(cameraManager.getCreateBy());
            cameraMqttInfo.setCreateTime(cameraManager.getCreateTime());
            cameraMqttInfo.setUpdateBy(cameraManager.getCreateBy());
            cameraMqttInfo.setUpdateTime(cameraManager.getCreateTime());
            cameraMqttInfoList.add(cameraMqttInfo);
        }
        IotMsgEntity<List<CameraMqttInfo>> iotMsgEntity = new IotMsgEntity<>();
        iotMsgEntity.setType(DeviceEnum.CAMERA_DEVICE_LIST.getCode());
        iotMsgEntity.setData(cameraMqttInfoList);
        iotMsgEntity.setDeviceSn(SysParameterUtil.getDeviceSn());
        try {
            IotMqttCustomOptions iotMqttCustomOptions = SpringContextUtils.getBean(IotMqttCustomOptions.class);
            MqttClientService.getInstance().getMqttClient().pub(iotMqttCustomOptions.getPubTopic(), JSON.toJSONString(iotMsgEntity));
        } catch (MqttException e) {
            log.error("同步相机列表失败！", e);
        }
    }
}
