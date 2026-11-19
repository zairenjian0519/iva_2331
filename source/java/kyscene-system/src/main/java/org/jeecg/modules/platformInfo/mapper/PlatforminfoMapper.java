package org.jeecg.modules.platformInfo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.platformInfo.entity.Platforminfo;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author
 * @since 2023-12-06
 */

public interface PlatforminfoMapper extends BaseMapper<Platforminfo> {

 /*   @Insert(" insert into platforminfo (address, name, password, port, sipid, deviceidlist, type) values (#{address}, #{name}, #{password}, #{port}, #{sipid}, #{deviceidlist}, #{type})")
    void savePlatForminfo(Platforminfo pfi);*/
}
