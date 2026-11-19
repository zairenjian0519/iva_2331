package org.jeecg.modules.alarmDetail.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.alarmAudit.entity.AlarmAudit;
import org.jeecg.modules.alarmAudit.service.IAlarmAuditService;
import org.jeecg.modules.alarmDetail.entity.*;
import org.jeecg.modules.alarmDetail.service.IAlarmDetailService;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.face.entity.FaceDetailDo;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecg.modules.face.service.FaceDetailService;
import org.jeecg.modules.face.service.FaceGroupService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description: 告警检测表
 * @Author: jeecg-boot
 * @Date: 2022-08-15
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "告警检测表")
@RestController
@RequestMapping("/alarmDetail/alarmDetail")
public class AlarmDetailController extends JeecgController<AlarmDetail, IAlarmDetailService> {
    @Autowired
    private IAlarmDetailService alarmDetailService;
    @Autowired
    private IAlarmAuditService alarmAuditService;
    @Autowired
    private ICameraManagerService cameraManagerService;
    @Autowired
    private IKyCategoryService kyCategoryService;
    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private IDeviceManagementService deviceManagementService;

    /**
     * 分页列表查询
     *
     * @param alarmDetail
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "告警检测表-分页列表查询")
    @ApiOperation(value = "告警检测表-分页列表查询", notes = "告警检测表-分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(AlarmDetail alarmDetail,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<AlarmDetail> queryWrapper = QueryGenerator.initQueryWrapper(alarmDetail, req.getParameterMap());
        queryWrapper.lambda().orderByDesc(AlarmDetail::getAlarmTime);
        Page<AlarmDetail> page = new Page<AlarmDetail>(pageNo, pageSize);
        IPage<AlarmDetail> pageList = alarmDetailService.page(page, queryWrapper);
        for (AlarmDetail detail : pageList.getRecords()) {
            //拼接图片路径
            detail.setPicPath("/alarmImg" + detail.getPicPath());
            detail.setThumbnailPath(detail.getPicPath());
//			KyCategory alarmArea = kyCategoryService.getById(detail.getAlarmArea());
//			if (alarmArea!=null){
//				detail.setAlarmAreaDict(alarmArea.getName());
//			}
            //根据自定义分组id查询所有父级 拼接显示
            String areaString = kyCategoryService.getAreaListByAreaId(detail.getAlarmArea());
            detail.setAlarmAreaDict(areaString);

            KyCategory alarmType = kyCategoryService.getOne(new QueryWrapper<KyCategory>().lambda()
                    .eq(KyCategory::getCode, detail.getAlarmType())
                    .eq(KyCategory::getCategory, "0"));
            if (alarmType != null) {
                detail.setAlarmTypeDict(alarmType.getName());
            }
            AlarmAudit alarmAudit = alarmAuditService.getOne(new QueryWrapper<AlarmAudit>().lambda().eq(AlarmAudit::getAlarmId, detail.getId()));
            if (alarmAudit != null) {
                SysUser sysUser = sysUserService.getById(alarmAudit.getAuditPeople());
                if (sysUser != null) {
                    detail.setAuditPeopleName(sysUser.getRealname());
                }
                detail.setAuditPeople(alarmAudit.getAuditPeople());
                detail.setAuditReason(alarmAudit.getAuditReason());
                detail.setAuditResult(alarmAudit.getAuditResult());
                detail.setAuditStatus(alarmAudit.getAuditStatus());
                detail.setAuditTime(alarmAudit.getAuditTime());
            }
        }
        return Result.OK(pageList);
    }

    /**
     * 按条件查询
     *
     * @param alarmSearchVo
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "告警检测表-按条件查询")
    @ApiOperation(value = "告警检测表-按条件查询", notes = "告警检测表-按条件查询")
    @GetMapping(value = "/queryDetailList")
    public Result<?> queryDetailList(AlarmSearchVo alarmSearchVo,
                                     @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                     HttpServletRequest req) {

        Page<AlarmDetail> page = new Page<>(pageNo, pageSize);

        //避免多次转换字典数据，只查一次放入map，根据需求获取
        List<KyCategory> alarmTypeList = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda().eq(KyCategory::getCategory, "0"));
        Map<String, String> alarmTypeMap = alarmTypeList.stream().collect(Collectors.toMap(KyCategory::getCode, KyCategory::getName));

        // 排除人脸识别算法
        String faceAlgorithmCode = alarmTypeMap.keySet().stream()
                .filter(key -> alarmTypeMap.get(key).contains("人脸识别")).findFirst().orElse("0");
        alarmSearchVo.setAlarmType(faceAlgorithmCode);

        // 查询人脸组信息转为map
        FaceGroupService faceGroupService = SpringContextUtils.getBean(FaceGroupService.class);
        Map<String, FaceGroupDo> faceGroupDoMap = faceGroupService.getList().stream().collect(Collectors.toMap(FaceGroupDo::getId, Function.identity()));

        FaceDetailService faceDetailService = SpringContextUtils.getBean(FaceDetailService.class);

        IPage<AlarmDetail> alarmDetailList = alarmDetailService.selectDetailList(page, alarmSearchVo);

        for (AlarmDetail a : alarmDetailList.getRecords()) {
            //根据自定义分组id查询所有父级 拼接显示
            String areaString = kyCategoryService.getAreaListByAreaId(a.getAlarmArea());
            //拼接图片路径
            a.setPicPath("/alarmImg" + a.getPicPath());
            a.setThumbnailPath(a.getPicPath());
            a.setAlarmTypeDict(alarmTypeMap.get(a.getAlarmType()));
            a.setAlarmAreaDict(areaString);
            // 如果包含人脸信息，需要拼接人脸信息
            if (StrUtil.isNotEmpty(a.getFaceName())) {
                FaceDetailDo faceDetailDo = faceDetailService.getOne(new QueryWrapper<FaceDetailDo>().lambda().eq(FaceDetailDo::getVectorId, a.getFaceName()), false);
                if (faceDetailDo != null) {
                    a.setFaceDetailDo(faceDetailDo);
                    if (faceGroupDoMap.containsKey(faceDetailDo.getFaceGroup())) {
                        a.setFaceGroupDo(faceGroupDoMap.get(faceDetailDo.getFaceGroup()));
                    }
                }
            }
        }

        AlarmDetailQueryVo alarmDetailQueryVo = new AlarmDetailQueryVo();
        alarmDetailQueryVo.setAlarmDetailList(page);
        return Result.OK(alarmDetailQueryVo);
    }

    @AutoLog(value = "告警检测表-告警等级统计数量")
    @ApiOperation(value = "告警检测表-告警等级统计数量", notes = "告警检测表-告警等级统计数量")
    @GetMapping(value = "/queryAlarmCount")
    public Result<?> queryAlarmCount(AlarmSearchVo alarmSearchVo,
                                     @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                     HttpServletRequest req) {
        AlarmDetailQueryVo detailCount = alarmDetailService.getCountFunction(alarmSearchVo);
        AlarmDetailQueryVo alarmDetailQueryVo = new AlarmDetailQueryVo();
        alarmDetailQueryVo.setAllCount(detailCount.getAllCount());
        alarmDetailQueryVo.setNormalCount(detailCount.getNormalCount());
        alarmDetailQueryVo.setRemindCount(detailCount.getRemindCount());
        alarmDetailQueryVo.setCommonCount(detailCount.getCommonCount());
        alarmDetailQueryVo.setWarningCount(detailCount.getWarningCount());
        alarmDetailQueryVo.setSeriousCount(detailCount.getSeriousCount());
        return Result.OK(alarmDetailQueryVo);
    }


    @AutoLog(value = "告警检测表-根据相机名查询告警")
    @ApiOperation(value = "告警检测表-根据相机名查询告警", notes = "告警检测表-根据相机名查询告警")
    @GetMapping(value = "/detailsByCamera")
    public Result<?> detailsByCamera(@RequestParam(name = "cameraId", required = false) String cameraId,
                                     @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        //避免多次转换字典数据，只查一次放入map，根据需求获取
        List<KyCategory> alarmTypeList = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda().eq(KyCategory::getCategory, "0"));
        Map<String, String> alarmTypeMap = new HashMap();
        for (KyCategory kyCategory : alarmTypeList) {
            alarmTypeMap.put(kyCategory.getCode(), kyCategory.getName());
        }
        List<KyCategory> alarmAreaList = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda().eq(KyCategory::getCategory, "1"));
        Map<String, String> alarmAreaMap = new HashMap();
        for (KyCategory kyCategory : alarmAreaList) {
            alarmAreaMap.put(kyCategory.getId(), kyCategory.getName());
        }
        //根据相机名查询告警
        Page<AlarmDetail> page = new Page<>(pageNo, pageSize);
        IPage<AlarmDetail> alarmDetailByCamera = alarmDetailService.getDetailByCamera(page, cameraId);
        for (AlarmDetail alarmDetail : alarmDetailByCamera.getRecords()) {
            DeviceManagement deviceManagement = deviceManagementService.getByCameraId(alarmDetail.getCameraId());
            alarmDetail.setPicPath("/alarmImg" + alarmDetail.getPicPath());
            alarmDetail.setThumbnailPath(alarmDetail.getPicPath());
            SysUser sysUser = sysUserService.getById(alarmDetail.getAuditPeople());
            if (sysUser != null) {
                alarmDetail.setAuditPeopleName(sysUser.getRealname());
            }

            alarmDetail.setAlarmTypeDict(alarmTypeMap.get(alarmDetail.getAlarmType()));
            alarmDetail.setAlarmAreaDict(alarmAreaMap.get(alarmDetail.getAlarmArea()));

        }
        return Result.OK(alarmDetailByCamera);
    }

    /**
     * 告警数据数量统计
     *
     * @param
     * @return
     */
    @AutoLog(value = "告警检测表-数量统计")
    @ApiOperation(value = "告警检测表-数量统计", notes = "告警检测表-数量统计")
    @GetMapping(value = "/count")
    public Result<?> count(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        //避免多次转换字典数据，只查一次放入map，根据需求获取
        List<KyCategory> alarmTypeList = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda().eq(KyCategory::getCategory, "0"));
        Map<String, String> alarmTypeMap = new HashMap();
        for (KyCategory kyCategory : alarmTypeList) {
            alarmTypeMap.put(kyCategory.getCode(), kyCategory.getName());
        }
        List<KyCategory> alarmAreaList = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda().eq(KyCategory::getCategory, "1"));
        Map<String, String> alarmAreaMap = new HashMap();
        for (KyCategory kyCategory : alarmAreaList) {
            alarmAreaMap.put(kyCategory.getId(), kyCategory.getName());
        }

        //最新告警列表
//		 List<AlarmDetail> alarmDetailToday =  alarmDetailService.getTodayList(); //查询的为未审核状态数据 最新数据为当前时间往前24小时内
//		 for (AlarmDetail today:alarmDetailToday){
//				 today.setAlarmTypeDict(alarmTypeMap.get(today.getAlarmType()));
//				 today.setAlarmAreaDict(alarmTypeMap.get(today.getAlarmArea()));
//			 }
        //告警总数量
        int allCount = alarmDetailService.getAlarmCount();
        //最新告警数量
        int todayCount = alarmDetailService.getTodayCount();
        //严重告警的数量 还未规定重要告警标识是几
        List<AlarmDetail> alarmDetailSerious = alarmDetailService.getSeriousList();//查询的为未审核状态数据
        Page<AlarmDetail> page = new Page<AlarmDetail>(pageNo, pageSize);
        int startRow = (pageNo - 1) * pageSize;
        int endRow = pageNo * pageSize;
        if (alarmDetailSerious != null) {
            if (endRow > alarmDetailSerious.size()) {
                endRow = alarmDetailSerious.size();
            }
            page.setRecords(alarmDetailSerious.subList(startRow, endRow));
            page.setTotal(alarmDetailSerious.size());
        } else {
            page.setRecords(alarmDetailSerious);
            page.setTotal(0);
        }
        for (AlarmDetail serious : page.getRecords()) {
            serious.setAlarmTypeDict(alarmTypeMap.get(serious.getAlarmType()));
            serious.setAlarmAreaDict(alarmAreaMap.get(serious.getAlarmArea()));
        }
        int seriousCount = alarmDetailSerious.size();

        AlarmDetailCountVo alarmDetailCountVo = new AlarmDetailCountVo();
        alarmDetailCountVo.setAllCount(allCount);                    //告警总数
        alarmDetailCountVo.setTodayCount(todayCount);                //当日告警数
        alarmDetailCountVo.setSeriousCount(seriousCount);            //严重告警数
//		 alarmDetailCountVo.setTodayList(alarmDetailToday);		//当日告警列表
        alarmDetailCountVo.setSeriousList(page.getRecords());    //严重告警列表
        return Result.OK(alarmDetailCountVo);
    }

    /**
     * 添加
     *
     * @param alarmDetail
     * @return
     */
    @AutoLog(value = "告警检测表-添加")
    @ApiOperation(value = "告警检测表-添加", notes = "告警检测表-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody AlarmDetail alarmDetail) {
        alarmDetailService.save(alarmDetail);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param alarmDetail
     * @return
     */
    @AutoLog(value = "告警检测表-编辑")
    @ApiOperation(value = "告警检测表-编辑", notes = "告警检测表-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody AlarmDetail alarmDetail) {
        alarmDetailService.updateById(alarmDetail);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "告警检测表-通过id删除")
    @ApiOperation(value = "告警检测表-通过id删除", notes = "告警检测表-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        AlarmAudit alarmAudit = alarmAuditService.getOne(new QueryWrapper<AlarmAudit>().lambda().eq(AlarmAudit::getAlarmId, id));
        if (alarmAudit != null) {
            alarmAuditService.removeById(alarmAudit.getId());
        }
        alarmDetailService.removeByIdWithImage(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "告警检测表-批量删除")
    @ApiOperation(value = "告警检测表-批量删除", notes = "告警检测表-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        List<String> list = Arrays.asList(ids.split(","));
        this.alarmDetailService.removeByIdsWithImages(list);
        for (String id : list) {
            AlarmAudit alarmAudit = alarmAuditService.getOne(new QueryWrapper<AlarmAudit>().lambda().eq(AlarmAudit::getAlarmId, id));
            if (alarmAudit != null) {
                alarmAuditService.removeById(alarmAudit.getId());
            }
        }
        return Result.OK("批量删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "告警检测表-通过id查询")
    @ApiOperation(value = "告警检测表-通过id查询", notes = "告警检测表-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        AlarmDetail alarmDetail = alarmDetailService.getById(id);
        return Result.OK(alarmDetail);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param alarmDetail
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, AlarmDetail alarmDetail) {
        return super.exportXls(request, alarmDetail, AlarmDetail.class, "告警检测表");
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
        return super.importExcel(request, response, AlarmDetail.class);
    }

    @GetMapping(value = "/getFaceDetails")
    public Result<?> getFaceDetails(AlarmSearchVo alarmSearchVo,
                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

        Page<AlarmDetail> page = new Page<>(pageNo, pageSize);

        //避免多次转换字典数据，只查一次放入map，根据需求获取
        List<KyCategory> alarmTypeList = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda().eq(KyCategory::getCategory, "0"));
        Map<String, String> alarmTypeMap = alarmTypeList.stream().collect(Collectors.toMap(KyCategory::getCode, KyCategory::getName));

        // 从alarmTypeMap中获取人脸识别算法的code
        String faceAlgorithmCode = alarmTypeMap.keySet().stream().filter(key -> alarmTypeMap.get(key).contains("人脸识别")).findFirst().orElse("0");

        alarmSearchVo.setAlarmType(faceAlgorithmCode);
        IPage<AlarmDetail> alarmDetailList = alarmDetailService.getSelectDetailList(page, alarmSearchVo);

        // 查询人脸组信息转为map
        FaceGroupService faceGroupService = SpringContextUtils.getBean(FaceGroupService.class);
        Map<String, FaceGroupDo> faceGroupDoMap = faceGroupService.getList().stream().collect(Collectors.toMap(FaceGroupDo::getId, Function.identity()));

        // 查询人脸信息转为map
        FaceDetailService faceDetailService = SpringContextUtils.getBean(FaceDetailService.class);
        Map<String, FaceDetailDo> faceDetailDoMap = faceDetailService.list().stream().collect(Collectors.toMap(FaceDetailDo::getVectorId, Function.identity()));

        // 只取人脸算法的数据
        List<AlarmDetail> newList = alarmDetailList.getRecords().stream().filter(a -> a.getAlarmType().equals(faceAlgorithmCode)).toList();


        List<FaceAlarmDetail> faceAlarmDetailList = new ArrayList<>();
        for (AlarmDetail a : newList) {
            // 告警信息
            FaceAlarmDetail faceAlarmDetail = new FaceAlarmDetail();
            faceAlarmDetail.setId(a.getId());
            faceAlarmDetail.setAlarmId(a.getAlarmId());
            faceAlarmDetail.setAlarmTime(a.getAlarmTime());
            faceAlarmDetail.setAlarmType(a.getAlarmType());
            faceAlarmDetail.setAlarmTypeDict(alarmTypeMap.get(a.getAlarmType()));
            faceAlarmDetail.setCameraId(a.getCameraId());
            faceAlarmDetail.setCameraName(a.getCameraName());
            faceAlarmDetail.setCameraNumber(a.getCameraNumber());
            faceAlarmDetail.setPicPath("/alarmImg" + a.getPicPath());
            faceAlarmDetail.setThumbnailPath("/alarmImg" + a.getThumbnailPath());
            faceAlarmDetail.setTargetCount(a.getTargetCount());
            faceAlarmDetail.setAlgorithmRemark(a.getRemarks());
            faceAlarmDetail.setIsRotate(a.getIsRotate());

            // 人脸信息
            FaceDetailDo faceDetailDo = faceDetailDoMap.get(a.getFaceName());
            if (ObjectUtil.isNotEmpty(faceDetailDo)) {
                faceAlarmDetail.setVectorId(faceDetailDo.getVectorId());
                faceAlarmDetail.setFaceName(faceDetailDo.getFaceName());
                faceAlarmDetail.setFacePhone(faceDetailDo.getFacePhone());
                faceAlarmDetail.setFaceIdCard(faceDetailDo.getFaceIdCard());
                faceAlarmDetail.setFaceGroup(faceDetailDo.getFaceGroup());
                faceAlarmDetail.setRemark(faceDetailDo.getRemark());
                faceAlarmDetail.setImageNewName(faceDetailDo.getImageNewName());
                faceAlarmDetail.setImagePath(faceDetailDo.getImagePath());
                // faceAlarmDetail.setImageUrl("/face/" + faceDetailDo.getFaceGroup() + "/" + faceDetailDo.getImageNewName());

                // group信息
                String faceGroup = faceDetailDo.getFaceGroup();
                faceAlarmDetail.setFaceGroup(faceGroup);
                faceAlarmDetail.setGroupPath(faceGroupDoMap.get(faceGroup).getGroupPath());
                faceAlarmDetail.setGroupName(faceGroupDoMap.get(faceGroup).getGroupName());
            }

            faceAlarmDetailList.add(faceAlarmDetail);
        }

        Page<FaceAlarmDetail> faceAlarmDetailPage = new Page<>();
        faceAlarmDetailPage.setRecords(faceAlarmDetailList);
        faceAlarmDetailPage.setTotal(alarmDetailList.getTotal());
        faceAlarmDetailPage.setSize(alarmDetailList.getSize());
        faceAlarmDetailPage.setCurrent(alarmDetailList.getCurrent());
        faceAlarmDetailPage.setPages(alarmDetailList.getPages());
        return Result.OK(faceAlarmDetailPage);
    }

    @GetMapping(value = "/clearAlarmPlan")
    public Result<?> clearAlarmPlan() {
        alarmDetailService.clearAlarm();
        return Result.OK("清除成功！");
    }

    /**
     * 清除指定天数前的数据
     * 暂时没用到
     */
    @GetMapping(value = "/clearAlarmDaysAgo")
    public Result<?> clearAlarmDaysAgo(@RequestParam Integer days) {
        alarmDetailService.clearAlarmDaysAgo(days);
        return Result.OK("清除成功！");
    }

    /**
     * 手动清除数据, 一键清楚
     */
    @GetMapping(value = "/clearAlarmByManual")
    public Result<?> clearAlarmByManual() {
        return alarmDetailService.clearAlarmByManual();
    }
}
