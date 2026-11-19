package org.jeecg.modules.system.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.model.CommandResult;
import org.jeecg.modules.system.service.ISysParameterService;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 系统参数工具类
 */
@Slf4j
public class SysParameterUtil {

    public static void initCache(List<SysParameter> list, RedisUtil redisUtil) {
        redisUtil.set(Constant.PARAMETER_CACHE_KEY, list);
        // 判断是否存在百度人脸的设置
        for (SysParameter sysParameter : list) {
            if ("ServerFace".equals(sysParameter.getParamCode())) {
                redisUtil.set(Constant.ENABLE_BAIDU_FACE_API, sysParameter.getParamVal());
                break;
            }
        }
    }

    public static SysParameter getSysParameter(String paramCode) {
        RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
        Object o = redisUtil.get(Constant.PARAMETER_CACHE_KEY);
        if (ObjectUtil.isEmpty(o)) return null;
        List<SysParameter> list = (List<SysParameter>) o;
        Optional<SysParameter> optionalSysParameter = list.stream()
                .filter(sysParameter -> sysParameter.getParamCode().equals(paramCode))
                .findFirst();
        return optionalSysParameter.orElse(null);
    }

    /**
     * 获取设备真实SN
     */
    public static String getDeviceRealSn() {
        // 查看设备分类
        String deviceCategory = getDeviceCategory();
        if (StrUtil.isBlank(deviceCategory)) {
            log.error("设备分类为空");
            return null;
        }
        String[] cmd = null;
        if ("N150".equals(deviceCategory)) {
            cmd = new String[]{"sudo", "/usr/local/lynxi/bsp/tools/lyn_cmd", "misc", "sn_get"};
        } else if ("N2110".equals(deviceCategory)) {
            cmd = new String[]{"mcutools", "-s"};
        } else if ("N200".equals(deviceCategory)) {
            cmd = new String[]{"sudo", "/home/super/web/get_sno"};
        }
        if (cmd == null) {
            log.error("设备分类不支持");
            return null;
        }
        try {
            CommandResult commandResult = CmdUti.executeCmd2(cmd);
            log.info("获取设备sn码结果：{}, 退出码：{}", commandResult.getOutput(), commandResult.getExitCode());
            Result<String> result = getDeviceSnCode(deviceCategory, commandResult.getOutput());
            if (result != null && StrUtil.isNotBlank(result.getResult())) {
                return result.getResult();
            }
        } catch (Exception e) {
            log.error("获取设备sn码失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取设备的SN
     */
    public static String getDeviceSn() {
        // 如果已经配置了sn码，保持不变
        SysParameter sysParameter = getSysParameter("DeviceSn");
        if (sysParameter != null && StrUtil.isNotBlank(sysParameter.getParamVal())) {
            String deviceSn = sysParameter.getParamVal();
            return deviceSn.trim().replaceAll("[\\r\\n]", "");
        }

        // 查看设备分类
        String[] cmd = new String[]{"/home/super/web/get_sno2.sh"};
        try {
            CommandResult commandResult = CmdUti.executeCmd2(cmd);
            String sn = commandResult.getOutput();
            log.info("获取设备sn码结果：{}, 退出码：{}", sn, commandResult.getExitCode());
            if (sn != null && StrUtil.isNotBlank(sn)) {
                sn = sn.trim().replaceAll("[\\r\\n]", "");
                sysParameter = new SysParameter();
                sysParameter.setParamCode("DeviceSn");
                sysParameter.setParamVal(sn);
                sysParameter.setDescription("设备SN码");
                sysParameter.setParamName("设备SN");
                addOrUpdateSysParameter(sysParameter);
                return sn;
            }
        } catch (Exception e) {
            log.error("获取设备sn码失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据不同设备获取解析sn
     */
    private static Result<String> getDeviceSnCode(String deviceCategory, String executeResult) {
        if (StrUtil.isBlank(executeResult)) {
            return Result.error("获取设备sn码失败，cmd执行结果为空");
        }
        if ("N150".equals(deviceCategory) || "N200".equals(deviceCategory)) {
            if (executeResult.contains("sn")) {
                String[] sn = executeResult.split("=");
                if (sn.length > 1) {
                    String snCode = sn[1].trim();
                    return Result.OK(snCode);
                }
            }
        } else if ("N2110".equals(deviceCategory)) {
            // SN: EMBLA24109A00163
            if (executeResult.contains("SN")) {
                String[] sn = executeResult.split(":");
                if (sn.length > 1) {
                    String snCode = sn[1].trim();
                    return Result.OK(snCode);
                }
            }
        } else {
            return Result.error("获取sn码失败，设备类别不支持");
        }
        return Result.error("获取设备sn码失败，执行结果为 " + executeResult);
    }

    /**
     * 获取设备类别
     * N50, N150 / N200 / N300 / N400
     */
    public static String getDeviceCategory() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter("DeviceCategory");
        if (sysParameter == null) return null;
        return sysParameter.getParamVal();
    }

    /**
     * 缓存系统参数
     */
    public static void cacheSystemParams() {
        ISysParameterService sysParameterService = SpringContextUtils.getBean(ISysParameterService.class);
        List<SysParameter> sysParameters = sysParameterService.list();
        RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
        if (ObjectUtil.isNotEmpty(sysParameters)) {
            redisUtil.set(Constant.PARAMETER_CACHE_KEY, sysParameters);
            return;
        }
        redisUtil.del(Constant.PARAMETER_CACHE_KEY);
    }

    /**
     * 获取业务ip，port
     */
    public static JSONObject getBusinessIpInf() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter("StreamBusinessIpConfig");
        if (sysParameter != null && StrUtil.isNotBlank(sysParameter.getParamVal())) {
            JSONObject businessIpConfig = JSON.parseObject(sysParameter.getParamVal());
            boolean enable = businessIpConfig.getBooleanValue("enable");
            if (enable) {
                String hostIp = businessIpConfig.getString("bizIp");
                Integer hostPort = businessIpConfig.getInteger("bizPort");
                if (ObjectUtil.isAllNotEmpty(hostIp, hostPort)) {
                    JSONObject bizParam = new JSONObject();
                    bizParam.put("bizIp", hostIp);
                    bizParam.put("bizPort", hostPort);
                    return bizParam;
                }
            }
        }
        return null;
    }

    /**
     * 添加系统参数
     */
    public static void addSystemParams(SysParameter sysParameter) {
        ISysParameterService sysParameterService = SpringContextUtils.getBean(ISysParameterService.class);

        String paramCode = sysParameter.getParamCode();
        SysParameter parameter = sysParameterService.getOne(Wrappers.lambdaQuery(SysParameter.class)
                .eq(SysParameter::getParamCode, paramCode));
        if (parameter != null) {
            BeanUtil.copyProperties(sysParameter, parameter, CopyOptions.create().ignoreNullValue());
            sysParameterService.updateById(parameter);
        } else {
            sysParameterService.save(sysParameter);
        }
        cacheSystemParams();
    }

    /**
     * 新增或者更新系统参数
     *
     * @param sysParameter SysParameter
     */
    public static void addOrUpdateSysParameter(SysParameter sysParameter) {
        ISysParameterService sysParameterService = SpringContextUtils.getBean(ISysParameterService.class);
        // 根据paramCode检查是否存在
        SysParameter fromDb = sysParameterService.getOne(new QueryWrapper<SysParameter>().eq("param_code", sysParameter.getParamCode()));
        if (fromDb == null) {
            sysParameter.setCreateTime(new Date());
            sysParameterService.save(sysParameter);
            // 缓存系统参数
            cacheSystemParams();
            return;
        }
        if (StrUtil.isNotBlank(sysParameter.getParamVal()) && !sysParameter.getParamVal().equals(fromDb.getParamVal())) {
            BeanUtil.copyProperties(sysParameter, fromDb, CopyOptions.create().ignoreNullValue());
            sysParameterService.updateById(fromDb);
            // 缓存系统参数
            cacheSystemParams();
        }
    }

    /**
     * 获取项目编号
     */
    public static String getProjectCode() {
        SysParameter sysParameter = getSysParameter("ProjectCode");
        if (sysParameter != null) return sysParameter.getParamVal();
        return null;
    }

    /**
     * 是否启用百度API
     */
    public static boolean isBaiduFaceEnabled() {
        SysParameter sysParameter = getSysParameter("ServerFace");
        if (sysParameter == null) return false;
        String paramVal = sysParameter.getParamVal();
        if (StrUtil.isBlank(paramVal)) return false;
        JSONObject jsonObject = JSONObject.parseObject(paramVal);
        return jsonObject.getBoolean("enable");
    }

    /**
     * 获取设置的百度人脸score
     */
    public static int getBaiduFaceScore() {
        SysParameter sysParameter = getSysParameter("ServerFace");
        if (sysParameter == null) return 0;
        String paramVal = sysParameter.getParamVal();
        if (StrUtil.isBlank(paramVal)) return 0;
        JSONObject jsonObject = JSONObject.parseObject(paramVal);
        return jsonObject.getIntValue("score");
    }
}
