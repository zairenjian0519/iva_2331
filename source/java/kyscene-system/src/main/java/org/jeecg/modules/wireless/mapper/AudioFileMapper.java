package org.jeecg.modules.wireless.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.wireless.entity.AudioFileInfo;

@Mapper
public interface AudioFileMapper extends BaseMapper<AudioFileInfo> {
    @Update("truncate table sound_device_audio")
    void truncateAudioInfo();
}
