package org.jeecg.modules.alarmAudit.controller;

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
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.modules.alarmAudit.entity.AlarmAudit;
import org.jeecg.modules.alarmAudit.service.IAlarmAuditService;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * @Description: 告警审核
 * @Author: jeecg-boot
 * @Date: 2022-08-30
 * @Version: V1.0
 */
@Api(tags = "告警审核")
@RestController
@RequestMapping("/alarmAudit/alarmAudit")
@Slf4j
public class AlarmAuditController extends JeecgController<AlarmAudit, IAlarmAuditService> {
    @Autowired
    private IAlarmAuditService alarmAuditService;

    @Autowired
    private ISysUserService sysUserService;

    /**
     * 分页列表查询
     *
     * @param alarmAudit
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    //@AutoLog(value = "告警审核-分页列表查询")
    @ApiOperation(value = "告警审核-分页列表查询", notes = "告警审核-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<AlarmAudit>> queryPageList(AlarmAudit alarmAudit,
                                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                   HttpServletRequest req) {
        QueryWrapper<AlarmAudit> queryWrapper = QueryGenerator.initQueryWrapper(alarmAudit, req.getParameterMap());
        Page<AlarmAudit> page = new Page<AlarmAudit>(pageNo, pageSize);
        IPage<AlarmAudit> pageList = alarmAuditService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 通过id审核
     *
     * @param
     * @return
     */
    @AutoLog(value = "告警审核-通过id审核")
    @ApiOperation(value = "告警审核-通过id审核", notes = "告警审核-通过id审核")
//	 @RequiresPermissions("org.jeecg.modules:alarm:audit")
    @PostMapping(value = "/audit")
    public Result<String> audit(@RequestBody AlarmAudit alarmAudit, HttpServletRequest req) throws ParseException {
        // 获取当前时间
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date newDate = sdf.parse(sdf.format(date));

        //获取当前用户名字
        String userName = JwtUtil.getUserNameByToken(req);
        SysUser sysUser = sysUserService.getUserByName(userName);

        if (StrUtil.isNotBlank(alarmAudit.getAlarmId())) {
            AlarmAudit alarmAuditFromDb = alarmAuditService.getOne(new QueryWrapper<AlarmAudit>().lambda().eq(AlarmAudit::getAlarmId, alarmAudit.getAlarmId()));
            if (alarmAuditFromDb != null) {
                alarmAuditFromDb.setAuditReason(alarmAudit.getAuditReason());
                alarmAuditFromDb.setAuditPeople(sysUser.getRealname()); //审核人为当前用户 修改为存放用户id
                alarmAuditFromDb.setAuditStatus("1"); //变为1：已审核
                alarmAuditFromDb.setAuditResult(alarmAudit.getAuditResult());//0：作废，1：通过
                alarmAuditFromDb.setAuditTime(newDate);
                alarmAuditService.updateById(alarmAuditFromDb);
            } else {
                AlarmAudit audit = new AlarmAudit();
                audit.setAlarmId(alarmAudit.getAlarmId());
                audit.setAuditReason(alarmAudit.getAuditReason());
                audit.setCreateTime(new Date());
                audit.setAuditStatus("1");
                audit.setAuditResult(alarmAudit.getAuditResult());
                audit.setAuditPeople(sysUser.getRealname());
                audit.setAuditTime(newDate);
                alarmAuditService.save(audit);
            }
            return Result.OK("审核成功！");
        } else {
           return Result.error("未传递必要的参数：alarmId");
        }
    }

    /**
     * 添加
     *
     * @param alarmAudit
     * @return
     */
    @AutoLog(value = "告警审核-添加")
    @ApiOperation(value = "告警审核-添加", notes = "告警审核-添加")
    //@RequiresPermissions("org.jeecg.modules:alarm_audit:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody AlarmAudit alarmAudit) {
        alarmAuditService.save(alarmAudit);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param alarmAudit
     * @return
     */
    @AutoLog(value = "告警审核-编辑")
    @ApiOperation(value = "告警审核-编辑", notes = "告警审核-编辑")
    //@RequiresPermissions("org.jeecg.modules:alarm_audit:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody AlarmAudit alarmAudit) {
        alarmAuditService.updateById(alarmAudit);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "告警审核-通过id删除")
    @ApiOperation(value = "告警审核-通过id删除", notes = "告警审核-通过id删除")
    //@RequiresPermissions("org.jeecg.modules:alarm_audit:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        alarmAuditService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "告警审核-批量删除")
    @ApiOperation(value = "告警审核-批量删除", notes = "告警审核-批量删除")
    //@RequiresPermissions("org.jeecg.modules:alarm_audit:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.alarmAuditService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "告警审核-通过id查询")
    @ApiOperation(value = "告警审核-通过id查询", notes = "告警审核-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<AlarmAudit> queryById(@RequestParam(name = "id", required = true) String id) {
        AlarmAudit alarmAudit = alarmAuditService.getById(id);
        if (alarmAudit == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(alarmAudit);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param alarmAudit
     */
    //@RequiresPermissions("org.jeecg.modules:alarm_audit:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, AlarmAudit alarmAudit) {
        return super.exportXls(request, alarmAudit, AlarmAudit.class, "告警审核");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    //@RequiresPermissions("alarm_audit:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, AlarmAudit.class);
    }

}
