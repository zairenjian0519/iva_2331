/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.deviceManagement.controller;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.deviceManagement.entity.SoftUpdateDo;
import org.jeecg.modules.deviceManagement.service.ISoftUpdateService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author： DecoderX
 * @date： 2025/5/29
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/deviceManagement/softUpdate")
public class SoftUpdateController {

    private final ISoftUpdateService softUpdateService;

    /**
     * 获取软件更新列表page
     */
    @GetMapping("/page")
    public Result<?> getSoftUpdatePage(SoftUpdateDo softUpdateDo,
                                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(softUpdateService.getUpdatePage(softUpdateDo, pageNo, pageSize));
    }

    /**
     * 上传更新文件
     * @param file MultipartFile
     * @param softUpdateDo SoftUpdateDo
     * @return Result
     */
    @PostMapping("/update")
    public Result<?> updateSoft(MultipartFile file, SoftUpdateDo softUpdateDo) {
        return softUpdateService.uploadUpdateFile(file, softUpdateDo);
    }

    /**
     * 删除上传的软件
     * @param id String
     * @return Result
     */
    @GetMapping("/delete")
    public Result<?> deleteSoft(@RequestParam(name = "id") String id) {
        return softUpdateService.deleteUploadSoft(id);
    }

    /**
     * 发布软件
     * @param id String
     * @return Result
     */
    @GetMapping("/publish")
    public Result<?> publishSoft(@RequestParam(name = "id") String id) {
        return softUpdateService.publishSoft(id);
    }

    /**
     * 撤回软件
     * @param softType String
     * @return Result
     */
    @GetMapping("/withdraw")
    public Result<?> withdrawSoft(@RequestParam(name = "softType") String softType) {
        return softUpdateService.withdrawSoft(softType);
    }

    /**
     * 获取所有软件版本
     * @return Result
     */
    @GetMapping("/version")
    public Result<?> getAllSoftVersion() {
        return softUpdateService.getAllSoftVersion();
    }
}
