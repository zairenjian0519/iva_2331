package org.jeecg.modules.viewPreview.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.alarmDetail.entity.AlarmDetail;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.viewPreview.entity.*;
import org.jeecg.modules.viewPreview.service.IViewPreviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @Description: 实时预览
 */
@Slf4j
@Api(tags = "实时预览")
@RestController
@RequestMapping("/viewPreview/viewPreview")
public class ViewPreviewController extends JeecgController<ViewPreview, IViewPreviewService> {
    @Autowired
    private IViewPreviewService viewPreviewService;
    @Autowired
    private IKyCategoryService kyCategoryService;
    @Autowired
    private IAlarmDetailService alarmDetailService;
    @Autowired
    private IDeviceManagementService deviceManagementService;

    @AutoLog(value = "实时预览-分页列表查询")
    @ApiOperation(value = "实时预览-分页列表查询", notes = "实时预览-分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(ViewPreview viewPreview,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) throws IOException {
        QueryWrapper<ViewPreview> queryWrapper = QueryGenerator.initQueryWrapper(viewPreview, req.getParameterMap());
        Page<ViewPreview> page = new Page<>(pageNo, pageSize);
        IPage<ViewPreview> pageList = viewPreviewService.page(page, queryWrapper);
        for (ViewPreview v : pageList.getRecords()) {
            List<AlarmDetail> seriousList = alarmDetailService.list(new QueryWrapper<AlarmDetail>().lambda()
                    .eq(AlarmDetail::getCameraId, v.getCameraName())
                    .eq(AlarmDetail::getAlarmLevel, 1));
            int seriousByCamera = seriousList.size();
            List<AlarmDetail> allList = alarmDetailService.list(new QueryWrapper<AlarmDetail>().lambda()
                    .eq(AlarmDetail::getCameraId, v.getCameraName()));
            int allByCamera = allList.size();
            v.setSeriousCameraCount(seriousByCamera);
            v.setAllCameraCount(allByCamera);
        }
        return Result.OK(pageList);
    }

    @AutoLog(value = "实时预览-相机区域/设备分页列表")
    @ApiOperation(value = "实时预览-相机区域/设备分页列表", notes = "实时预览-相机区域/设备分页列表")
    @GetMapping(value = "/pageListByCamera")
    public Result<?> pageListByCamera(@RequestParam(name = "ids", required = false) List<String> ids,
                                      @RequestParam(name = "categorys", required = false) List<String> categorys,
                                      @RequestParam(name = "onlyAlarm", required = false, defaultValue = "false") Boolean onlyAlarm,
                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                      HttpServletRequest req) throws IOException {
        List<ViewPreview> cameraByLocationList = viewPreviewService.getSelectCameraList(ids, categorys); //实时预览，根据相机id和告警类型名称查询数据
        //查询设备列表

        List<DeviceManagement> deviceManagementList = deviceManagementService.list();
        //刷新预览图方法不走mqtt 走算法新的restful接口
//		 new Thread(() -> {
//			 for (DeviceManagement deviceManagement : deviceManagementList) {
//				 try {
//					 MqttClientConnect client = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
//					 if (null != client) {
//						 JSONObject info = new JSONObject();
//						 info.put("refresh", 1);
////					 System.out.println("在这里：" + info);
//						 client.pub("web_picrefresh", info.toJSONString());
//					 }
//				 } catch (MqttException e) {
//					 e.printStackTrace();
//
//				 }
//			 }
//		 }).start();

        //分页相机列表
        pageSize = 1000; //目前相当于不分页展示，按全量返回，后期根据需求修改。
        Page<ViewPreview> page = new Page<>(pageNo, pageSize);
        int startRow = (pageNo - 1) * pageSize;//起始页码=（当前页码-1）*每页展示条数
        int endRow = pageNo * pageSize;//截至页码=当前页码*每页展示条数
        if (cameraByLocationList != null) {
            if (endRow > cameraByLocationList.size()) {
                endRow = cameraByLocationList.size();
            }
            page.setRecords(cameraByLocationList.subList(startRow, endRow));
            page.setTotal(cameraByLocationList.size());
        } else {
            page.setRecords(cameraByLocationList);
            page.setTotal(0);
        }
        List<AlarmCountVo> alarmCountVoList = viewPreviewService.getAlarmCountByCamera();
        //根据相机名获取对应的告警总数和严重告警数

        for (ViewPreview v : page.getRecords()) {
            if (v != null) {
                for (AlarmCountVo a : alarmCountVoList) {
                    if (a.getCameraId().equals(v.getId())) {
                        v.setSeriousCameraCount(a.getSeriousCount());
                        v.setAllCameraCount(a.getAllCount());
                    }
                }
                String algorithm = viewPreviewService.getAllAlgorithmById(v.getId()); //根据相机id查对应的多个算法 拼起来返给前端
                v.setAlarmTypeDict(algorithm);
//				 Integer seriousCount = alarmDetailService.getSeriousList(v.getId()); //所有查询默认都为未审核;
//				 Integer allCount = alarmDetailService.getAllList(v.getId());
//				 v.setSeriousCameraCount(seriousCount);
//				 v.setAllCameraCount(allCount);

            }
        }

        //存放返回的数据（按区域分组）
        List byLocationList = new ArrayList();
        Map<String, List<ViewPreview>> locationMap = new LinkedHashMap<>(); //需要按id顺序排列，用linkedHash
        //查询区域列表（category=1，pid!=0）
        List<KyCategory> kyCategoryList = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda()
//				 .ne(KyCategory::getPid,0)  9.21日修改任何级别都可以下挂相机，所以不需要过滤pid
                .eq(KyCategory::getCategory, 1)
                .orderByAsc(KyCategory::getId));
        //遍历区域
        for (KyCategory k : kyCategoryList) {
            List<ViewPreview> mapList = new ArrayList();
            if (k != null) {
                locationMap.put(k.getId(), mapList);
            }
        }
        //遍历分页取得的相机表
        for (int i = 0; i < page.getRecords().size(); i++) {
            //根据key：位置信息 存入map的value：mapList
            if (page.getRecords().get(i).getLocation() != null) {
                KyCategory kyCategory = kyCategoryService.getById(page.getRecords().get(i).getLocation());
//				 if (kyCategory!=null) {
//					 page.getRecords().get(i).setLocationDict(kyCategory.getName());
//				 }

                String areaString = kyCategoryService.getAreaListByAreaId(page.getRecords().get(i).getLocation());
                page.getRecords().get(i).setLocationDict(areaString);

                DeviceManagement deviceManagement = deviceManagementService.getById(page.getRecords().get(i).getCameraOwnership());
                if (deviceManagement != null) {
                    page.getRecords().get(i).setDeviceDict(deviceManagement.getDeviceName());
                }
                List<ViewPreview> cameraList = locationMap.get(page.getRecords().get(i).getLocation());
                //仅告警按钮，为true显示有未审核告警的相机
                String cameraId = page.getRecords().get(i).getId();
                String deviceIp = deviceManagementService.getByCameraId(cameraId).getDeviceIp();
                page.getRecords().get(i).setDeviceIp(deviceIp);
                if (onlyAlarm == true) {
                    if (page.getRecords().get(i).getAllCameraCount() != null) {
                        if (!page.getRecords().get(i).getAllCameraCount().equals(0)) {
                            String picPath = page.getRecords().get(i).getPic();
                            page.getRecords().get(i).setPic(picPath);
                            cameraList.add(page.getRecords().get(i));
                        }
                    }
                } else {
                    if (kyCategory != null && deviceManagement != null) {
                        String picPath = page.getRecords().get(i).getPic();
                        page.getRecords().get(i).setPic(picPath);
                        cameraList.add(page.getRecords().get(i));
                    }
                }
            }

        }
        for (Map.Entry<String, List<ViewPreview>> entry : locationMap.entrySet()) {
            LocationVo locationVo = new LocationVo();

            for (KyCategory kyCategory : kyCategoryList) {
                if (kyCategory != null) {
                    if (kyCategory.getId().equals(entry.getKey())) {
                        locationVo.setId(kyCategory.getId());
                        locationVo.setCameraLocation(entry.getKey());
//						 locationVo.setCameraLocationDict(kyCategory.getName());
                        //修改实时预览展示位置时 显示父级+子级信息
                        String areaString = kyCategoryService.getAreaListByAreaId(kyCategory.getId());
                        locationVo.setCameraLocationDict(areaString);
                        locationVo.setCameraLocationList(entry.getValue());
                    }
                }
            }
            byLocationList.add(locationVo);
        }

        //存放返回的数据（按设备分组）
        List byOwnershipList = new ArrayList();
        Map<String, List<ViewPreview>> ownershipMap = new LinkedHashMap<>();
        //查询设备列表 放到最上面
        //遍历设备
        for (DeviceManagement d : deviceManagementList) {
            List<ViewPreview> mapList = new ArrayList();
            if (d != null) {
                ownershipMap.put(d.getId(), mapList);
            }
        }
        //遍历分页取得的相机表
        for (int i = 0; i < page.getRecords().size(); i++) {
            //根据key：设备名 存入map的value：mapList
            if (page.getRecords().get(i).getLocation() != null) {
                KyCategory kyCategory = kyCategoryService.getById(page.getRecords().get(i).getLocation());
//				 if (kyCategory!=null) {
//					 page.getRecords().get(i).setLocationDict(kyCategory.getName());
//				 }
                String areaString = kyCategoryService.getAreaListByAreaId(page.getRecords().get(i).getLocation());
                page.getRecords().get(i).setLocationDict(areaString);

                DeviceManagement deviceManagement = deviceManagementService.getById(page.getRecords().get(i).getCameraOwnership());
                if (deviceManagement != null) {
                    page.getRecords().get(i).setDeviceDict(deviceManagement.getDeviceName());
                }
                String cameraId = page.getRecords().get(i).getId();
                String deviceIp = deviceManagementService.getByCameraId(cameraId).getDeviceIp();
                page.getRecords().get(i).setDeviceIp(deviceIp);
                List<ViewPreview> cameraList = ownershipMap.get(page.getRecords().get(i).getCameraOwnership());
                //仅告警按钮，为true显示有未审核告警的相机
                if (onlyAlarm == true) {
                    if (page.getRecords().get(i).getAllCameraCount() != null) {
                        if (!page.getRecords().get(i).getAllCameraCount().equals(0)) {
                            cameraList.add(page.getRecords().get(i));
                        }
                    }
                } else {
                    if (kyCategory != null && deviceManagement != null) {
                        cameraList.add(page.getRecords().get(i));
                    }
                }
            }
        }
        for (Map.Entry<String, List<ViewPreview>> entry : ownershipMap.entrySet()) {
            OwnershipVo ownershipVo = new OwnershipVo();
            for (DeviceManagement deviceManagement : deviceManagementList) {
                if (deviceManagement != null) {
                    if (deviceManagement.getId().equals(entry.getKey())) {
                        ownershipVo.setId(deviceManagement.getId());
                        ownershipVo.setCameraOwnership(entry.getKey());
                        ownershipVo.setCameraOwnershipDict(deviceManagement.getDeviceName());
                        ownershipVo.setCameraOwnershipList(entry.getValue());
                    }
                }
            }

            byOwnershipList.add(ownershipVo);
        }
        CameraGroupVo cameraGroupVo = new CameraGroupVo();
        cameraGroupVo.setByLocationList(byLocationList);
        cameraGroupVo.setByOwnershipList(byOwnershipList);
        cameraGroupVo.setTotal(cameraByLocationList.size());
        return Result.OK(cameraGroupVo);
    }

    @AutoLog(value = "实时预览-相机对应的告警数量")
    @ApiOperation(value = "实时预览-相机对应的告警数量", notes = "实时预览-相机对应的告警数量")
    @GetMapping(value = "/getAlarmCountByCamera")
    public Result<?> getAlarmCountByCamera() {
        List<AlarmCountVo> alarmCountVoList = viewPreviewService.getAlarmCountByCamera();
        return Result.OK(alarmCountVoList);
    }


    /**
     * 通过id获取设备详细信息
     *
     * @param cameraId 相机id
     * @return R
     */
    @AutoLog(value = "实时预览-获取流地址")
    @ApiOperation(value = "实时预览-获取流地址", notes = "实时预览-获取流地址")
    @GetMapping(value = "/getDeviceInfo")
    public Result<?> getDeviceInfo(@RequestParam(name = "cameraId") String cameraId) {
        DeviceManagement deviceManagement = deviceManagementService.getByCameraId(cameraId);
        deviceManagement.setIsTypeChoose(true);
        return Result.OK(deviceManagement);
    }

    /**
     * 通过id关闭视频流 3。7日此接口弃用，改为算法通过http监听心跳控制关闭
     *
     * @param cameraId
     * @return
     */
    @AutoLog(value = "实时预览-关闭视频流")
    @ApiOperation(value = "实时预览-关闭视频流", notes = "实时预览-关闭视频流")
    @GetMapping(value = "/closeVideoStream")
    public Result<?> closeVideoStream(@RequestParam(name = "cameraId", required = false) String cameraId) {
        if (cameraId != null && !cameraId.equals("")) {
            DeviceManagement deviceManagement = deviceManagementService.getByCameraId(cameraId);
            try {
                MqttClientConnect client = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
                if (null != client) {
                    JSONObject info = new JSONObject();
                    info.put("cameraId", cameraId);
                    info.put("type", "2");
                    client.pub("web_videoconf", info.toJSONString());
                    System.out.println("关闭信息：" + info);
                    log.info("关闭设备" + deviceManagement.getDeviceIp() + "的视频流");
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        return Result.OK();

    }

    /**
     * 添加
     *
     * @param viewPreview
     * @return
     */
    @AutoLog(value = "实时预览-添加")
    @ApiOperation(value = "实时预览-添加", notes = "实时预览-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody ViewPreview viewPreview) {
        viewPreviewService.save(viewPreview);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param viewPreview
     * @return
     */
    @AutoLog(value = "实时预览-编辑")
    @ApiOperation(value = "实时预览-编辑", notes = "实时预览-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody ViewPreview viewPreview) {
        viewPreviewService.updateById(viewPreview);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "实时预览-通过id删除")
    @ApiOperation(value = "实时预览-通过id删除", notes = "实时预览-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        viewPreviewService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "实时预览-批量删除")
    @ApiOperation(value = "实时预览-批量删除", notes = "实时预览-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.viewPreviewService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "实时预览-通过id查询")
    @ApiOperation(value = "实时预览-通过id查询", notes = "实时预览-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        ViewPreview viewPreview = viewPreviewService.getById(id);
        return Result.OK(viewPreview);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param viewPreview
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, ViewPreview viewPreview) {
        return super.exportXls(request, viewPreview, ViewPreview.class, "实时预览");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, ViewPreview.class);
    }

    /**
     * 是否开启算法视频流
     * 用于界面是否展示这个功能
     */
    @GetMapping("/enableAlgStream")
    public Result<?> enableAlgStream(){
        boolean enable = viewPreviewService.enableAlgStream();
        return Result.ok(enable);
    }
}
