package org.jeecg.modules.pin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.pin.entity.DidoAlgorithmDO;
import org.jeecg.modules.pin.entity.PageVo;
import org.jeecg.modules.pin.entity.SerialPinDO;
import org.jeecg.modules.pin.mapper.DidoAlgorithmMapper;
import org.jeecg.modules.pin.service.DidoAlgorithmService;
import org.jeecg.modules.pin.service.SerialPinService;
import org.jeecg.modules.pin.utils.DidoTimerTask;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DidoAlgorithmServiceImpl extends ServiceImpl<DidoAlgorithmMapper, DidoAlgorithmDO> implements DidoAlgorithmService {

    private final Timer timer = new Timer();

    @Override
    public List<KyCategory> getAlgorithmList() {
        LambdaQueryWrapper<KyCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KyCategory::getCategory, 0);
        queryWrapper.eq(KyCategory::getHasChild, 0);
        IKyCategoryService kyCategoryService = SpringContextUtils.getBean(IKyCategoryService.class);
        return kyCategoryService.list(queryWrapper);
    }

    @Override
    public IPage<PageVo> getTableData(Integer pageNo, Integer pageSize) {
        // 创建分页对象
        Page<DidoAlgorithmDO> page = new Page<>(pageNo, pageSize);

        // 查询分页数据
        Page<DidoAlgorithmDO> paged = this.page(page, Wrappers.lambdaQuery(DidoAlgorithmDO.class));
        List<DidoAlgorithmDO> records = paged.getRecords();

        if (records.isEmpty()) return null;

        // 提取所有的 serialPinId
        List<String> pidIds = records.stream()
                .map(DidoAlgorithmDO::getSerialPinId)
                .collect(Collectors.toList());

        if (pidIds.isEmpty()) throw new JeecgBootException("没有查询到端点配置信息");

        // 获取 SerialPinService，并根据 pidIds 获取 SerialPinDO 列表
        SerialPinService serialPinService = SpringContextUtils.getBean(SerialPinService.class);
        List<SerialPinDO> serialPins = serialPinService.listByIds(pidIds);

        // 将 serialPins 转为 Map，方便快速查找
        Map<String, SerialPinDO> serialPinMap = serialPins.stream()
                .collect(Collectors.toMap(SerialPinDO::getId, Function.identity()));

        // 创建新的 PageVo 列表
        List<PageVo> newRecords = records.stream()
                .map(didoAlgorithmDO -> {
                    String serialPinId = didoAlgorithmDO.getSerialPinId();
                    SerialPinDO serialPinDO = serialPinMap.get(serialPinId);
                    if (serialPinDO != null) {
                        // 创建 PageVo 并设置属性
                        PageVo pageVo = new PageVo();
                        pageVo.setId(didoAlgorithmDO.getId());
                        pageVo.setSerialPinId(serialPinDO.getId());
                        pageVo.setPinName(serialPinDO.getPinName());
                        pageVo.setPinType(serialPinDO.getPinType());
                        pageVo.setPinNumber(serialPinDO.getPinNumber());
                        pageVo.setPinVal(serialPinDO.getPinVal());
                        pageVo.setAlgorithmCode(didoAlgorithmDO.getAlgorithmCode());
                        pageVo.setStatus(didoAlgorithmDO.getStatus());
                        return pageVo;
                    }
                    return null;
                })
                .filter(Objects::nonNull) // 过滤掉为 null 的结果
                .collect(Collectors.toList());

        // 创建新分页对象并设置记录
        Page<PageVo> newPage = new Page<>();
        BeanUtil.copyProperties(paged, newPage, CopyOptions.create().ignoreNullValue().ignoreError().setIgnoreProperties("records"));
        newPage.setRecords(newRecords);

        return newPage;
    }

    @Override
    public void saveData(DidoAlgorithmDO didoAlgorithmDO) {
        DidoAlgorithmDO queryData = this.getOne(Wrappers.lambdaQuery(DidoAlgorithmDO.class)
                .eq(DidoAlgorithmDO::getSerialPinId, didoAlgorithmDO.getSerialPinId())
                .eq(DidoAlgorithmDO::getAlgorithmCode, didoAlgorithmDO.getAlgorithmCode()));
        if (queryData != null) throw new JeecgBootException("请不要在一个端点上重复绑定相同的算法");
        this.save(didoAlgorithmDO);
    }

    @Override
    public boolean enableDido() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter("GPIO");
        if (sysParameter != null) {
            String paramVal = sysParameter.getParamVal();
            JSONObject jsonObject = JSONObject.parseObject(paramVal);
            return jsonObject.getBoolean("enable");
        }
        return false;
    }

    @Override
    public void syncCache() {
        RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
        List<Map<String, Object>> mapList = this.baseMapper.queryDataToCache();
        redisUtil.set("gpio_cache", mapList);
    }

    @Override
    public void executeGpoCommand(List<JSONObject> algorithms) {
        RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
        List<Map<String, Object>> gpioCache = (ArrayList<Map<String, Object>>) redisUtil.get("gpio_cache");
        if (gpioCache.isEmpty()) return;
        for(Map<String, Object> map: gpioCache) {
            Integer algorithmCode = Convert.toInt(map.get("algorithmCode"));
            for (JSONObject json: algorithms) {
                Integer code = json.getInteger("algorithmCode");
                if (code.equals(algorithmCode)) {
                    String pinNumber = String.valueOf(map.get("pinNumber"));
                    String pinValue = String.valueOf(map.get("pinValue"));
                    SerialPinService serialPinService = SpringContextUtils.getBean(SerialPinService.class);
                    serialPinService.executeFileWrite(pinNumber, pinValue);
                    // io复位
                    SysParameter sysParameter = SysParameterUtil.getSysParameter("GPIO_RESET_TIME");
                    int timeSegment = 10;
                    if (sysParameter != null && StrUtil.isNotEmpty(sysParameter.getParamVal())){
                        timeSegment = Integer.parseInt(sysParameter.getParamVal());
                    }
                    timer.purge();
                    timer.schedule(new DidoTimerTask(json.getString("cameraId"), pinNumber, algorithmCode.toString(), timeSegment), timeSegment * 1000L);
                }
            }
        }
    }
}
