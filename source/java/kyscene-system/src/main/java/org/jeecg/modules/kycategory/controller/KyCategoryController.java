package org.jeecg.modules.kycategory.controller;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.algorithmmanager.service.IAlgorithmManagerService;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.entity.TreeVo;
import org.jeecg.modules.kycategory.service.IKyCategoryService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.aspect.annotation.AutoLog;

 /**
 * @Description: 分类数据
 * @Author: jeecg-boot
 * @Date:   2022-08-08
 * @Version: V1.0
 */
@Api(tags="分类数据")
@RestController
@RequestMapping("/kycategory/kyCategory")
@Slf4j
public class KyCategoryController extends JeecgController<KyCategory, IKyCategoryService>{
	@Autowired
	private IKyCategoryService kyCategoryService;
	@Autowired
	private ICameraManagerService cameraManagerService;

	@Autowired
	private IAlgorithmManagerService algorithmManagerService;

	/**
	 * 分页列表查询
	 *
	 * @param kyCategory
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "分类数据-分页列表查询")
	@ApiOperation(value="分类数据-分页列表查询", notes="分类数据-分页列表查询")
	@GetMapping(value = "/rootList")
	public Result<IPage<KyCategory>> queryPageList(KyCategory kyCategory,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		String hasQuery = req.getParameter("hasQuery");
        if(hasQuery != null && "true".equals(hasQuery)){
            QueryWrapper<KyCategory> queryWrapper =  QueryGenerator.initQueryWrapper(kyCategory, req.getParameterMap());
            List<KyCategory> list = kyCategoryService.queryTreeListNoPage(queryWrapper);
            IPage<KyCategory> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        }else{
            String parentId = kyCategory.getPid();
            if (oConvertUtils.isEmpty(parentId)) {
                parentId = "0";
            }
            kyCategory.setPid(null);
            QueryWrapper<KyCategory> queryWrapper = QueryGenerator.initQueryWrapper(kyCategory, req.getParameterMap());
            // 使用 eq 防止模糊查询
            queryWrapper.eq("pid", parentId);
            Page<KyCategory> page = new Page<KyCategory>(pageNo, pageSize);
            IPage<KyCategory> pageList = kyCategoryService.page(page, queryWrapper);
            return Result.OK(pageList);
        }
	}

	 /**
	  * 【vue3专用】加载节点的子数据
	  *
	  * @param pid
	  * @return
	  */
	 @RequestMapping(value = "/loadTreeChildren", method = RequestMethod.GET)
	 public Result<List<SelectTreeModel>> loadTreeChildren(@RequestParam(name = "pid") String pid) {
		 Result<List<SelectTreeModel>> result = new Result<>();
		 try {
			 List<SelectTreeModel> ls = kyCategoryService.queryListByPid(pid);
			 result.setResult(ls);
			 result.setSuccess(true);
		 } catch (Exception e) {
			 e.printStackTrace();
			 result.setMessage(e.getMessage());
			 result.setSuccess(false);
		 }
		 return result;
	 }

	 /**
	  * 【vue3专用】加载一级节点/如果是同步 则所有数据
	  *
	  * @param async
	  * @param pcode
	  * @return
	  */
	 @RequestMapping(value = "/loadTreeRoot", method = RequestMethod.GET)
	 public Result<List<SelectTreeModel>> loadTreeRoot(@RequestParam(name = "async") Boolean async, @RequestParam(name = "pcode") String pcode, @RequestParam(name="category") String cate) {
		 Result<List<SelectTreeModel>> result = new Result<>();
		 try {
			 List<SelectTreeModel> ls = kyCategoryService.queryListByCode(pcode, cate);
			 if (!async) {
				 loadAllChildren(ls);
			 }
			 result.setResult(ls);
			 result.setSuccess(true);
		 } catch (Exception e) {
			 e.printStackTrace();
			 result.setMessage(e.getMessage());
			 result.setSuccess(false);
		 }
		 return result;
	 }

	 /**
	  * 【vue3专用】递归求子节点 同步加载用到
	  *
	  * @param ls
	  */
	 private void loadAllChildren(List<SelectTreeModel> ls) {
		 for (SelectTreeModel tsm : ls) {
			 List<SelectTreeModel> temp = kyCategoryService.queryListByPid(tsm.getKey());
			 if (temp != null && temp.size() > 0) {
				 tsm.setChildren(temp);
				 loadAllChildren(temp);
			 }
		 }
	 }

	 /**
      * 获取子数据
      * @param kyCategory
      * @param req
      * @return
      */
	//@AutoLog(value = "分类数据-获取子数据")
	@ApiOperation(value="分类数据-获取子数据", notes="分类数据-获取子数据")
	@GetMapping(value = "/childList")
	public Result<IPage<KyCategory>> queryPageList(KyCategory kyCategory,HttpServletRequest req) {
		QueryWrapper<KyCategory> queryWrapper = QueryGenerator.initQueryWrapper(kyCategory, req.getParameterMap());
		List<KyCategory> list = kyCategoryService.list(queryWrapper);
		IPage<KyCategory> pageList = new Page<>(1, 10, list.size());
        pageList.setRecords(list);
		return Result.OK(pageList);
	}

    /**
      * 批量查询子节点
      * @param parentIds 父ID（多个采用半角逗号分割）
      * @return 返回 IPage
      * @param parentIds
      * @return
      */
	//@AutoLog(value = "分类数据-批量获取子数据")
    @ApiOperation(value="分类数据-批量获取子数据", notes="分类数据-批量获取子数据")
    @GetMapping("/getChildListBatch")
    public Result getChildListBatch(@RequestParam("parentIds") String parentIds) {
        try {
            QueryWrapper<KyCategory> queryWrapper = new QueryWrapper<>();
            List<String> parentIdList = Arrays.asList(parentIds.split(","));
            queryWrapper.in("pid", parentIdList);
            List<KyCategory> list = kyCategoryService.list(queryWrapper);
            IPage<KyCategory> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error("批量查询子节点失败：" + e.getMessage());
        }
    }


	 @AutoLog(value = "分类数据-告警类型树")
	 @ApiOperation(value="分类数据-告警类型树", notes="分类数据-告警类型树")
	 @GetMapping(value = "/categoryTree")
	 public Result<?> getTree(@RequestParam(defaultValue = "0", required = false) String category) {
		 try {
			 // 参数校验
			 if ((!"0".equals(category) && !"1".equals(category))) {
				 return Result.error("分类参数不合法，仅支持0或1");
			 }

			 // 获取指定分类的数据
			 List<KyCategory> records = kyCategoryService.list(
					 new QueryWrapper<KyCategory>().lambda()
							 .eq(KyCategory::getCategory, category)
			 );

			 // 构建树形结构
			 List<TreeVo> treeList = buildTree(records);

			 return Result.OK(treeList);
		 } catch (Exception e) {
			 log.error("构建分类树失败，category={}", category, e);
			 return Result.error("获取分类树失败：" + e.getMessage());
		 }
	 }

	 /**
	  * 构建树形结构
	  * @param records 分类数据列表
	  * @return 树形结构列表
	  */
	 private List<TreeVo> buildTree(List<KyCategory> records) {
		 List<TreeVo> treeList = new LinkedList<>();

		 if (records == null || records.isEmpty()) {
			 return treeList;
		 }

		 // 先构建所有节点的映射关系，避免递归中的重复查找
		 Map<String, TreeVo> nodeMap = new HashMap<>();
		 List<KyCategory> rootNodes = new ArrayList<>();

		 // 第一次遍历：创建所有节点并识别根节点
		 for (KyCategory category : records) {
			 TreeVo node = new TreeVo();
			 node.setValue(category.getId());
			 node.setTitle(category.getName());
			 node.setChildren(new LinkedList<>());
			 nodeMap.put(category.getId(), node);

			 // 识别根节点
			 if ("0".equals(category.getPid())) {
				 rootNodes.add(category);
			 }
		 }

		 // 第二次遍历：建立父子关系
		 for (KyCategory category : records) {
			 String parentId = category.getPid();
			 String currentId = category.getId();

			 // 如果不是根节点，建立父子关系
			 if (!"0".equals(parentId) && nodeMap.containsKey(parentId)) {
				 TreeVo parent = nodeMap.get(parentId);
				 TreeVo current = nodeMap.get(currentId);
				 if (parent != null && current != null) {
					 parent.getChildren().add(current);
				 }
			 }
		 }

		 // 收集根节点对应的TreeVo对象
		 for (KyCategory root : rootNodes) {
			 TreeVo rootNode = nodeMap.get(root.getId());
			 if (rootNode != null) {
				 treeList.add(rootNode);
			 }
		 }

		 return treeList;
	 }

	/**
	 *   添加
	 *
	 * @param kyCategory
	 * @return
	 */
	@AutoLog(value = "分类数据-添加")
//	@RequiresPermissions("org.jeecg.modules:kycategory:add")
	@ApiOperation(value="分类数据-添加", notes="分类数据-添加")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody KyCategory kyCategory) {
		if(kyCategory.getName().equals("人员聚集")){
			if (kyCategory.getTargetThreshold() < 4){
				return Result.error("人员聚集目标数量阈值不能小于4");
			}
		}
		if (kyCategory.getCategory().equals("0")) {
			KyCategory ky = kyCategoryService.getOne(new QueryWrapper<KyCategory>().lambda()
							.eq(KyCategory::getCategory,"0")
					.eq(KyCategory::getCode, kyCategory.getCode()));
			if (ky != null) {
				return Result.error("已存在相同编码，请重新输入！");
			}
		}
		//自定义分组规则：父级与父级不可重复，父级下的子级在同父级下不可重复，但是可以与不同父级下的子级重复

		if (kyCategory.getCategory().equals("1")) {
			if (kyCategory.getPid()==null){
				KyCategory parent = kyCategoryService.getOne(new QueryWrapper<KyCategory>()
						.lambda()
						.eq(KyCategory::getCategory,"1")
						.eq(KyCategory::getName,kyCategory.getName())
						.eq(KyCategory::getPid,0));
				if (parent != null) {
					return Result.error("已存在相同名称，请重新输入！");
				}
			}
			KyCategory ky = kyCategoryService.getOne(new QueryWrapper<KyCategory>().lambda()
					.eq(KyCategory::getCategory,"1")
					.eq(KyCategory::getPid,kyCategory.getPid())
					.eq(KyCategory::getName, kyCategory.getName()));
			if (ky != null) {
				return Result.error("已存在相同名称，请重新输入！");
			}
		}
		KyCategory ky = kyCategoryService.getById(kyCategory.getPid());
		if (ky!=null&&!ky.getPid().equals("0")){
			return Result.error("只允许创建两级分组");
		}
		kyCategoryService.addKyCategory(kyCategory);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param kyCategory
	 * @return
	 */
	@AutoLog(value = "分类数据-编辑")
//	@RequiresPermissions("org.jeecg.modules:kycategory:edit")
	@ApiOperation(value="分类数据-编辑", notes="分类数据-编辑")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody KyCategory kyCategory) {
		if (kyCategory.getCategory().equals("1")) {
			if (kyCategory.getPid().equals("0")) {
				KyCategory parent = kyCategoryService.getOne(new QueryWrapper<KyCategory>()
						.lambda()
						.eq(KyCategory::getCategory,"1")
						.eq(KyCategory::getName, kyCategory.getName())
						.eq(KyCategory::getPid, 0));
			if (parent != null&&!kyCategory.getId().equals(parent.getId())) {
				return Result.error("已存在相同名称，请重新输入！");
			}
		}
			KyCategory ky = kyCategoryService.getOne(new QueryWrapper<KyCategory>().lambda()
					.eq(KyCategory::getCategory,"1")
					.eq(KyCategory::getPid,kyCategory.getPid())
					.eq(KyCategory::getName, kyCategory.getName()));
			if (ky != null&&!kyCategory.getId().equals(ky.getId())) {
				return Result.error("已存在相同名称，请重新输入！");
			}
		}
		kyCategoryService.updateKyCategory(kyCategory);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "分类数据-通过id删除") //与批量删除共用一个接口
//	@RequiresPermissions("org.jeecg.modules:kycategory:delete")
	@ApiOperation(value="分类数据-通过id删除", notes="分类数据-通过id删除")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String ids) {
//		System.out.println("ids = " + ids);
		List<String> idList = Arrays.asList(ids.split(","));
		for(String id:idList) {
			System.out.println(id);
			if (id.equals("0")) {
				return Result.error("未分组类型不可删除！");
			}
			KyCategory kyCategory = kyCategoryService.getById(id);
			if (kyCategory != null) {
				KyCategory kyParent = kyCategoryService.getById(kyCategory.getPid());
				if (null != kyCategory && kyCategory.getCategory().equals("0") && checkDataUse(kyCategory.getId())) {
					return Result.error("删除失败，该类型已被使用，请先删除相机算法");
				}

				List<String> treeIds = cameraManagerService.getTreeId(id);
				if (!treeIds.isEmpty()) {
					cameraManagerService.deleteCameraLocation(treeIds); //删除区域后，将对应区域的相机变为未分组
					kyCategoryService.removeByIds(treeIds);
					List<KyCategory> kyCategoryList = kyCategoryService.list(new QueryWrapper<KyCategory>().lambda().eq(KyCategory::getPid, kyCategory.getPid()));
					if (kyCategoryList.size() == 0) {
						kyParent.setHasChild("0"); //父级下子级为空将hasChild置为0 防止页面显示loading
						kyCategoryService.updateById(kyParent);
					}
				}
			}
		}
		return Result.OK("删除成功!");
	}

	//判断是否被使用
	 private boolean checkDataUse(String id) {
		 List<AlgorithmManager> list = algorithmManagerService.list(new QueryWrapper<AlgorithmManager>().lambda().eq(AlgorithmManager::getModelName, id));
		 return list.size() > 0;
	 }

	 /**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "分类数据-批量删除")
	@ApiOperation(value="分类数据-批量删除", notes="分类数据-批量删除")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.kyCategoryService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "分类数据-通过id查询")
	@ApiOperation(value="分类数据-通过id查询", notes="分类数据-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<KyCategory> queryById(@RequestParam(name="id",required=true) String id) {
		KyCategory kyCategory = kyCategoryService.getById(id);
		if(kyCategory==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(kyCategory);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param kyCategory
    */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, KyCategory kyCategory) {
		return super.exportXls(request, kyCategory, KyCategory.class, "分类数据");
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
		return super.importExcel(request, response, KyCategory.class);
    }

}
