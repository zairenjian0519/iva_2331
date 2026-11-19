package org.jeecg.modules.algorithmmanager.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.algorithmmanager.mapper.AlgorithmManagerMapper;
import org.jeecg.modules.algorithmmanager.service.IAlgorithmManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
 * @Description: 算法实例
 * @Author: jeecg-boot
 * @Date:   2022-09-07
 * @Version: V1.0
 */
@Service
public class AlgorithmManagerServiceImpl extends ServiceImpl<AlgorithmManagerMapper, AlgorithmManager> implements IAlgorithmManagerService {

    @Autowired
    private AlgorithmManagerMapper managerMapper;

    @Override
    public List<AlgorithmManager> getListByCameraId(String cameraId) {
        return managerMapper.getListByCameraId(cameraId);
    }

    @Override
    public List<AlgorithmManager> getListByCameraIds(List<String> cameraIds) {
        return this.list(Wrappers.lambdaQuery(AlgorithmManager.class).in(AlgorithmManager::getBelongTo, cameraIds));
    }

    @Override
    public void updateCameraStatus(String cameraId, String status) {
        managerMapper.updateCameraStatus(cameraId, status);
        managerMapper.updateAlgStatus(cameraId, status);
    }

    @Override
    public int getLicenseNumByDevId(String devId) {
        return managerMapper.getLicenseNumByDevId(devId);
    }

    @Override
    public Integer getAllAlgorithmCount(List<String> cameraIds, List<String> categorys) {
        return managerMapper.getAllAlgorithmCount(cameraIds,categorys);
    }

    @Override
    public int getCameraLicense(String deviceId) {
        return managerMapper.getCameraLicenseByDevId(deviceId);
    }

    @Override
    public void removeByGroupId(String groupId) {
        managerMapper.removeByGroupId(groupId);
    }

    @Override
    public List<AlgorithmManager> getByAlgGroupId(String groupId, String algorithmId) {
        return managerMapper.getByAlgGroupId(groupId,algorithmId);
    }

    @Override
    public List<AlgorithmManager> getByGroupId(String groupId) {
        return managerMapper.getByGroupId(groupId);
    }

    @Override
    public void removeByCameraId(String cameraId) {
        managerMapper.removeByCameraId(cameraId);
    }

    @Override
    public void removeByAlgId(String algId) {
        managerMapper.removeByAlgId(algId);
    }

    @Override
    public List<AlgorithmManager> alarmTypeInfoList() {
        return managerMapper.getAlarmTypeInfo();
    }

    @Override
    public List<AlgorithmManager> getListByDevId(String devId) {
        return managerMapper.getListByDevId(devId);
    }

    @Override
    public String getAlgIdsByGroupId(String id) {
        return managerMapper.getAlgIdsByGroupId(id);
    }

}
