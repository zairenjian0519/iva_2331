/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial.job;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.iot.serial.Serial485Service;
import org.jeecg.modules.iot.serial.Serial485Setting;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.boot.CommandLineRunner;

import java.io.File;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@Slf4j
// @Component
public class Serial485CommandLiner implements CommandLineRunner {

    private Serial485Service serial485Service;

    // @Autowired
    private void setSerial485Service(Serial485Service serial485Service) {
        this.serial485Service = serial485Service;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("准备初始化串口配置...");
        // 启动的时候清理串口锁文件
        File lockDir = new File("/var/lock");
        if (lockDir.exists()) {
            for (File f : lockDir.listFiles()) {
                if (f.getName().startsWith("LCK..")) {
                    f.delete();
                }
            }
        }
        SysParameter parameter = SysParameterUtil.getSysParameter("TH_SENSOR");
        if (parameter == null || parameter.getParamVal() == null) {
            log.info("未配置串口参数 {}", "TH_SENSOR");
            return;
        }
        try {
            String paramVal = parameter.getParamVal();
            Serial485Setting serial485Setting = JSON.toJavaObject(JSON.parseObject(paramVal), Serial485Setting.class);
            serial485Service.openCommPort(serial485Setting);
        } catch (Exception e) {
            log.error("初始化串口配置失败 {}", e.getMessage(), e);
        }

    }
}
