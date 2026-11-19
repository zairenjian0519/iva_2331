package org.jeecg.modules.cameraGroupAlgorithm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.cameraGroupAlgorithm.entity.CameraGroupAlgorithm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 轮询组算法表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
public interface CameraGroupAlgorithmMapper extends BaseMapper<CameraGroupAlgorithm> {

    List<CameraGroupAlgorithm> getAlgorithmByGroupId(String groupId);

    void removeByGroupId(String id);

    CameraGroupAlgorithm getAlgByGroupIdAndAlgId(@Param("groupId") String groupId, @Param("algorithmId") String algorithmId);

    List<CameraGroupAlgorithm> getAlarmTypeInfo();
}
