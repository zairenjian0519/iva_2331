/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.algAdapter.controller;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.algAdapter.entity.AlgorithmClsReg;
import org.jeecg.modules.algAdapter.service.IAlgorithmClsRegService;
import org.springframework.web.bind.annotation.*;

/**
 * @author： DecoderX
 * @date： 2025/7/18
 */
@RequiredArgsConstructor
@RequestMapping("/algAdapter/algorithmClsReg")
@RestController
public class AlgorithmClsRegController {

    private final IAlgorithmClsRegService algorithmClsRegService;

    @RequestMapping("/page")
    public Result<?> page(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(algorithmClsRegService.getPage(pageNo, pageSize));
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody AlgorithmClsReg algorithmClsReg) {
        return algorithmClsRegService.addData(algorithmClsReg);
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody AlgorithmClsReg algorithmClsReg) {
        return algorithmClsRegService.updateData(algorithmClsReg);
    }

    @GetMapping("/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        return algorithmClsRegService.deleteRow(id);
    }

}
