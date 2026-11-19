/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.algAdapter.controller;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.algAdapter.entity.AlgorithmMethodReg;
import org.jeecg.modules.algAdapter.service.IAlgorithmMethodRegService;
import org.springframework.web.bind.annotation.*;

/**
 * @author： DecoderX
 * @date： 2025/7/18
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/algAdapter/algorithmMethodReg")
public class AlgorithmMethodRegController {

    private final IAlgorithmMethodRegService algorithmMethodRegService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.ok(algorithmMethodRegService.getPage(pageNo, pageSize));
    }

    @GetMapping("/getAlgorithmList")
    public Result<?> getAlgorithmList() {
        return Result.ok(algorithmMethodRegService.getAlgorithmList());
    }

    @GetMapping("/getClassNameList")
    public Result<?> getClassNameList() {
        return Result.ok(algorithmMethodRegService.getClassNameList());
    }

    @PostMapping("/add")
    public Result<?> registerAlgorithm(@RequestBody AlgorithmMethodReg algorithmMethodReg) {
        return algorithmMethodRegService.saveData(algorithmMethodReg);
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody AlgorithmMethodReg algorithmMethodReg) {
        return algorithmMethodRegService.updateData(algorithmMethodReg);
    }

    @GetMapping("/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        return algorithmMethodRegService.removeById(id) ? Result.OK("删除成功") : Result.error("删除失败");
    }

    @GetMapping("/initAiEngine")
    public Result<?> initAiEngine() {
        algorithmMethodRegService.initAiEngine();
        return Result.OK();
    }
}
