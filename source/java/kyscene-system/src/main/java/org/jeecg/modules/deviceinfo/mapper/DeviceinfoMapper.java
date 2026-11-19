package org.jeecg.modules.deviceinfo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.deviceinfo.entity.Deviceinfo;

import java.util.List;


/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author
 * @since 2023-12-07
 */

public interface DeviceinfoMapper extends BaseMapper<Deviceinfo> {
    @Select("SELECT * from deviceinfo where platformid = #{sipid} ")
    List<Deviceinfo> getFindAll(String sipid);
    @Select("SELECT * from deviceinfo where sipid = #{sipid} ")
    List<Deviceinfo> getsipidAll(String sipid);
}
