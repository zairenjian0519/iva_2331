/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.baseImage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.baseImage.entity.BasicImageDo;
import org.jeecg.modules.baseImage.service.IBasicImageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author： DecoderX
 * @date： 2025/8/12
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/basicImage")
public class BasicImageController {

    private final IBasicImageService basicImageService;

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        return basicImageService.upload(file);
    }

    @GetMapping("/page")
    public Result<?> page(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "30") Integer pageSize,
                          BasicImageDo basicImageDo) {
        return Result.OK(basicImageService.pageList(new Page<>(pageNo, pageSize), basicImageDo));
    }

    @GetMapping("/delete")
    public Result<?> delete(@RequestParam("id") String id) {
        return basicImageService.deleteById(id);
    }

    @GetMapping("/list")
    public Result<?> list() {
        return Result.OK(basicImageService.getList());
    }
}
