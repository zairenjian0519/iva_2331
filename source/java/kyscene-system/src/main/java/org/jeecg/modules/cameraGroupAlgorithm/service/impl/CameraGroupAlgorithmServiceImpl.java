package org.jeecg.modules.cameraGroupAlgorithm.service.impl;

import org.jeecg.modules.cameraGroupAlgorithm.entity.CameraGroupAlgorithm;
import org.jeecg.modules.cameraGroupAlgorithm.mapper.CameraGroupAlgorithmMapper;
import org.jeecg.modules.cameraGroupAlgorithm.service.ICameraGroupAlgorithmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
 * @Description: 轮询组算法表
 * @Author: jeecg-boot
 * @Date:   2023-02-17
 * @Version: V1.0
 */
@Service
public class CameraGroupAlgorithmServiceImpl extends ServiceImpl<CameraGroupAlgorithmMapper, CameraGroupAlgorithm> implements ICameraGroupAlgorithmService {
    @Autowired
    CameraGroupAlgorithmMapper cameraGroupAlgorithmMapper;

    @Override
    public List<CameraGroupAlgorithm> getAlgorithmByGroupId(String groupId) {
        return cameraGroupAlgorithmMapper.getAlgorithmByGroupId(groupId);
    }

    @Override
    public void removeByGroupId(String id) {
        cameraGroupAlgorithmMapper.removeByGroupId(id);
    }

    @Override
    public CameraGroupAlgorithm getAlgByGroupIdAndAlgId(String groupId, String algorithmId) {
        return cameraGroupAlgorithmMapper.getAlgByGroupIdAndAlgId(groupId,algorithmId);
    }

    @Override
    public List<CameraGroupAlgorithm> alarmTypeInfoList() {
        return cameraGroupAlgorithmMapper.getAlarmTypeInfo();
    }
}
