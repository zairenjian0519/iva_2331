package org.jeecg.modules.cameraGroupAlgorithm.service;

import org.jeecg.modules.cameraGroupAlgorithm.entity.CameraGroupAlgorithm;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @Description: 轮询组算法表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
public interface ICameraGroupAlgorithmService extends IService<CameraGroupAlgorithm> {

    List<CameraGroupAlgorithm> getAlgorithmByGroupId(String groupId);

    void removeByGroupId(String id);

    CameraGroupAlgorithm getAlgByGroupIdAndAlgId(String groupId, String algorithmId);

    List<CameraGroupAlgorithm> alarmTypeInfoList();
}
