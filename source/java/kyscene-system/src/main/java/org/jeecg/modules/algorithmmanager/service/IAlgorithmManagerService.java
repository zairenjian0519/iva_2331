package org.jeecg.modules.algorithmmanager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;

import java.util.List;

/**
 * @Description: 算法实例
 * @Author: jeecg-boot
 * @Date:   2022-09-07
 * @Version: V1.0
 */
public interface IAlgorithmManagerService extends IService<AlgorithmManager> {

    List<AlgorithmManager> getListByCameraId(String cameraId);

    void updateCameraStatus(String cameraId, String status);

    int getLicenseNumByDevId(String id);

    Integer getAllAlgorithmCount(List<String> cameraIds, List<String> categorys);

    int getCameraLicense(String deviceId);

    void removeByGroupId(String groupId);

    List<AlgorithmManager> getByAlgGroupId(String groupId, String algorithmId);

    List<AlgorithmManager> getByGroupId(String groupId);

    void removeByCameraId(String cameraId);

    void removeByAlgId(String algId);

    List<AlgorithmManager> alarmTypeInfoList();

    List<AlgorithmManager> getListByDevId(String devId);

    String getAlgIdsByGroupId(String id);

    List<AlgorithmManager> getListByCameraIds(List<String> cameraIds);
}
