package org.jeecg.modules.cameraManager.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.cameraManager.entity.ExportCamera;
import org.jeecg.modules.algorithmmanager.mapper.AlgorithmManagerMapper;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.mapper.CameraManagerMapper;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.mapper.DeviceManagementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.HashMap;
import java.util.List;

/**
 * @Description: 相机管理
 * @Author: jeecg-boot
 * @Date:   2022-08-23
 * @Version: V1.0
 */
@Service
public class CameraManagerServiceImpl extends ServiceImpl<CameraManagerMapper, CameraManager> implements ICameraManagerService {

    @Autowired
    private CameraManagerMapper cameraManagerMapper;
    @Autowired
    private AlgorithmManagerMapper algorithmmanager;
    @Autowired
    private DeviceManagementMapper deviceManagementMapper;

    @Override
    public IPage<CameraManager> pageList(IPage page, CameraManager cameraManager){
        return cameraManagerMapper.pageList(page,cameraManager);
    }

    @Override
    public List<CameraManager> getListByDeviceId(String deviceId) {
        return cameraManagerMapper.getListByDeviceId(deviceId);
    }

    @Override
    public void updateStatusById(String cameraId, Integer status) {
        cameraManagerMapper.updateStatusById(cameraId, status);
    }

    @Override
    public void removeByDeviceId(String deviceId) {
        List<CameraManager> managers = cameraManagerMapper.getListByDeviceId(deviceId);
        for(CameraManager cameraManager : managers) {
            algorithmmanager.deleteByMap(new HashMap<String, Object>(){{ put("belong_to", cameraManager.getId());}});
            cameraManagerMapper.deleteById(cameraManager.getId());
        }
    }

    @Override
    public Integer getCountNum(List<String> treeIds) {
        return cameraManagerMapper.getCountNum(treeIds);
    }

    @Override
    public Integer onLineCameraCount(CameraManager cameraManager) {
        return cameraManagerMapper.onLineCameraCount(cameraManager);
    }

    @Override
    public List<ExportCamera> exportCamera(CameraManager cameraManager) {
        return cameraManagerMapper.exportCamera(cameraManager);
    }

    @Override
    public String createIdByDevice(String devId) {
        CameraManager camera = cameraManagerMapper.getLastByDeviceId(devId);
        DeviceManagement dev = deviceManagementMapper.selectById(devId);
        if(null == dev || null == dev.getDeviceCode()) return String.format("%020d", 1);
        if(null == camera || camera.getId().length() != 20) return dev.getDeviceCode() + String.format("%07d", 1);
        String lastNum = camera.getId().substring(13);
        int num = Integer.parseInt(lastNum);
        return dev.getDeviceCode()+ String.format("%07d", num + 1);
    }

    @Override
    public void deleteCameraLocation(List<String> treeIds) {
        cameraManagerMapper.deleteLocation(treeIds);
    }

    @Override
    public void deleteAlarm(String cameraId) {
        cameraManagerMapper.deleteAlarm(cameraId);
    }

    @Override
    public List<String> getTreeId(String categoryId) {
        return cameraManagerMapper.getTreeId(categoryId);
    }

    @Override
    public List<CameraManager> getAlgorithm() {
        return cameraManagerMapper.getAlgorithm();
    }

    @Override
    public List<CameraManager> getCameraByGroup(String deviceId, String groupId) {
        return cameraManagerMapper.getCameraByGroup(deviceId,groupId);
    }

    @Override
    public List<CameraManager> getCameraByGroups(String deviceId, List<String> groupIds) {
        return cameraManagerMapper.getCameraByGroups(deviceId, groupIds);
    }

    /**
     * 获取相机视频流地址
     * @param cameraId
     * @return
     */
    @Override
    public String getStreamAddress(String cameraId) {
        CameraManager cameraManager = this.baseMapper.selectById(cameraId);
        return cameraManager.getStreamAddress();
    }

}
