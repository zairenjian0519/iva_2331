package org.jeecg.modules.iot.serial;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author： DecoderX
 * @date： 2025/10/21
 */
public interface Serial485ReaderService extends IService<Serial485Reader> {

    IPage<Serial485Reader> queryPageList(IPage<Serial485Reader> page);

    void addData(Serial485Reader serial485Reader);

    void updateData(Serial485Reader serial485Reader);

    void deleteData(String id);

    void saveToSystemParam(Serial485Setting serial485Setting);

    Serial485Setting getCommInfo();

    void disconnectCommPort();

    void sendCommand(String id);

    Serial485Reader getBySlaveAddress(Integer slaveAddress);
}
