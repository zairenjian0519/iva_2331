package org.jeecg.modules.kycategory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.kycategory.entity.KyCategory;

import java.util.List;
import java.util.Map;

/**
 * @Description: 分类数据
 * @Author: jeecg-boot
 * @Date: 2022-08-08
 * @Version: V1.0
 */
public interface KyCategoryMapper extends BaseMapper<KyCategory> {

    String getAreaListByAreaId(@Param("alarmAreaId") String alarmArea);

    /**
     * 编辑节点状态
     *
     * @param id
     * @param status
     */
    void updateTreeNodeStatus(@Param("id") String id, @Param("status") String status);

    /**
     * 【vue3专用】根据父级ID查询树节点数据
     *
     * @param pid
     * @param query
     * @return
     */
    List<SelectTreeModel> queryListByPid(@Param("pid") String pid, @Param("query") Map<String, String> query);


    List<KyCategory> getTreeLocationById(String locationId);

    @Select("select * from ky_category where code=#{alarmType}")
    KyCategory getOneCode(String alarmType);
}
