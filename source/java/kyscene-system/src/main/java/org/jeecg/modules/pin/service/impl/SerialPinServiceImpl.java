package org.jeecg.modules.pin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pin.entity.SerialPinDO;
import org.jeecg.modules.pin.mapper.SerialPinMapper;
import org.jeecg.modules.pin.service.SerialPinService;
import org.jeecg.modules.pin.utils.GpioControl;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SerialPinServiceImpl extends ServiceImpl<SerialPinMapper, SerialPinDO> implements SerialPinService {

    @Override
    public IPage<SerialPinDO> getTableData(Integer pageNo, Integer pageSize) {
        Page<SerialPinDO> page = new Page<>(pageNo, pageSize);
        return this.page(page, Wrappers.lambdaQuery(SerialPinDO.class).orderByAsc(SerialPinDO::getPinNumber));
    }

    @Override
    public void updatePinSetting(SerialPinDO serialPinDO) {
        this.updateById(serialPinDO);
    }

    @Override
    public List<SerialPinDO> getList() {
        return this.list(Wrappers.lambdaQuery(SerialPinDO.class).eq(SerialPinDO::getStatus, 1)
                .orderByAsc(SerialPinDO::getPinNumber));
    }

    @Override
    public void addData(SerialPinDO serialPinDO) {
        Integer pinNumber = serialPinDO.getPinNumber();
        if (pinNumber == null) throw new JeecgBootException("端点编号为空");
        SerialPinDO queryResult = this.getOne(Wrappers.lambdaQuery(SerialPinDO.class)
                .eq(SerialPinDO::getPinNumber, serialPinDO.getPinNumber())
                .eq(SerialPinDO::getPinType, serialPinDO.getPinType())
                .eq(SerialPinDO::getPinVal, serialPinDO.getPinVal()));
        if (ObjectUtil.isNotEmpty(queryResult)) throw new JeecgBootException("请不要重复录入相同数据");
        serialPinDO.setStatus(1);
        this.save(serialPinDO);
    }

    @Override
    public void testPin(String id) {
        SerialPinDO serialPinDO = this.getById(id);
        Integer pinNumber = serialPinDO.getPinNumber();
        String pinVal = serialPinDO.getPinVal();
        GpioControl gpioControl = GpioControl.getInstance(String.valueOf(pinNumber), "out");
        try {
            gpioControl.init().setValue(pinVal);
        } catch (Exception e) {
            throw new JeecgBootException("执行异常：" + e.getLocalizedMessage());
        }
    }

    @Override
    public void executeFileWrite(String pinNumber, String pinValue) {
        GpioControl gpioControl = GpioControl.getInstance(pinNumber, "out");
        try {
            gpioControl.init().setValue(pinValue);
        } catch (Exception e) {
            log.error("GPIO执行异常：" + e.getLocalizedMessage());
        }
    }


}
