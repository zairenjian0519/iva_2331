package org.jeecg.modules.cameraGroupCamera.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.cameraGroupCamera.entity.CameraGroupCamera;
import org.jeecg.modules.cameraGroupCamera.service.ICameraGroupCameraService;
import java.util.Date;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

 /**
 * @Description: 轮询组相机表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
@Slf4j
@Api(tags="轮询组相机表")
@RestController
@RequestMapping("/cameraGroupCamera/cameraGroupCamera")
public class CameraGroupCameraController extends JeecgController<CameraGroupCamera, ICameraGroupCameraService> {
	@Autowired
	private ICameraGroupCameraService cameraGroupCameraService;
	
	/**
	 * 分页列表查询
	 *
	 * @param cameraGroupCamera
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@AutoLog(value = "轮询组相机表-分页列表查询")
	@ApiOperation(value="轮询组相机表-分页列表查询", notes="轮询组相机表-分页列表查询")
	@GetMapping(value = "/list")
	public Result<?> queryPageList(CameraGroupCamera cameraGroupCamera,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<CameraGroupCamera> queryWrapper = QueryGenerator.initQueryWrapper(cameraGroupCamera, req.getParameterMap());
		Page<CameraGroupCamera> page = new Page<CameraGroupCamera>(pageNo, pageSize);
		IPage<CameraGroupCamera> pageList = cameraGroupCameraService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 * 添加
	 *
	 * @param cameraGroupCamera
	 * @return
	 */
	@AutoLog(value = "轮询组相机表-添加")
	@ApiOperation(value="轮询组相机表-添加", notes="轮询组相机表-添加")
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody CameraGroupCamera cameraGroupCamera) {
		cameraGroupCameraService.save(cameraGroupCamera);
		return Result.OK("添加成功！");
	}
	
	/**
	 * 编辑
	 *
	 * @param cameraGroupCamera
	 * @return
	 */
	@AutoLog(value = "轮询组相机表-编辑")
	@ApiOperation(value="轮询组相机表-编辑", notes="轮询组相机表-编辑")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<?> edit(@RequestBody CameraGroupCamera cameraGroupCamera) {
		cameraGroupCameraService.updateById(cameraGroupCamera);
		return Result.OK("编辑成功!");
	}
	
	/**
	 * 通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "轮询组相机表-通过id删除")
	@ApiOperation(value="轮询组相机表-通过id删除", notes="轮询组相机表-通过id删除")
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		cameraGroupCameraService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 * 批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "轮询组相机表-批量删除")
	@ApiOperation(value="轮询组相机表-批量删除", notes="轮询组相机表-批量删除")
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.cameraGroupCameraService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "轮询组相机表-通过id查询")
	@ApiOperation(value="轮询组相机表-通过id查询", notes="轮询组相机表-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		CameraGroupCamera cameraGroupCamera = cameraGroupCameraService.getById(id);
		return Result.OK(cameraGroupCamera);
	}

  /**
   * 导出excel
   *
   * @param request
   * @param cameraGroupCamera
   */
  @RequestMapping(value = "/exportXls")
  public ModelAndView exportXls(HttpServletRequest request, CameraGroupCamera cameraGroupCamera) {
      return super.exportXls(request, cameraGroupCamera, CameraGroupCamera.class, "轮询组相机表");
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
      return super.importExcel(request, response, CameraGroupCamera.class);
  }

}
