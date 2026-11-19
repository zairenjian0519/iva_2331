package org.jeecg.modules.wireless;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.wireless.entity.SoundDeviceInfo;

import java.util.List;

public interface SoundPillarService extends IService<SoundDeviceInfo> {

    void restartDevice();

    void updateSoundDeviceInfo(List<SoundDeviceInfo> list);

    Result<IPage<SoundDeviceInfo>> getDeviceList(Integer pageNo, Integer pageSize);

    void updateStatus(Integer status, String id);

    List<KyCategory> getAlgorithmList();

    String playAudioFile(JSONObject params);

    void updateVolume(Integer deviceId, Integer volume);

    void closePlayTask(String taskCode);

    JSONObject getSoundPillarAccountInfo();

    //是否启用音柱
    boolean isEnableSoundPillar();

    //清空音柱设备信息
    void truncateDeviceInfo();

    void syncDevices(String soundPillarDir);

    JSONObject getProjectInfo();
}
