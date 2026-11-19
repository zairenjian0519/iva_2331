package org.jeecg.modules.cameraGroup.controller;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.algorithmmanager.service.IAlgorithmManagerService;
import org.jeecg.modules.cameraGroup.entity.AlgorithmVo;
import org.jeecg.modules.cameraGroup.entity.CameraGroup;
import org.jeecg.modules.cameraGroup.entity.CameraVo;
import org.jeecg.modules.cameraGroup.service.ICameraGroupService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.cameraGroupAlgorithm.entity.CameraGroupAlgorithm;
import org.jeecg.modules.cameraGroupAlgorithm.service.ICameraGroupAlgorithmService;
import org.jeecg.modules.cameraGroupCamera.entity.CameraGroupCamera;
import org.jeecg.modules.cameraGroupCamera.service.ICameraGroupCameraService;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


 /**
 * @Description: 轮询组
 * @Author: jeecg-boot
 * @Date:   2023-02-14
 * @Version: V1.0
 */
@Slf4j
@Api(tags="轮询组")
@RestController
@RequestMapping("/cameraGroup/cameraGroup")
public class CameraGroupController extends JeecgController<CameraGroup, ICameraGroupService> {
	 @Autowired
	 private ICameraGroupService cameraGroupService;
	 @Autowired
	 private IAlgorithmManagerService algorithmManagerService;
	 @Autowired
	 private IKyCategoryService kyCategoryService;
	 @Autowired
	 private ICameraManagerService cameraManagerService;
	 @Autowired
	 private ICameraGroupCameraService cameraGroupCameraService;
	 @Autowired
	 private ICameraGroupAlgorithmService cameraGroupAlgorithmService;
	/**
	 * 分页列表查询
	 *
	 * @param cameraGroup
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@AutoLog(value = "相机组-分页列表查询")
	@ApiOperation(value="相机组-分页列表查询", notes="相机组-分页列表查询")
	@GetMapping(value = "/list")
	public Result<?> queryPageList(CameraGroup cameraGroup,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
//		Page<CameraGroup> page = new Page<CameraGroup>(pageNo, pageSize);
//		IPage<CameraGroup> pageList = cameraGroupService.pageList(page, cameraGroup);

		QueryWrapper<CameraGroup> queryWrapper = QueryGenerator.initQueryWrapper(cameraGroup, req.getParameterMap());
		Page<CameraGroup> page = new Page<CameraGroup>(pageNo, pageSize);
		IPage<CameraGroup> pageList = cameraGroupService.page(page, queryWrapper);

		for (CameraGroup group:pageList.getRecords()) {
			if (group.getCameraIds() != null) {
				String[] cameraIdsList = group.getCameraIds().split(",");
				List<CameraVo> cameraList = new ArrayList();
				for (String cId : cameraIdsList) {
					CameraVo cameraVo = new CameraVo();
					CameraManager cameraManager = cameraManagerService.getById(cId);
					cameraVo.setCameraId(cId);
					if (cameraManager != null) {
						cameraVo.setTitle(cameraManager.getCameraName());
					}
					if (!cId.equals("")&&cId!=null) {
						cameraList.add(cameraVo);
					}
				}
				group.setCameraList(cameraList);
			}
			if (group.getAlgorithmIds() != null) {
				String[] algorithmIdsList = group.getAlgorithmIds().split(",");
				List<AlgorithmVo> algorithmList = new ArrayList();
				for (String aId : algorithmIdsList) {
					AlgorithmVo algorithmVo = new AlgorithmVo();
					KyCategory kyCategory = kyCategoryService.getById(aId);
					algorithmVo.setAlgorithmId(aId);
					if (kyCategory != null) {
						algorithmVo.setAlgorithmName(kyCategory.getName());
					}
					if (!aId.equals("")&&aId!=null) {
						algorithmList.add(algorithmVo);
					}
				}
				group.setAlgorithmList(algorithmList);
			}
		}

		return Result.OK(pageList);
	}
	
	/**
	 * 添加
	 *
	 * @param cameraGroup
	 * @return
	 */
	@AutoLog(value = "相机组-添加")
	@ApiOperation(value="相机组-添加", notes="相机组-添加")
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody CameraGroup cameraGroup) {
		List<CameraGroup> cameraGroupList = cameraGroupService.list(new QueryWrapper<CameraGroup>()
				.lambda().eq(CameraGroup::getGroupName,cameraGroup.getGroupName()));
		if (cameraGroupList.size()!=0){
			return Result.error("已存在相同名称的轮询组！");
		}
//		cameraGroup.setRunStatus(0); //轮询组运行状态默认0：未启动
		cameraGroupService.save(cameraGroup);
		/* 分别存入轮询组相机表和轮询组算法表 */
		if (cameraGroup.getCameraIds()!=null) {
			List cameraList = Arrays.asList(cameraGroup.getCameraIds().split(","));
			for (int i = 0; i < cameraList.size(); i++) {
				CameraGroupCamera camera = new CameraGroupCamera();
				camera.setGroupId(cameraGroup.getId());
				camera.setCameraId(cameraList.get(i).toString());
				cameraGroupCameraService.save(camera);
			}
		}
		/* 分别存完组相机和组算法后 调用交叉生成算法方法存入算法总表*/
		this.saveAlgorithmConfig(cameraGroup.getId());
		return Result.OK("添加成功！");
	}



	/**
	 * 编辑
	 *
	 * @param cameraGroup
	 * @return
	 */
	@AutoLog(value = "相机组-编辑")
	@ApiOperation(value="相机组-编辑", notes="相机组-编辑")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<?> edit(@RequestBody CameraGroup cameraGroup) {
		String groupId = cameraGroup.getId();
		/* 传下来的相机、算法列表 */
		if (cameraGroup.getCameraIds()!=null) {
			List cameraIdsList = Arrays.asList(cameraGroup.getCameraIds().split(","));
			/* 已存在的相机对象列表 */
			List<CameraGroupCamera> cameraList = cameraGroupCameraService.getCameraByGroupId(groupId);
			/* 取id放到新的list 并与传下来的ids进行取并集去重 */
			List<String> list = new ArrayList<>();
			for (CameraGroupCamera camera : cameraList) {
				list.add(camera.getCameraId());
			}
			list.parallelStream().collect(Collectors.toList());
			cameraIdsList.parallelStream().collect(Collectors.toList());
			list.addAll(cameraIdsList);
			List<String> listAllDistinct = list.stream().distinct().collect(Collectors.toList()); //并集
			/* 得到去重后的并集列表 遍历如果轮询组相机表中有该id 则不进行操作，如果没有则新增 */
			if (!cameraGroup.getCameraIds().equals("")){
				for (String cameraId : listAllDistinct) {
					int size = cameraList.stream().filter(camera -> cameraId.equals(camera.getCameraId())).collect(Collectors.toList()).size();
					if (size == 0) {
						/* 存储相机表里没有记录的id */
						CameraGroupCamera camera = new CameraGroupCamera();
						camera.setGroupId(groupId);
						camera.setCameraId(cameraId);
						cameraGroupCameraService.save(camera);
					}
				}
			}
			/* 取传来的ids与轮询相机表里的id差集 删除表里去掉的相机 */
			List<String> reduceList = list.stream().filter(item -> !cameraIdsList.contains(item)).collect(Collectors.toList()); //差集
			for (String cameraId : reduceList) {
				List<CameraGroupCamera> oldCameraList = cameraList.stream().filter(camera -> cameraId.equals(camera.getCameraId())).collect(Collectors.toList());
				for (CameraGroupCamera camera : oldCameraList) {
					cameraGroupCameraService.removeById(camera);
				}
			}
		}
		cameraGroupService.updateById(cameraGroup);
		/* 生成相机对应算法到算法总表 */
		this.saveAlgorithmConfig(cameraGroup.getId());
		List<String> deviceManagementList = cameraGroupService.getDeviceIpByGroupId(groupId);
		return Result.OK("编辑成功!",deviceManagementList);
	}


	public void saveAlgorithmConfig(String groupId) {
		List<AlgorithmManager> algorithmManagerList = algorithmManagerService.getByGroupId(groupId); //总表
		List<CameraGroupCamera> cameraList = cameraGroupCameraService.getCameraByGroupId(groupId); //相机表
		List<CameraGroupAlgorithm> algorithmList = cameraGroupAlgorithmService.getAlgorithmByGroupId(groupId); //算法表
		CameraGroup cameraGroup = cameraGroupService.getById(groupId);
		/* 相机和算法有一方为空 就清空对应组算法表的算法 */
		if (cameraList.size()==0||algorithmList.size()==0){
			algorithmManagerService.removeByGroupId(groupId);
			cameraGroup.setRunStatus(0); //一方为空新建组状态为0 关闭
			cameraGroupService.updateById(cameraGroup);
		}
		if (cameraList.size() != 0 && algorithmList.size() != 0) { //都不为空才操作算法总表
			cameraGroup.setRunStatus(1); //都不为空组状态为1 运行
			cameraGroupService.updateById(cameraGroup);
			List<String> allCameraIdsList = new ArrayList();
			for (AlgorithmManager algorithmManager : algorithmManagerList){
				allCameraIdsList.add(algorithmManager.getBelongTo());
			}
			/* 已存在的相机对象列表 */
			/* 取id放到新的list 并与传下来的ids进行取并集去重 */
			List<String> list = new ArrayList<>();
			for (CameraGroupCamera camera : cameraList) {
				list.add(camera.getCameraId());
			}
			/*取轮询组相机表和总表的相机交集*/
			List<String> intersection = allCameraIdsList.stream().filter(item -> list.contains(item)).collect(Collectors.toList());
			/* 差集 */
			List<String> reduceList = allCameraIdsList.stream().filter(item -> !list.contains(item)).collect(Collectors.toList());
//			System.out.println("相机表："+list);
//			System.out.println("总表相机："+allCameraIdsList);
//			System.out.println("交集："+intersection);
//			System.out.println("差集："+reduceList);
			if (intersection.size()==0) {
				algorithmManagerService.removeByGroupId(groupId);
				for (String cameraId : list) {
					for (CameraGroupAlgorithm algorithm : algorithmList) {
						AlgorithmManager algorithmManager = new AlgorithmManager();
						/* 字段大部分相同，将a表相同字段赋值到b表，忽略id */
						BeanUtils.copyProperties(algorithm, algorithmManager, "id");
						algorithmManager.setIsRotate(1);
						algorithmManager.setBelongTo(cameraId);
						algorithmManagerService.save(algorithmManager);
					}
				}
			}else{
				for (String reduce : reduceList){
					algorithmManagerService.removeByCameraId(reduce);
				}
				for (String cameraId : list){
					if (!allCameraIdsList.contains(cameraId)){
						for (CameraGroupAlgorithm algorithm : algorithmList) {
							AlgorithmManager algorithmManager = new AlgorithmManager();
							/* 字段大部分相同，将a表相同字段赋值到b表，忽略id */
							BeanUtils.copyProperties(algorithm, algorithmManager, "id");
							algorithmManager.setIsRotate(1);
							algorithmManager.setBelongTo(cameraId);
							algorithmManagerService.save(algorithmManager);
						}
					}
				}
			}

			List<String> allAlgorithmIdsList = new ArrayList();
			for (AlgorithmManager algorithmManager : algorithmManagerList){
				allAlgorithmIdsList.add(algorithmManager.getModelName());
			}
			/* 已存在的算法对象列表 */
			List<String> list2 = new ArrayList<>();
			for (CameraGroupAlgorithm algorithm : algorithmList) {
				list2.add(algorithm.getAlgorithmId());
			}
			/*取轮询组算法和总表的算法交集*/
			List<String> intersection2 = allAlgorithmIdsList.stream().filter(item -> list2.contains(item)).collect(Collectors.toList());
			/* 差集 */
			List<String> reduceList2 = allAlgorithmIdsList.stream().filter(item -> !list2.contains(item)).collect(Collectors.toList());
//			System.out.println("算法表："+list2);
//			System.out.println("总表算法："+allAlgorithmIdsList);
//			System.out.println("交集："+intersection2);
//			System.out.println("差集："+reduceList2);
			if (intersection2.size()==0) {
				algorithmManagerService.removeByGroupId(groupId);
				for (String cameraId : list) {
					for (CameraGroupAlgorithm algorithm : algorithmList) {
						AlgorithmManager algorithmManager = new AlgorithmManager();
						/* 字段大部分相同，将a表相同字段赋值到b表，忽略id */
						BeanUtils.copyProperties(algorithm, algorithmManager, "id");
						algorithmManager.setIsRotate(1);
						algorithmManager.setBelongTo(cameraId);
						algorithmManagerService.save(algorithmManager);
					}
				}
			}
			else{
				for (String reduce : reduceList2){
					algorithmManagerService.removeByAlgId(reduce);
				}
				for (String algorithmId : list2){
					if (!allAlgorithmIdsList.contains(algorithmId)){
						for (String cameraId : list){
								AlgorithmManager algorithmManager = new AlgorithmManager();
								CameraGroupAlgorithm cameraGroupAlgorithm = cameraGroupAlgorithmService
										.getOne(new QueryWrapper<CameraGroupAlgorithm>().lambda().
												eq(CameraGroupAlgorithm::getAlgorithmId,algorithmId)
												.eq(CameraGroupAlgorithm::getGroupId,groupId));
								/* 字段大部分相同，将a表相同字段赋值到b表，忽略id */
								BeanUtils.copyProperties(cameraGroupAlgorithm, algorithmManager, "id");
								algorithmManager.setIsRotate(1);
								algorithmManager.setBelongTo(cameraId);
								algorithmManagerService.save(algorithmManager);
						}
					}
				}
			}
		}
	}


	 /**
	  * 改变状态
	  *
	  * @return
	  */
	 @AutoLog(value = "相机组-改变状态")
	 @ApiOperation(value="相机组-改变状态", notes="相机组-改变状态")
	 @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT,RequestMethod.POST})
	 public Result<?> changeStatus(@RequestBody JSONObject jsonObject) {
		 String id = jsonObject.getString("id");
		 Integer runStatus = jsonObject.getInteger("runStatus");
		 CameraGroup cameraGroup = cameraGroupService.getById(id);
		 if (runStatus==0){
			 cameraGroup.setRunStatus(runStatus);
			 cameraGroupService.updateById(cameraGroup);
		 }
		 if (runStatus==1){
			 if (cameraGroup.getCameraIds()!=null&&!cameraGroup.getCameraIds().equals("")&&cameraGroup.getAlgorithmIds()!=null&&!cameraGroup.getAlgorithmIds().equals("")) {
				 cameraGroup.setRunStatus(runStatus);
				 cameraGroupService.updateById(cameraGroup);
			 }else{
				 return Result.error("当相机和算法不为空才能启动");
			 }
		 }
		 return Result.OK("");
	 }

	 /**
	  * 修改轮询组信息
	  *
	  * @return
	  */
	 @AutoLog(value = "相机组-修改轮询组信息")
	 @ApiOperation(value="相机组-修改轮询组信息", notes="相机组-修改轮询组信息")
	 @RequestMapping(value = "/editGroup", method = {RequestMethod.PUT,RequestMethod.POST})
	 public Result<?> editGroup(@RequestBody JSONObject jsonObject) {
		 String id = jsonObject.getString("id");
		 String groupName = jsonObject.getString("groupName");
		 Integer pollingPeriod = jsonObject.getInteger("pollingPeriod");
		 CameraGroup cameraGroup = cameraGroupService.getById(id);
		 CameraGroup checkGroupName = cameraGroupService.getOne(new QueryWrapper<CameraGroup>().lambda().eq(CameraGroup::getGroupName,groupName));
		 if (null != checkGroupName && !checkGroupName.getId().equals(cameraGroup.getId())){
			 return Result.error("已存在相同名称的轮询组！");
		 }
		 cameraGroup.setGroupName(groupName);
		 cameraGroup.setPollingPeriod(pollingPeriod);
		 cameraGroupService.updateById(cameraGroup);
		 return Result.OK("编辑成功");
	 }


	 @AutoLog(value = "轮询组-编辑ROI")
	 @ApiOperation(value="轮询组-编辑ROI", notes="轮询组-编辑ROI")
	 @RequestMapping(value = "/editRoi", method = {RequestMethod.PUT,RequestMethod.POST})
	 public Result<String> editRoi(@RequestBody AlgorithmManager algorithmManager) {
		 AlgorithmManager alg = algorithmManagerService.getOne(new QueryWrapper<AlgorithmManager>().lambda()
				 .eq(AlgorithmManager::getBelongTo,algorithmManager.getBelongTo())
				 .eq(AlgorithmManager::getModelName,algorithmManager.getModelName())
				 .eq(AlgorithmManager::getGroupId,algorithmManager.getGroupId()));
		 if (alg!=null){
			 alg.setCheckArea(algorithmManager.getCheckArea());
			 alg.setInvalidArea(algorithmManager.getInvalidArea());
			 algorithmManagerService.updateById(alg);
		 }
		 return Result.OK("编辑成功!");
	 }


	 /**
	  * 通过组id，相机id，算法id查询roi区域
	  * @return
	  */
	 @AutoLog(value = "轮询组相机表-查询算法roi")
	 @ApiOperation(value="轮询组相机表-查询算法roi", notes="轮询组相机表-查询算法roi")
	 @GetMapping(value = "/getRoiById")
	 public Result<?> getRoiById(@RequestParam(name="groupId") String groupId,
								 @RequestParam(name="cameraId") String cameraId,
								 @RequestParam(name="algorithmId") String algorithmId) {
		 AlgorithmManager algorithmManager = algorithmManagerService.getOne(new QueryWrapper<AlgorithmManager>().lambda()
				 .eq(AlgorithmManager::getBelongTo,cameraId)
				 .eq(AlgorithmManager::getModelName,algorithmId)
				 .eq(AlgorithmManager::getGroupId,groupId));
		 return Result.OK(algorithmManager);
	 }

	 /**
	  *
	  * @param groupId
	  * @param algorithmId
	  * @return
	  */
	 @AutoLog(value = "轮询组相机表-查询组算法")
	 @ApiOperation(value="轮询组相机表-查询组算法", notes="轮询组相机表-查询组算法")
	 @GetMapping(value = "/getGroupAlgInfo")
	 public Result<?> getGroupAlgInfo(@RequestParam(name="groupId") String groupId,
								 @RequestParam(name="algorithmId") String algorithmId) {
		 CameraGroupAlgorithm cameraGroupAlgorithm = cameraGroupAlgorithmService.getOne(new QueryWrapper<CameraGroupAlgorithm>().lambda()
				 .eq(CameraGroupAlgorithm::getAlgorithmId,algorithmId)
				 .eq(CameraGroupAlgorithm::getGroupId,groupId));
//		 System.out.println(cameraGroupAlgorithm);
		 return Result.OK(cameraGroupAlgorithm);
	 }


	 @AutoLog(value = "根据组id查询所有ip")
	 @ApiOperation(value="根据组id查询所有ip", notes="根据组id查询所有ip")
	 @GetMapping(value = "/getDevIpList")
	 public Result<List> getDevIpList(@RequestParam(name="groupId") String groupId) {
		 List<String> deviceManagementList = cameraGroupService.getDeviceIpByGroupId(groupId);
		 return Result.OK(deviceManagementList);
	 }

	/**
	 * 通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "相机组-通过id删除")
	@ApiOperation(value="相机组-通过id删除", notes="相机组-通过id删除")
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		cameraGroupCameraService.removeByGroupId(id);
		cameraGroupAlgorithmService.removeByGroupId(id);
		algorithmManagerService.removeByGroupId(id); //删除相机组需要把算法表对应的相机以及算法删掉
		cameraGroupService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 * 批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "相机组-批量删除")
	@ApiOperation(value="相机组-批量删除", notes="相机组-批量删除")
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.cameraGroupService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "相机组-通过id查询")
	@ApiOperation(value="相机组-通过id查询", notes="相机组-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		CameraGroup cameraGroup = cameraGroupService.getById(id);
		return Result.OK(cameraGroup);
	}

  /**
   * 导出excel
   *
   * @param request
   * @param cameraGroup
   */
  @RequestMapping(value = "/exportXls")
  public ModelAndView exportXls(HttpServletRequest request, CameraGroup cameraGroup) {
      return super.exportXls(request, cameraGroup, CameraGroup.class, "相机组");
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
      return super.importExcel(request, response, CameraGroup.class);
  }

}
