package org.jeecg.modules.cameraGroupAlgorithm.controller;

import java.util.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.algorithmmanager.service.IAlgorithmManagerService;
import org.jeecg.modules.cameraGroup.controller.CameraGroupController;
import org.jeecg.modules.cameraGroup.entity.CameraGroup;
import org.jeecg.modules.cameraGroup.service.ICameraGroupService;
import org.jeecg.modules.cameraGroupAlgorithm.entity.CameraGroupAlgorithm;
import org.jeecg.modules.cameraGroupAlgorithm.service.ICameraGroupAlgorithmService;
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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

 /**
 * @Description: 轮询组算法表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
@Slf4j
@Api(tags="轮询组算法表")
@RestController
@RequestMapping("/cameraGroupAlgorithm/cameraGroupAlgorithm")
public class CameraGroupAlgorithmController extends JeecgController<CameraGroupAlgorithm, ICameraGroupAlgorithmService> {
	@Autowired
	private ICameraGroupAlgorithmService cameraGroupAlgorithmService;
	@Autowired
	private CameraGroupController cameraGroupController;
	@Autowired
	private IAlgorithmManagerService algorithmManagerService;
	 @Autowired
	 private ICameraGroupService cameraGroupService;
	/**
	 * 分页列表查询
	 *
	 * @param cameraGroupAlgorithm
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@AutoLog(value = "轮询组算法表-分页列表查询")
	@ApiOperation(value="轮询组算法表-分页列表查询", notes="轮询组算法表-分页列表查询")
	@GetMapping(value = "/list")
	public Result<?> queryPageList(CameraGroupAlgorithm cameraGroupAlgorithm,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<CameraGroupAlgorithm> queryWrapper = QueryGenerator.initQueryWrapper(cameraGroupAlgorithm, req.getParameterMap());
		Page<CameraGroupAlgorithm> page = new Page<CameraGroupAlgorithm>(pageNo, pageSize);
		IPage<CameraGroupAlgorithm> pageList = cameraGroupAlgorithmService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	/**
	 * 添加
	 *
	 * @param cameraGroupAlgorithm
	 * @return
	 */
	@AutoLog(value = "轮询组算法表-添加")
	@ApiOperation(value="轮询组算法表-添加", notes="轮询组算法表-添加")
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody CameraGroupAlgorithm cameraGroupAlgorithm) {
		if (cameraGroupAlgorithm.getAlgorithmId().equals("")){
			return Result.error("请选择一个算法");
		}
		String groupId = cameraGroupAlgorithm.getGroupId();
		cameraGroupAlgorithm.setModelName(cameraGroupAlgorithm.getAlgorithmId());
		cameraGroupAlgorithm.setAlarmType(cameraGroupAlgorithm.getAlgorithmId());
		CameraGroup cameraGroup = cameraGroupService.getById(groupId);
		if (cameraGroup.getAlgorithmIds()==null||cameraGroup.getAlgorithmIds().equals("")){
			cameraGroup.setAlgorithmIds(cameraGroupAlgorithm.getAlgorithmId());
			cameraGroupService.updateById(cameraGroup);
		}else{
			String ids = cameraGroup.getAlgorithmIds()+","+cameraGroupAlgorithm.getAlgorithmId();
			cameraGroup.setAlgorithmIds(ids);
			cameraGroupService.updateById(cameraGroup);
		}
		cameraGroupAlgorithmService.save(cameraGroupAlgorithm);
		/* 生成算法到算法总表 */
		cameraGroupController.saveAlgorithmConfig(groupId);
		return Result.OK("添加成功！");
	}
	
	/**
	 * 编辑
	 *
	 * @param cameraGroupAlgorithm
	 * @return
	 */
	@AutoLog(value = "轮询组算法表-编辑")
	@ApiOperation(value="轮询组算法表-编辑", notes="轮询组算法表-编辑")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<?> edit(@RequestBody CameraGroupAlgorithm cameraGroupAlgorithm) {
		String groupId = cameraGroupAlgorithm.getGroupId();
		String algorithmId = cameraGroupAlgorithm.getAlgorithmId();
		List<AlgorithmManager> algorithmManagerList = algorithmManagerService.getByAlgGroupId(groupId,algorithmId);
		CameraGroupAlgorithm algorithm = cameraGroupAlgorithmService.getAlgByGroupIdAndAlgId(groupId,algorithmId);
		algorithm.setCheckTime(cameraGroupAlgorithm.getCheckTime());
		algorithm.setThreshold(cameraGroupAlgorithm.getThreshold());
		algorithm.setAlarmLevel(cameraGroupAlgorithm.getAlarmLevel());
		algorithm.setTaskRemark(cameraGroupAlgorithm.getTaskRemark());
		algorithm.setTargetThreshold(cameraGroupAlgorithm.getTargetThreshold());
		cameraGroupAlgorithmService.updateById(algorithm);
		/* 编辑单个算法时，需要更新算法总表同组算法 */
		for (AlgorithmManager alg:algorithmManagerList){
			BeanUtils.copyProperties(algorithm,alg,"id","belongTo");
			alg.setIsRotate(1);
			algorithmManagerService.updateById(alg);
		}
		return Result.OK("编辑成功!");
	}
	
	/**
	 * 通过id删除
	 *
	 * @return
	 */
	@AutoLog(value = "轮询组算法表-通过id删除")
	@ApiOperation(value="轮询组算法表-通过id删除", notes="轮询组算法表-通过id删除")
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="groupId",required = true) String groupId,@RequestParam(name="algorithmId",required=true) String algorithmId) {
		/* 获取所选组 */
		CameraGroup cameraGroup = cameraGroupService.getById(groupId);
		/* 将所选组内的算法根据id删除，取传来的id和原有的算法ids的差集，修改轮询组表的算法ids字符串 */
		List<String> algorithmIdsList = Arrays.asList(cameraGroup.getAlgorithmIds().split(","));
		CameraGroupAlgorithm cameraGroupAlgorithm = cameraGroupAlgorithmService.getAlgByGroupIdAndAlgId(groupId,algorithmId);
		List<String> list = new ArrayList();
		list.add(cameraGroupAlgorithm.getAlgorithmId());
		List<String> reduceList = algorithmIdsList.stream().filter(item -> !list.contains(item)).collect(Collectors.toList());//取差集
		String ids = StringUtils.strip(reduceList.toString(),"[]").replace(" ",""); //保存的ids需要处理去除[]和自带的空格
		cameraGroup.setAlgorithmIds(ids);
		cameraGroupService.updateById(cameraGroup); //更新算法ids
		cameraGroupAlgorithmService.removeById(cameraGroupAlgorithm); //删除轮询组算法表中的算法
		cameraGroupController.saveAlgorithmConfig(groupId); //调用生成算法到算法总表的方法
		return Result.OK("删除成功!");
	}
	
	/**
	 * 批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "轮询组算法表-批量删除")
	@ApiOperation(value="轮询组算法表-批量删除", notes="轮询组算法表-批量删除")
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.cameraGroupAlgorithmService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "轮询组算法表-通过id查询")
	@ApiOperation(value="轮询组算法表-通过id查询", notes="轮询组算法表-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		CameraGroupAlgorithm cameraGroupAlgorithm = cameraGroupAlgorithmService.getById(id);
		return Result.OK(cameraGroupAlgorithm);
	}

  /**
   * 导出excel
   *
   * @param request
   * @param cameraGroupAlgorithm
   */
  @RequestMapping(value = "/exportXls")
  public ModelAndView exportXls(HttpServletRequest request, CameraGroupAlgorithm cameraGroupAlgorithm) {
      return super.exportXls(request, cameraGroupAlgorithm, CameraGroupAlgorithm.class, "轮询组算法表");
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
      return super.importExcel(request, response, CameraGroupAlgorithm.class);
  }

}
