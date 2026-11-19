/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.algAdapter.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.algAdapter.entity.AlgorithmClsReg;
import org.jeecg.modules.algAdapter.entity.AlgorithmMethodReg;
import org.jeecg.modules.algAdapter.mapper.AlgorithmClsRegMapper;
import org.jeecg.modules.algAdapter.service.IAlgorithmClsRegService;
import org.jeecg.modules.algAdapter.service.IAlgorithmMethodRegService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author： DecoderX
 * @date： 2025/7/18
 */
@Service
public class AlgorithmClsRegServiceImpl extends ServiceImpl<AlgorithmClsRegMapper, AlgorithmClsReg> implements IAlgorithmClsRegService {

    @Override
    public IPage<AlgorithmClsReg> getPage(Integer pageNo, Integer pageSize) {
        IPage<AlgorithmClsReg> page = new Page<>(pageNo, pageSize);
        return this.page(page, Wrappers.lambdaQuery(AlgorithmClsReg.class).orderByDesc(AlgorithmClsReg::getCreateTime));
    }

    @Override
    public Result<?> addData(AlgorithmClsReg algorithmClsReg) {
        long count = this.count(Wrappers.lambdaQuery(AlgorithmClsReg.class)
                .eq(AlgorithmClsReg::getClassName, algorithmClsReg.getClassName()));
        if (count > 0) {
            return Result.error("已存在同名类");
        }
        boolean save = this.save(algorithmClsReg);
        if (save) {
            return Result.OK("添加成功");
        }
        return Result.error("添加失败");
    }

    @Override
    public Result<?> updateData(AlgorithmClsReg algorithmClsReg) {
        long count = this.count(Wrappers.lambdaQuery(AlgorithmClsReg.class)
                .eq(AlgorithmClsReg::getClassName, algorithmClsReg.getClassName())
                .ne(AlgorithmClsReg::getId, algorithmClsReg.getId()));
        if (count > 0) {
            return Result.error("已存在同名类");
        }
        boolean update = this.updateById(algorithmClsReg);
        if (update) {
            return Result.OK("修改成功");
        }
        return Result.error("修改失败");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<?> deleteRow(String id) {
        IAlgorithmMethodRegService algorithmMethodRegService = SpringContextUtils.getBean(IAlgorithmMethodRegService.class);
        algorithmMethodRegService.remove(Wrappers.lambdaQuery(AlgorithmMethodReg.class)
                .eq(AlgorithmMethodReg::getClassId, id));
        this.removeById(id);
        return Result.OK("删除成功");
    }
}
