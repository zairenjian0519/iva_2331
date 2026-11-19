package org.jeecg.modules.algorithmmanager.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.algorithmmanager.service.IAlgorithmManagerService;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.configjson.CreateConfigJson;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.mqtt.MqttClientConnect;
import org.jeecg.modules.mqtt.MqttClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

 /**
 * @Description: 算法实例
 * @Author: jeecg-boot
 * @Date:   2022-09-07
 * @Version: V1.0
 */
@Api(tags="算法实例")
@RestController
@RequestMapping("/algorithmmanager/algorithmManager")
@Slf4j
public class AlgorithmManagerController extends JeecgController<AlgorithmManager, IAlgorithmManagerService> {
	@Autowired
	private IAlgorithmManagerService algorithmManagerService;

	@Autowired
	private IDeviceManagementService deviceManagementService;

	@Autowired
	private ICameraManagerService cameraManagerService;

	@Autowired
	private CreateConfigJson createConfigJson;
	@Autowired
	private IKyCategoryService kyCategoryService;

	/**
	 * 分页列表查询
	 *
	 * @param algorithmManager
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "算法实例-分页列表查询")
	@ApiOperation(value="算法实例-分页列表查询", notes="算法实例-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<AlgorithmManager>> queryPageList(AlgorithmManager algorithmManager,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="999") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<AlgorithmManager> queryWrapper = QueryGenerator.initQueryWrapper(algorithmManager, req.getParameterMap());
		queryWrapper.lambda().ne(AlgorithmManager::getIsRotate,1);
		Page<AlgorithmManager> page = new Page<AlgorithmManager>(pageNo, pageSize);
		IPage<AlgorithmManager> pageList = algorithmManagerService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	/**
	 *   添加
	 *
	 * @param algorithmManager
	 * @return
	 */
	@AutoLog(value = "算法实例-添加")
	@ApiOperation(value="算法实例-添加", notes="算法实例-添加")
//	@RequiresPermissions("org.modules:algorithm_manager:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody AlgorithmManager algorithmManager) {
		algorithmManager.setIsRotate(0);
		if (algorithmManager.getModelName().equals("")){
			return Result.error("请选择一个算法");
		}
		//可添加相机数量
		if(!checkLicense(algorithmManager)){
			return Result.error("添加失败,可添加已达到最大授权数");
		}
		//可配置算法的相机数量
		if(!checkCameraLicense(algorithmManager)){
			return Result.error("添加失败,可配置算法相机已达到最大授权数");
		}
		algorithmManagerService.save(algorithmManager);
		return Result.OK("添加成功！");
	}

	/**
	 * 可添加相机数量的授权检查
	 * */
	private boolean checkLicense(AlgorithmManager algManager){
		DeviceManagement dev = deviceManagementService.getByCameraId(algManager.getBelongTo());
		if(null == dev) return false;
		int nowNum = algorithmManagerService.getLicenseNumByDevId(dev.getId());
		return nowNum < dev.getAlgorithmNum();
	}

	 /**
	  * 可配置算法的相机数量授权检查
	  * */
	 private boolean checkCameraLicense(AlgorithmManager algManager){
		 Boolean enough = true;
		 String cameraId = algManager.getBelongTo();
		 DeviceManagement dev = deviceManagementService.getByCameraId(cameraId);
		 if(null == dev) return false;
		 int nowNum = algorithmManagerService.getCameraLicense(dev.getId());
//		 System.out.println("设备信息："+dev);
//		 System.out.println("设备："+dev.getDeviceName()+"，有："+nowNum);

		 List<AlgorithmManager> algorithmManagerList = algorithmManagerService.list(new QueryWrapper<AlgorithmManager>().lambda()
				 .in(AlgorithmManager::getBelongTo,cameraId)
				 .ne(AlgorithmManager::getIsRotate,1));
		 System.out.println(algorithmManagerList.size());
		 System.out.println(nowNum);
		 if (nowNum >= dev.getAlgorithmCameraNum()){
			 enough = false;
		 }
		 if (nowNum >= dev.getAlgorithmCameraNum()&&!algorithmManagerList.isEmpty()){
			 enough = true;
		 }
		 System.out.println(enough);
		 return enough;
	 }

	/**
	 *  编辑
	 *
	 * @param algorithmManager
	 * @return
	 */
	@AutoLog(value = "算法实例-编辑")
	@ApiOperation(value="算法实例-编辑", notes="算法实例-编辑")
//	@RequiresPermissions("org.modules:algorithm_manager:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody AlgorithmManager algorithmManager) {
		if (algorithmManager.getModelName().equals("")||algorithmManager.getModelName()==null){
			return Result.error("请选择一个算法");
		}
		algorithmManagerService.updateById(algorithmManager);
		return Result.OK("编辑成功!");
	}

	 @ApiOperation(value="算法实例-更改相机状态", notes="算法实例-更改相机状态")
	 @GetMapping(value = "/setStatus")
	 public Result<String> setStatus(@RequestParam(name="id") String cameraId,@RequestParam(name="status") String status) {
		 if(StringUtils.isNotEmpty(status) && status.equals("Y")){
			 if(sendCameraStatus(cameraId, 1)){
				 algorithmManagerService.updateCameraStatus(cameraId, status);
				 return Result.OK("开启成功");
			 }else{
				 return Result.error("开启失败,发送命令到算法失败");
			 }
		 }
		 if(sendCameraStatus(cameraId, 2)){
			 algorithmManagerService.updateCameraStatus(cameraId, status);
			 return Result.OK("关闭成功");
		 }else{
			 return Result.error("关闭失败,发送命令到算法失败");
		 }
	 }

	 @ApiOperation(value="算法实例-获取相机状态", notes="算法实例-获取相机状态")
	 @GetMapping(value = "/getStatus")
	 public Result<JSONObject> getStatus(@RequestParam(name="id") String cameraId) {
		CameraManager cameraManager = cameraManagerService.getById(cameraId);
		 JSONObject obj = new JSONObject();
		if(null == cameraManager){
			obj.put("runStatus", "N");
			return Result.OK(obj);
		}
		 obj.put("runStatus", cameraManager.getRunStatus());
		 return Result.OK(obj);
	 }

	 @ApiOperation(value="算法实例-获取相机图片", notes="算法实例-获取相机图片")
	 @GetMapping(value = "/getImgInfo")
	 public Result<JSONObject> getImgInfo(@RequestParam(name="cameraId") String cameraId) {
		 CameraManager cameraManager = cameraManagerService.getById(cameraId);
		 DeviceManagement dev = deviceManagementService.getByCameraId(cameraId);
		 JSONObject obj = new JSONObject();
		 if(null == cameraManager || null == dev){
			 obj.put("ip", "127.0.0.1");
			 obj.put("imgSrc", "/cameraImg/" + cameraId + ".png");
		 }else{
			 try {
				 JSONObject info = new JSONObject();
				 info.put("refresh", 1);
				 MqttClientService.getInstance().getMqttClient(dev.getDeviceIp()).pub("web_picrefresh", info.toJSONString(),2);
			 } catch (MqttException e) {
			 }
			 obj.put("ip", dev.getDeviceIp());
			 obj.put("imgSrc", cameraManager.getPic());
		 }
		 return Result.OK(obj);
	 }

	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "算法实例-通过id删除")
	@ApiOperation(value="算法实例-通过id删除", notes="算法实例-通过id删除")
//	@RequiresPermissions("org.modules:algorithm_manager:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		String msg = "删除成功!";
		DeviceManagement deviceManagement = deviceManagementService.getByAlgorithmId(id);
		try {
			//确保设备是可连接状态再删除算法，并更新配置文件
			MqttClientConnect clientConnect  = MqttClientService.getInstance().getMqttClient(deviceManagement.getDeviceIp());
			if (null != clientConnect && clientConnect.getMqttClient().isConnected()) {
				algorithmManagerService.removeById(id);
				createConfigJson.saveAlgConfig(deviceManagement.getId(),deviceManagement.getDeviceIp());
			}else{
				return Result.error("设备连接失败，请确认连接后再次尝试");
			}
		} catch (MqttException e) {
			msg = "设备连接失败，请确认连接后再次尝试";
			e.printStackTrace();
			return Result.error(msg);
		}
		return Result.OK(msg);
	}

	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "算法实例-批量删除")
	@ApiOperation(value="算法实例-批量删除", notes="算法实例-批量删除")
//	@RequiresPermissions("org.modules:algorithm_manager:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.algorithmManagerService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}

	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "算法实例-通过id查询")
	@ApiOperation(value="算法实例-通过id查询", notes="算法实例-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<AlgorithmManager> queryById(@RequestParam(name="id",required=true) String id) {
		AlgorithmManager algorithmManager = algorithmManagerService.getById(id);
		if(algorithmManager==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(algorithmManager);
	}

	 @GetMapping("/restarstPythone")
	 public Result restartPythone() {
		 System.out.println("重启接口调用");
		 //拼接指令
		 String cmd = "systemctl restart ai_server";
		 String cmd1 = "systemctl restart ai_engine";
		 exect(cmd);

		 exect(cmd1);
		 return Result.OK("算法服务重启成功,请勿连续点击");
	 }

	 public void exect(String cmd) {
		 try {
			 //通过运行时环境.exec方法执行指定的指令
			 Process exec = Runtime.getRuntime().exec(cmd);
			 //创建用于读取命令
			 BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()));//获取命令的的标准输出流.将其封装成字符流
			 //定义变量存储读取的命令行
			 String line;
			 //循环读取命令,直到读取命令为空
			 while ((line = reader.readLine()) != null) {
				 //讲每一行命令输出到控制台
				 System.out.println(line);
				 log.info("执行成功指令 : " + line);
			 }
			 //等待命令执行完成,并获取命令退出码
			 int exitCode = exec.waitFor();
			 //打印命令的退出码到控制台
			 System.out.println("Command exited with code" + exitCode);
		 } catch (IOException | InterruptedException e) {
			 log.info("执行失败");
			 throw new JeecgBootException("算法服务重启失败,请手动重新启动");

		 }
	 }

	 /**
	  * 通过id查询阈值
	  *
	  * @param id
	  * @return
	  */
	 //@AutoLog(value = "算法实例-通过id查询")
	 @ApiOperation(value="算法实例-通过id查询阈值", notes="算法实例-通过id查询阈值")
	 @GetMapping(value = "/getThresholdById")
	 public Result<?> getThresholdById(@RequestParam(name="id",required=true) String id) {
		 KyCategory kyCategory = kyCategoryService.getById(id);
		 JSONObject jsonObject = new JSONObject();
		 if (kyCategory!=null){
			 jsonObject.put("threshold",kyCategory.getThreshold());
			 jsonObject.put("targetThreshold",kyCategory.getTargetThreshold());

			 // 添加iouThreshold、sensitivity、timeSpace、hitCount字段
			 jsonObject.put("iouThreshold",kyCategory.getIouThreshold());
			 jsonObject.put("sensitivity",kyCategory.getSensitivity());
			 jsonObject.put("timeSpace",kyCategory.getTimeSpace());
			 jsonObject.put("hitCount", kyCategory.getHitCount());
		 }
		 return Result.OK(jsonObject);
	 }

    /**
    * 导出excel
    *
    * @param request
    * @param algorithmManager
    */
    @RequiresPermissions("org.modules:algorithm_manager:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, AlgorithmManager algorithmManager) {
        return super.exportXls(request, algorithmManager, AlgorithmManager.class, "算法实例");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("algorithm_manager:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, AlgorithmManager.class);
    }


	 @ApiOperation(value="算法实例-查询相机和授权", notes="算法实例-查询相机和授权")
	 @GetMapping(value = "/cameraNums")
	 public Result<JSONObject> cameraNums() {
		JSONObject 	 obj = new JSONObject();
		List<DeviceManagement> list = deviceManagementService.list();
		int cameraOn =0, cameraTotal=0, algOn=0, altTotal=0;
		for(DeviceManagement dev: list){
			altTotal += dev.getAlgorithmNum() == null ? 999: dev.getAlgorithmNum();
			algOn += deviceManagementService.getUsedAlgNum(dev.getId());
		}
		cameraTotal = (int) cameraManagerService.count();
		cameraOn = (int) cameraManagerService.count((Wrapper<CameraManager>) new QueryWrapper().eq("camera_status", "1"));

		obj.put("on", cameraOn);
		obj.put("total", cameraTotal);
		obj.put("algOn", algOn);
		obj.put("algAll", altTotal);
		return Result.OK(obj);
	 }

	 private boolean sendCameraStatus(String cameraId, int status){
		DeviceManagement dev = deviceManagementService.getByCameraId(cameraId);
		if(null != dev){
			if(StringUtils.isNotEmpty(dev.getDeviceIp())) {
				try {
					MqttClientService.getInstance().getMqttClient(dev.getDeviceIp()).pub("detectstatus", cameraId+":"+status);
					return true;
				} catch (MqttException e) {
					log.error(e.getMessage());
				}
			}
		}
		return false;
	 }

}
