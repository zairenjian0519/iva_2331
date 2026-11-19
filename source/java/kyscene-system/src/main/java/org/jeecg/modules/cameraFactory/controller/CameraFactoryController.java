package org.jeecg.modules.cameraFactory.controller;

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
import org.jeecg.modules.cameraFactory.entity.CameraFactory;
import org.jeecg.modules.cameraFactory.service.ICameraFactoryService;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @Description: 相机基础信息
 * @Author: jeecg-boot
 * @Date: 2022-08-29
 * @Version: V1.0
 */
@Api(tags = "相机基础信息")
@RestController
@RequestMapping("/cameraFactory/cameraFactory")
@Slf4j
public class CameraFactoryController extends JeecgController<CameraFactory, ICameraFactoryService> {
    @Autowired
    private ICameraFactoryService cameraFactoryService;

    /**
     * 分页列表查询
     *
     * @param cameraFactory
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    //@AutoLog(value = "相机基础信息-分页列表查询")
    @ApiOperation(value = "相机基础信息-分页列表查询", notes = "相机基础信息-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<CameraFactory>> queryPageList(CameraFactory cameraFactory,
                                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                      HttpServletRequest req) {
        QueryWrapper<CameraFactory> queryWrapper = QueryGenerator.initQueryWrapper(cameraFactory, req.getParameterMap());
        Page<CameraFactory> page = new Page<CameraFactory>(pageNo, pageSize);
        IPage<CameraFactory> pageList = cameraFactoryService.page(page, queryWrapper);
        return Result.OK(pageList);
    }


    //@AutoLog(value = "相机基础信息-厂商列表")
    @ApiOperation(value = "相机基础信息-厂商列表", notes = "相机基础信息-厂商列表")
    @GetMapping(value = "/factoryList")
    public Result<?> factoryList() {
        List<CameraFactory> cameraFactories = cameraFactoryService.list();
        List list = new ArrayList();
        for (CameraFactory c : cameraFactories) {
            list.add(c.getFactoryName());
        }
        Map<String, Object> map = new HashMap();
        map.put("factoryList", list);
        return Result.OK(map);
    }

    /**
     * 添加
     *
     * @param cameraFactory
     * @return
     */
    @AutoLog(value = "相机基础信息-添加")
    @ApiOperation(value = "相机基础信息-添加", notes = "相机基础信息-添加")
//	@RequiresPermissions("org.jeecg.modules.demo:camera_factory:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody CameraFactory cameraFactory) {
        CameraFactory camera = cameraFactoryService.getOne(new QueryWrapper<CameraFactory>()
                .lambda()
                .eq(CameraFactory::getFactoryName, cameraFactory.getFactoryName())
                .eq(CameraFactory::getCameraType, cameraFactory.getCameraType()));
        if (camera != null) {
            return Result.error("该厂家下已存在相同相机型号，请重新输入！");
        }
        cameraFactoryService.save(cameraFactory);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param cameraFactory
     * @return
     */
    @AutoLog(value = "相机基础信息-编辑")
    @ApiOperation(value = "相机基础信息-编辑", notes = "相机基础信息-编辑")
//	@RequiresPermissions("org.jeecg.modules.demo:camera_factory:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody CameraFactory cameraFactory) {
        CameraFactory camera = cameraFactoryService.getOne(new QueryWrapper<CameraFactory>()
                .lambda()
                .eq(CameraFactory::getFactoryName, cameraFactory.getFactoryName())
                .eq(CameraFactory::getCameraType, cameraFactory.getCameraType()));
        if (camera != null && !cameraFactory.getId().equals(camera.getId())) {
            return Result.error("该厂家下已存在相同相机型号，请重新输入！");
        }
        cameraFactoryService.updateById(cameraFactory);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "相机基础信息-通过id删除")
    @ApiOperation(value = "相机基础信息-通过id删除", notes = "相机基础信息-通过id删除")
//	@RequiresPermissions("org.jeecg.modules.demo:camera_factory:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        cameraFactoryService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "相机基础信息-批量删除")
    @ApiOperation(value = "相机基础信息-批量删除", notes = "相机基础信息-批量删除")
//	@RequiresPermissions("org.jeecg.modules.demo:camera_factory:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.cameraFactoryService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "相机基础信息-通过id查询")
    @ApiOperation(value = "相机基础信息-通过id查询", notes = "相机基础信息-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<CameraFactory> queryById(@RequestParam(name = "id", required = true) String id) {
        CameraFactory cameraFactory = cameraFactoryService.getById(id);
        if (cameraFactory == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(cameraFactory);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param cameraFactory
     */
//    @RequiresPermissions("org.jeecg.modules.demo:camera_factory:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, CameraFactory cameraFactory) {
        return super.exportXls(request, cameraFactory, CameraFactory.class, "相机基础信息");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
//    @RequiresPermissions("camera_factory:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            // 获取上传文件对象
            MultipartFile file = entity.getValue();
            ImportParams params = new ImportParams();
            params.setTitleRows(2);
            params.setHeadRows(1);
            params.setNeedSave(true);
            try {
                List<CameraFactory> list = ExcelImportUtil.importExcel(file.getInputStream(), CameraFactory.class, params);
                long start = System.currentTimeMillis();
                List<CameraFactory> cameraList = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getCameraType() != null) {
                        CameraFactory camera = cameraFactoryService.getOne(new QueryWrapper<CameraFactory>()
                                .lambda()
                                .eq(CameraFactory::getFactoryName, list.get(i).getFactoryName())
                                .eq(CameraFactory::getCameraType, list.get(i).getCameraType()));
                        if (camera != null) {
                            continue;
                        } else {
                            cameraList.add(list.get(i));
                        }
                    } else {
                        break;
                    }
                }
                service.saveBatch(cameraList);
                log.info("消耗时间" + (System.currentTimeMillis() - start) + "毫秒");
                return Result.ok("文件导入成功！已过滤掉厂商下相同型号相机数据");
            } catch (Exception e) {
                String msg = e.getMessage();
                log.error(msg, e);
                if (msg != null && msg.indexOf("Duplicate entry") >= 0) {
                    return Result.error("文件导入失败:有重复数据！");
                } else {
                    return Result.error("文件导入失败:" + e.getMessage());
                }
                //update-end-author:taoyan date:20211124 for: 导入数据重复增加提示
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
}
