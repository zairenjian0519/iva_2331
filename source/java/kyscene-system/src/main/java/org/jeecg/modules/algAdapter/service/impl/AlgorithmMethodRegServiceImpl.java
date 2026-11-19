/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.algAdapter.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.algAdapter.entity.AlgorithmClsReg;
import org.jeecg.modules.algAdapter.entity.AlgorithmMethodReg;
import org.jeecg.modules.algAdapter.mapper.AlgorithmMethodRegMapper;
import org.jeecg.modules.algAdapter.service.IAlgorithmClsRegService;
import org.jeecg.modules.algAdapter.service.IAlgorithmMethodRegService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.system.util.CmdUti;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author： DecoderX
 * @date： 2025/7/18
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AlgorithmMethodRegServiceImpl extends ServiceImpl<AlgorithmMethodRegMapper, AlgorithmMethodReg> implements IAlgorithmMethodRegService {

    private final IKyCategoryService kyCategoryService;

    private final IAlgorithmClsRegService algorithmClsRegService;

    @Override
    public IPage<AlgorithmMethodReg> getPage(Integer pageNo, Integer pageSize) {
        IPage<AlgorithmMethodReg> page = new Page<>(pageNo, pageSize);
        return this.baseMapper.selectPageRows(page);
    }

    @Override
    public List<KyCategory> getAlgorithmList() {
        return kyCategoryService.list(Wrappers.<KyCategory>lambdaQuery()
                .eq(KyCategory::getCategory, "0")
                .eq(KyCategory::getHasChild, "0"));
    }

    @Override
    public List<AlgorithmClsReg> getClassNameList() {
        return algorithmClsRegService.list(Wrappers.lambdaQuery(AlgorithmClsReg.class).eq(AlgorithmClsReg::getStatus, 1));
    }

    @Override
    public Result<?> saveData(AlgorithmMethodReg algorithmMethodReg) {
        long count = this.count(Wrappers.lambdaQuery(AlgorithmMethodReg.class)
                .eq(AlgorithmMethodReg::getClassId, algorithmMethodReg.getClassId())
                .eq(AlgorithmMethodReg::getKyCategoryId, algorithmMethodReg.getKyCategoryId()));
        if (count > 0) return Result.error("已存在同名算法");
        return this.save(algorithmMethodReg) ? Result.OK("保存成功") : Result.error("保存失败");
    }

    @Override
    public Result<?> updateData(AlgorithmMethodReg algorithmMethodReg) {
        long count = this.count(Wrappers.lambdaQuery(AlgorithmMethodReg.class)
                .eq(AlgorithmMethodReg::getKyCategoryId, algorithmMethodReg.getKyCategoryId())
                .eq(AlgorithmMethodReg::getClassId, algorithmMethodReg.getClassId())
                .ne(AlgorithmMethodReg::getId, algorithmMethodReg.getId()));
        if (count > 0) return Result.error("已存在同名算法");
        return this.updateById(algorithmMethodReg) ? Result.OK("修改成功") : Result.error("修改失败");
    }

    @Override
    public void initAiEngine() {
        List<Map<String, String>> mapList = this.baseMapper.queryInitAiParams();
        if (ObjectUtil.isEmpty(mapList)) return;
        List<Map<String, Object>> maps = mapList.stream().map(map -> {
            Map<String, Object> tempMap = new HashMap<>();
            tempMap.put("className", map.get("className"));
            String algorithms = map.get("algorithms");
            if (StrUtil.isNotBlank(algorithms)) {
                tempMap.put("algorithms", StrUtil.split(algorithms, ","));
            } else {
                tempMap.put("algorithms", new ArrayList<>());
            }
            return tempMap;
        }).toList();
        Integer maxId = this.baseMapper.queryMaxId();

        JSONObject result = new JSONObject();
        result.put("maxId", maxId);
        result.put("data", maps);

        // 存入redis
        RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
        redisUtil.set("algorithm_adapter", result);

        log.info("成功初始化参数到redis: {}", result.toJSONString());

        // 重启算法
        CmdUti.restartAiEngine();
    }
}
