/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.serial;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.api.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author： DecoderX
 * @date： 2025/10/23
 */
@RequestMapping("/thRealData")
@RestController
public class THRealDataController {

    @Autowired
    private THRealDataService thRealDataService;

    @GetMapping("/realData")
    public Result<THRealtimeData> getTHRealData(@RequestParam String id) {
        return Result.OK(thRealDataService.getLastData(id));
    }

    @GetMapping("/page")
    public Result<?> getTHRealDataPage(@RequestParam String id,
                                       @RequestParam(defaultValue = "1") Integer pageNo,
                                       @RequestParam(defaultValue = "30") Integer pageSize) {
        return Result.OK(thRealDataService.listPage(id, new Page<>(pageNo, pageSize)));
    }

    @GetMapping("/delete")
    public Result<?> deleteTHRealData(@RequestParam String id) {
        return Result.OK(thRealDataService.removeById(id));
    }
}
