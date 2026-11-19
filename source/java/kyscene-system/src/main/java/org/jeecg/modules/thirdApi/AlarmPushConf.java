package org.jeecg.modules.thirdApi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.dataPush.Constant;
import org.jeecg.modules.dataPush.DataPushVo;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.service.ISysParameterService;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.jeecg.modules.system.util.Constant.PARAMETER_CACHE_KEY;

@Slf4j
@Component
public class AlarmPushConf implements CommandLineRunner {

    private ISysParameterService sysParameterService;

    private RedisUtil redisUtil;

    @Autowired
    public void setSysParameterService(ISysParameterService sysParameterService) {
        this.sysParameterService = sysParameterService;
    }

    @Autowired
    public void setRedisUtil(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Setter
    private DataPushVo dataPushVo;

    public DataPushVo getDataPushVo() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter(Constant.DATA_PUSH_CODE);
        if (null == sysParameter) return null;
        String paramVal = sysParameter.getParamVal();
        if (StrUtil.isBlank(paramVal)) return null;
        return JSONObject.parseObject(paramVal, DataPushVo.class);
    }

    public boolean isEnable() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter(Constant.DATA_PUSH_CODE);
        if (null == sysParameter) return false;
        String paramVal = sysParameter.getParamVal();
        if (StrUtil.isBlank(paramVal)) return false;
        JSONObject jsonObject = JSONObject.parseObject(paramVal);
        Boolean enableObj = jsonObject.getBoolean("enable");
        if (enableObj != null) return enableObj;
        return false;
    }

    public String getPushUrl() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter(Constant.DATA_PUSH_CODE);
        if (null == sysParameter) return null;
        String paramVal = sysParameter.getParamVal();
        if (StrUtil.isBlank(paramVal)) return null;
        JSONObject jsonObject = JSONObject.parseObject(paramVal);
        String url = jsonObject.getString("pushUrl");
        if (StrUtil.isNotBlank(url)) return url;
        return null;
    }

    @Override
    public void run(String... args) throws Exception {
        // 系统启动自动覆盖系统参数
        List<SysParameter> parameterList = sysParameterService.list();
        if (ObjectUtil.isEmpty(parameterList)) {
            redisUtil.del(PARAMETER_CACHE_KEY);
            log.info("告警推送启用：{}", Boolean.FALSE);
            return;
        }
        // redisUtil.set(PARAMETER_CACHE_KEY, parameterList);
        SysParameterUtil.initCache(parameterList, redisUtil);
        parameterList.stream()
                .filter(sysParameter -> sysParameter.getParamCode().equals(Constant.DATA_PUSH_CODE))
                .findFirst()
                .ifPresent(sysParameter -> {
                    String paramVal = sysParameter.getParamVal();
                    DataPushVo dataPushVo = JSONObject.parseObject(paramVal, DataPushVo.class);
                    setDataPushVo(dataPushVo);
                });

        log.info("告警推送启用：{} 推送地址：{}", isEnable(), getPushUrl());
    }

}
