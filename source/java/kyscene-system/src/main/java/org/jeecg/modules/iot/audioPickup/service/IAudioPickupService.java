package org.jeecg.modules.iot.audioPickup.service;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.iot.audioPickup.entity.AudioPickup;

/**
 * @author： DecoderX
 * @date： 2025/6/30
 */
public interface IAudioPickupService extends IService<AudioPickup> {

    IPage<AudioPickup> getAudioPickupList(Integer pageNo, Integer pageSize);

    void saveData(AudioPickup audioPickup);

    void updateData(AudioPickup audioPickup);

    Result<?> deleteData(String id);

    Result<?> startCollecting(String id);

    Result<?> stopCollecting(String id);

    JSONArray getAudioFileList(String id);

    Result<?> deleteByPath(String path);

    void stopAll();
}