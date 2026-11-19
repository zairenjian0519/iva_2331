package org.jeecg.modules.face.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.face.entity.FaceDetailDo;

@Mapper
public interface FaceDetailMapper extends BaseMapper<FaceDetailDo> {
    /**
     * 清空表数据
     */
    @Delete("TRUNCATE TABLE face_detail")
    void truncateTable();

}
