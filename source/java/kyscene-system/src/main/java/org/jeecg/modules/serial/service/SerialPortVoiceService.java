package org.jeecg.modules.serial.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.serial.entity.SerialPortVoiceDo;

public interface SerialPortVoiceService extends IService<SerialPortVoiceDo> {
    void saveOrUpdateData(SerialPortVoiceDo serialPortVoiceDo);

    String checkSaveData(String serialPortId, Integer algorithm);
}
