/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.algAdapter.controller;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.algAdapter.service.IMainFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author： DecoderX
 * @date： 2025/7/15
 */
@RestController
@RequestMapping("/mainFile")
public class MainFileController {

    @Autowired
    private IMainFileService mainFileService;

    @PostMapping("/upload/{type}")
    public Result<?> upload(@PathVariable String type,  MultipartFile file) {
        return mainFileService.upload(type, file);
    }

    @GetMapping("/tableData")
    public Result<?> tableData(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(mainFileService.getPageData(pageNo, pageSize));
    }

    @GetMapping("/deleteById")
    public Result<?> deleteById(@RequestParam(name = "id") String id) {
        return mainFileService.deleteFileById(id);
    }

    @GetMapping("/applyFileById")
    public Result<?> applyFileById(@RequestParam(name = "id") String id) {
        return mainFileService.applyFileById(id);
    }
}
