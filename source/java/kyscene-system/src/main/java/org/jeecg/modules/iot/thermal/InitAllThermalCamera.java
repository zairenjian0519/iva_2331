/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.thermal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author： DecoderX
 * @date： 2025/11/19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitAllThermalCamera implements CommandLineRunner {

    private final ThermalService thermalService;

    @Override
    public void run(String... args) throws Exception {
        thermalService.initAllCameraLogin();
    }
}
