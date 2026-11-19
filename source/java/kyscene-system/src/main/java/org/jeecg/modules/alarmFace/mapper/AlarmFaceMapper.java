package org.jeecg.modules.alarmFace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.alarmFace.AlarmFaceDo;
import org.jeecg.modules.alarmFace.AlarmFaceParams;

/**
 * @author： DecoderX
 * @date： 2025/4/8
 */
@Mapper
public interface AlarmFaceMapper extends BaseMapper<AlarmFaceDo> {

    IPage<AlarmFaceDo> selectPageData(Page<AlarmFaceDo> page, @Param("alarmFaceParams") AlarmFaceParams alarmFaceParams);
}
