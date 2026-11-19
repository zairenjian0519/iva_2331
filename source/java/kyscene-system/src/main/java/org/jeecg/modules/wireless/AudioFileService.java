package org.jeecg.modules.wireless;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.wireless.entity.AudioFileInfo;

import java.util.List;

public interface AudioFileService extends IService<AudioFileInfo> {

    Result<IPage<AudioFileInfo>> listAudioFiles(Integer pageNo, Integer pageSize);

    void syncAudioFiles();

    void updateVoiceAlgorithm(AudioFileInfo audioFileInfo);

    List<AudioFileInfo> getAvailableAudioFiles();

}
