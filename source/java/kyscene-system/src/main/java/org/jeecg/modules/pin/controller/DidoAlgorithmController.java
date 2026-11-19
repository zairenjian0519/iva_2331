package org.jeecg.modules.pin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.pin.entity.DidoAlgorithmDO;
import org.jeecg.modules.pin.entity.PageVo;
import org.jeecg.modules.pin.service.DidoAlgorithmService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/didoAlgorithm")
public class DidoAlgorithmController {

    private final DidoAlgorithmService didoAlgorithmService;

    /**
     * 获取算法列表
     */
    @GetMapping("/getAlgorithmList")
    public Result<?> getAlgorithmList() {
        List<KyCategory> algorithmList = didoAlgorithmService.getAlgorithmList();
        return Result.OK(algorithmList);
    }

    /**
     * 获取表格数据
     */
    @PostMapping("/getTableData")
    public Result<IPage<PageVo>> getPageData(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                             @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(didoAlgorithmService.getTableData(pageNo, pageSize));
    }

    @PostMapping("/addData")
    public Result<?> saveData(@RequestBody DidoAlgorithmDO didoAlgorithmDO) {
        didoAlgorithmService.saveData(didoAlgorithmDO);
        didoAlgorithmService.syncCache();
        return Result.OK();
    }

    @PostMapping("/updateDate")
    public Result<?> updateDate(@RequestBody DidoAlgorithmDO didoAlgorithmDO) {
        didoAlgorithmService.updateById(didoAlgorithmDO);
        didoAlgorithmService.syncCache();
        return Result.OK();
    }

    @GetMapping("/deleteRow")
    public Result<?> deleteRow(@RequestParam String id) {
        didoAlgorithmService.removeById(id);
        didoAlgorithmService.syncCache();
        return Result.OK();
    }
}
