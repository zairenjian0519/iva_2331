package org.jeecg.modules.serial.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.serial.entity.SerialPortDo;
import org.jeecg.modules.serial.entity.SerialSettingsVo;
import org.jeecg.modules.serial.entity.SoundPillarParam;

import java.util.List;

/**
 * 串口service接口
 */
public interface SerialPortService extends IService<SerialPortDo> {

    List<KyCategory> getAlgorithmList();

    void playSound(SoundPillarParam soundPillarParam);

    void saveOrUpdateSettings(SerialSettingsVo serialSettingsVo);

    Result<IPage<SerialSettingsVo>> getSettingList(Integer pageNo, Integer pageSize);

    void closeVoice(String serialName, String closeCode);

    void refreshRedisCache();

    void deleteSetting(String id);

    void updateBySerialStatus(String serialName, Integer status);

    void playVoice(SoundPillarParam soundPillarParam);
}
