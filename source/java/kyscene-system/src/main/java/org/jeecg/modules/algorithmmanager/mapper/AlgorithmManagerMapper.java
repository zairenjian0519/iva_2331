package org.jeecg.modules.algorithmmanager.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 算法实例
 * @Author: jeecg-boot
 * @Date:   2022-09-07
 * @Version: V1.0
 */
public interface AlgorithmManagerMapper extends BaseMapper<AlgorithmManager> {
    List<AlgorithmManager> getListByCameraId(String cameraId);

    void updateCameraStatus(String cameraId, String status);

    void updateAlgStatus(String cameraId, String status);

    int getLicenseNumByDevId(String devId);

    Integer getAllAlgorithmCount(List<String> cameraIds, List<String> categorys);

    int getCameraLicenseByDevId(String deviceId);

    void removeByGroupId(String groupId);

    List<AlgorithmManager> getByAlgGroupId(@Param("groupId") String groupId, @Param("algorithmId") String algorithmId);

    List<AlgorithmManager> getByGroupId(String groupId);

    void removeByCameraId(String cameraId);

    void removeByAlgId(String algId);

    List<AlgorithmManager> getAlarmTypeInfo();

    List<AlgorithmManager> getListByDevId(String devId);

    String getAlgIdsByGroupId(String id);

    AlgorithmManager getByCameraIdAndAlgorithmCode(@Param("cameraId") String cameraId, @Param("algorithmId") String algorithmId);
}
