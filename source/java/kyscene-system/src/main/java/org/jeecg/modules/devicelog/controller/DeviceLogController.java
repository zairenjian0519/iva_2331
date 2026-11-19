package org.jeecg.modules.devicelog.controller;

import java.util.Arrays;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONObject;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.devicelog.entity.DeviceLog;
import org.jeecg.modules.devicelog.service.IDeviceLogService;

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
 * @Description: 设备升级日志
 * @Author: jeecg-boot
 * @Date:   2022-09-06
 * @Version: V1.0
 */
@Api(tags="设备升级日志")
@RestController
@RequestMapping("/devicelog/deviceLog")
@Slf4j
public class DeviceLogController extends JeecgController<DeviceLog, IDeviceLogService> {
	@Autowired
	private IDeviceLogService deviceLogService;
	
	/**
	 * 分页列表查询
	 *
	 * @param deviceLog
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "设备升级日志-分页列表查询")
	@ApiOperation(value="设备升级日志-分页列表查询", notes="设备升级日志-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<DeviceLog>> queryPageList(DeviceLog deviceLog,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<DeviceLog> queryWrapper = QueryGenerator.initQueryWrapper(deviceLog, req.getParameterMap());
		Page<DeviceLog> page = new Page<DeviceLog>(pageNo, pageSize);
		IPage<DeviceLog> pageList = deviceLogService.page(page, queryWrapper);
		return Result.OK(pageList);
	}


	 /**
	  *
	  * @param jsonObject
	  */
	 @AutoLog(value = "算法升级日志-添加")
	 @ApiOperation(value="算法升级日志-添加", notes="算法升级日志-添加")
	 //@RequiresPermissions("org.modules:device_log:add")
	 @PostMapping(value = "/algLog")
	 public void algLog(@RequestBody JSONObject jsonObject) {
		 String deviceId = jsonObject.getString("deviceId");
		 String msg = jsonObject.getString("msg");
		 Integer code = jsonObject.getInteger("code");
		 DeviceLog devLog = new DeviceLog();
		 LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		 if (code!=null && code == 2000){
			 devLog.setUpPeople(user.getRealname());
			 devLog.setDevid(deviceId);
			 devLog.setDownStatus(1);
			 devLog.setDownTime(new Date());
			 devLog.setBackupStatus(1);
			 devLog.setBackupTime(new Date());
			 devLog.setUpStatus(1);
			 devLog.setUpTime(new Date());
			 devLog.setResultStatus(1);
			 devLog.setResultTime(new Date());
			 devLog.setDownDes(msg);
			 devLog.setBackupDes(msg);
			 devLog.setResultDes(msg);
			 devLog.setUpDes(msg);
		 }else{
			 devLog.setUpPeople(user.getRealname());
			 devLog.setDevid(deviceId);
			 devLog.setDownStatus(0);
			 devLog.setDownTime(new Date());
			 devLog.setBackupStatus(0);
			 devLog.setBackupTime(new Date());
			 devLog.setUpStatus(0);
			 devLog.setUpTime(new Date());
			 devLog.setResultStatus(0);
			 devLog.setResultTime(new Date());
			 devLog.setDownDes(msg);
			 devLog.setBackupDes(msg);
			 devLog.setResultDes(msg);
			 devLog.setUpDes(msg);
		 }
		 deviceLogService.save(devLog);
	 }

	/**
	 *   添加
	 *
	 * @param deviceLog
	 * @return
	 */
	@AutoLog(value = "设备升级日志-添加")
	@ApiOperation(value="设备升级日志-添加", notes="设备升级日志-添加")
	//@RequiresPermissions("org.modules:device_log:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody DeviceLog deviceLog) {
		deviceLogService.save(deviceLog);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param deviceLog
	 * @return
	 */
	@AutoLog(value = "设备升级日志-编辑")
	@ApiOperation(value="设备升级日志-编辑", notes="设备升级日志-编辑")
	//@RequiresPermissions("org.modules:device_log:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody DeviceLog deviceLog) {
		deviceLogService.updateById(deviceLog);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "设备升级日志-通过id删除")
	@ApiOperation(value="设备升级日志-通过id删除", notes="设备升级日志-通过id删除")
	//@RequiresPermissions("org.modules:device_log:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		deviceLogService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "设备升级日志-批量删除")
	@ApiOperation(value="设备升级日志-批量删除", notes="设备升级日志-批量删除")
	//@RequiresPermissions("org.modules:device_log:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.deviceLogService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "设备升级日志-通过id查询")
	@ApiOperation(value="设备升级日志-通过id查询", notes="设备升级日志-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<DeviceLog> queryById(@RequestParam(name="id",required=true) String id) {
		DeviceLog deviceLog = deviceLogService.getById(id);
		if(deviceLog==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(deviceLog);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param deviceLog
    */
    //@RequiresPermissions("org.modules:device_log:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, DeviceLog deviceLog) {
        return super.exportXls(request, deviceLog, DeviceLog.class, "设备升级日志");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    //@RequiresPermissions("device_log:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, DeviceLog.class);
    }

}
