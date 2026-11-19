/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.alarmFace;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * @author： DecoderX
 * @date： 2025/4/8
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/alarmFace")
public class AlarmFaceController  {

    private final AlarmFaceService alarmFaceService;
    /**
     * 获取告警人脸列表
     */
    @GetMapping("/page")
    public Result<?> getPage(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                             @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                             AlarmFaceParams alarmFaceParams) {
        return Result.OK(alarmFaceService.getPageData(pageNo, pageSize, alarmFaceParams));
    }

    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        alarmFaceService.removeById(id);
        return Result.OK("删除成功!");
    }

    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        alarmFaceService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

    @RequestMapping(value = "/exportXls")
    public Result<?> exportXls(HttpServletRequest request, AlarmFaceDo alarmFaceDo) {
        return Result.OK("导出功能完善中...");
    }
}
