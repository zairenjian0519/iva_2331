package org.jeecg.modules.face.controller;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.face.entity.FaceDetailDo;
import org.jeecg.modules.face.entity.FaceQueryParams;
import org.jeecg.modules.face.service.FaceDetailService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/faceDetail")
public class FaceDetailController {

    private final FaceDetailService faceDetailService;

    @PostMapping("/add")
    public Result<?> addFaceDetail(@ModelAttribute FaceDetailDo faceDetailDo, @RequestParam("file") MultipartFile file) {
        faceDetailService.addFaceDetail(faceDetailDo, file);
        return Result.OK("操作成功");
    }

    @PostMapping("/update")
    public Result<?> updateFaceDetail(@ModelAttribute FaceDetailDo faceDetailDo, @RequestParam(value = "file", required = false) MultipartFile file) {
        faceDetailService.updateFaceDetail(faceDetailDo, file);
        return Result.OK("操作成功");
    }

    @GetMapping("/delete")
    public Result<?> deleteFaceDetail(String id) {
        faceDetailService.deleteFaceDetail(id);
        return Result.OK("操作成功");
    }

    @GetMapping("/batchDelete")
    public Result<?> batchDeleteFaceDetail(@RequestParam("ids") String ids) {
        faceDetailService.batchDeleteFaceDetail(ids);
        return Result.OK("操作成功");
    }

    @GetMapping("/deleteImg")
    public Result<?> deleteImg(String id) {
        faceDetailService.deleteImg(id);
        return Result.OK("操作成功");
    }

    // 需要分页
    @GetMapping("/getByGroupId")
    public Result<?> getByGroupId(FaceQueryParams faceQueryParams,
                                  @RequestParam(defaultValue = "1") Integer pageNo,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.OK(faceDetailService.getByGroupId(faceQueryParams, pageNo, pageSize));
    }

    @GetMapping("/export")
    public void exportXls(Integer startNum, Integer exportNum, HttpServletResponse response) {
        faceDetailService.exportXls(startNum, exportNum, response);
    }

    @PostMapping("/importExcel")
    public Result<?> importExcel(@RequestParam("groupId") String groupId, @RequestParam("file") MultipartFile file) {
        List<String> errList = faceDetailService.importExcel(groupId, file);
        if (!errList.isEmpty()) return Result.OK(errList);
        return Result.OK("操作成功");
    }

    @GetMapping("/importSingleFace")
    public Result<?> importSingleFace(String alarmDetailId,  FaceDetailDo faceDetailDo) {
        faceDetailService.importSingleFace(alarmDetailId, faceDetailDo);
        return Result.OK("操作成功");
    }

    @GetMapping("/batchImportFace")
    public Result<?> batchImportFace(String faceGroup, String ids) {
        faceDetailService.batchImportFace(faceGroup, ids);
        return Result.OK("操作成功");
    }

    @GetMapping("/syncBaiduYun")
    public Result<?> syncBaiduYun(@RequestParam String groupKey) {
        faceDetailService.syncBaiduYun(groupKey);
        return Result.OK();
    }
}
