package org.jeecg.modules.pin.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.pin.service.DidoAlgorithmService;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化dido配置参数
 */
@Slf4j
@Component
public class InitDidoCache implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        SysParameter sysParameter = SysParameterUtil.getSysParameter("GPIO");
        if (sysParameter == null) return;
        String paramVal = sysParameter.getParamVal();
        if (StrUtil.isBlank(paramVal)) return;
        JSONObject jsonObject = JSONObject.parseObject(paramVal);
        Boolean enable = jsonObject.getBoolean("enable");
        if (enable == null || !enable) return;
        DidoAlgorithmService didoAlgorithmService = SpringContextUtils.getBean(DidoAlgorithmService.class);
        didoAlgorithmService.syncCache();
        log.info("DIDO缓存已刷新");
    }
}
