package org.jeecg.modules.pin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.pin.entity.SerialPinDO;

import java.util.List;

public interface SerialPinService extends IService<SerialPinDO> {

    void addData(SerialPinDO serialPinDO);

    IPage<SerialPinDO> getTableData(Integer pageNo, Integer pageSize);

    void updatePinSetting(SerialPinDO serialPinDO);

    List<SerialPinDO> getList();

    void testPin(String id);

    void executeFileWrite(String pinNumber, String pinValue);
}
