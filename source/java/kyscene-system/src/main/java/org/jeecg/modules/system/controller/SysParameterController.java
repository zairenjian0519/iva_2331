package org.jeecg.modules.system.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.service.ISysParameterService;
import org.jeecg.modules.system.util.Constant;
import org.jeecg.modules.thirdApi.AlarmPushConf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: 系统参数表
 * @Author: jeecg-boot
 * @Date: 2023-03-03
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "系统参数表")
@RestController
@RequestMapping("/system/sysParameter")
public class SysParameterController extends JeecgController<SysParameter, ISysParameterService> {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ISysParameterService sysParameterService;

    @Autowired
    private AlarmPushConf alarmPushConf;

    /**
     * 分页列表查询
     *
     * @param sysParameter
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "系统参数表-分页列表查询")
    @ApiOperation(value = "系统参数表-分页列表查询", notes = "系统参数表-分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(SysParameter sysParameter,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<SysParameter> queryWrapper = QueryGenerator.initQueryWrapper(sysParameter, req.getParameterMap());
        Page<SysParameter> page = new Page<SysParameter>(pageNo, pageSize);
        IPage<SysParameter> pageList = sysParameterService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param sysParameter
     * @return
     */
    @AutoLog(value = "系统参数表-添加")
    @ApiOperation(value = "系统参数表-添加", notes = "系统参数表-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody SysParameter sysParameter) {
        sysParameterService.save(sysParameter);
        cacheSysParameter();
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param sysParameter
     * @return
     */
    @AutoLog(value = "系统参数表-编辑")
    @ApiOperation(value = "系统参数表-编辑", notes = "系统参数表-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody SysParameter sysParameter) {
        sysParameterService.updateById(sysParameter);
        cacheSysParameter();
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "系统参数表-通过id删除")
    @ApiOperation(value = "系统参数表-通过id删除", notes = "系统参数表-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        sysParameterService.removeById(id);
        cacheSysParameter();
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "系统参数表-批量删除")
    @ApiOperation(value = "系统参数表-批量删除", notes = "系统参数表-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.sysParameterService.removeByIds(Arrays.asList(ids.split(",")));
        cacheSysParameter();
        return Result.OK("批量删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "系统参数表-通过id查询")
    @ApiOperation(value = "系统参数表-通过id查询", notes = "系统参数表-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        SysParameter sysParameter = sysParameterService.getById(id);
        return Result.OK(sysParameter);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param sysParameter
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, SysParameter sysParameter) {
        return super.exportXls(request, sysParameter, SysParameter.class, "系统参数表");
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
        Result<?> result = super.importExcel(request, response, SysParameter.class);
        if (result.isSuccess()) cacheSysParameter();
        return result;
    }

    // 缓存系统参数, 如果没有查询到数据，就删除key
    private void cacheSysParameter() {
        List<SysParameter> list = sysParameterService.list();
        if (ObjectUtil.isNotEmpty(list)) {
            redisUtil.set(Constant.PARAMETER_CACHE_KEY, list);
            return;
        }
        redisUtil.del(Constant.PARAMETER_CACHE_KEY);
    }

    /**
     * 通过code获取参数
     * @param code 参数编码
     * @return SysParameter 对象
     */
    @GetMapping("/getByCode")
    public Result<SysParameter> getByCode(@RequestParam("paramCode") String code) {
        SysParameter sysParameter = sysParameterService.getOne(Wrappers.<SysParameter>lambdaQuery().eq(SysParameter::getParamCode, code));
        return Result.OK(sysParameter);
    }

    /**
     * 添加或更新
     * @param sysParameter SysParameter
     * @return Result<?>
     */
    @PostMapping("/addOrUpdate")
    public Result<?> addOrUpdate(@RequestBody SysParameter sysParameter) {
        // 根据 paramCode 来判断是添加还是更新
        SysParameter parameter = sysParameterService.getOne(Wrappers.lambdaQuery(SysParameter.class)
                .eq(SysParameter::getParamCode, sysParameter.getParamCode()), false);
        if (parameter != null) {
            BeanUtil.copyProperties(sysParameter, parameter, CopyOptions.create().ignoreNullValue());
            sysParameterService.updateById(parameter);
        } else {
            sysParameterService.save(sysParameter);
        }
        cacheSysParameter();
        return Result.OK();
    }
}
