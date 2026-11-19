package org.jeecg.modules.face.controller;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecg.modules.face.service.FaceGroupService;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/faceGroup")
public class FaceGroupController {

    private final FaceGroupService faceGroupService;

    @GetMapping("/list")
    public Result<?> getPage() {
        return Result.OK(faceGroupService.getList());
    }

    @GetMapping("/get")
    public Result<?> getFaceGroup(String id) {
        return Result.OK(faceGroupService.getById(id));
    }

    @PostMapping("/add")
    public Result<?> addFaceGroup(@RequestBody FaceGroupDo faceGroupDo) {
        faceGroupService.addFaceGroup(faceGroupDo);
        return Result.OK("添加成功");
    }

    @PostMapping("/update")
    public Result<?> updateFaceGroup(@RequestBody FaceGroupDo faceGroupDo) {
        faceGroupService.updateFaceGroup(faceGroupDo);
        return Result.OK("修改成功");
    }

    @GetMapping("/delete")
    public Result<?> deleteFaceGroup(String id) {
        return faceGroupService.deleteFaceGroup(id);
    }

    /**
     * 初始化人脸库，本地初始化npy文件
     */
    @GetMapping("/initFaceGroup")
    public Result<?> initFaceGroup(String faceGroupId) {
        faceGroupService.initFaceGroup(faceGroupId);
        return Result.OK("初始化人脸库请求成功");
    }

    /**
     * 根据项目编号去初始化人脸组
     */
    @GetMapping("/initGroup")
    public Result<?> initGroup() {
        return faceGroupService.initGroup();
    }
}
