package org.jeecg.modules.wireless.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.wireless.entity.SoundDeviceInfo;

@Mapper
public interface SoundDeviceMapper extends BaseMapper<SoundDeviceInfo> {
    //清空表
    @Update("truncate table sound_device_info")
    void truncateDeviceInfo();

}
