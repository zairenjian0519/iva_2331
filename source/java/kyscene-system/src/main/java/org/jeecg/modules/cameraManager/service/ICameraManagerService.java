package org.jeecg.modules.cameraManager.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.cameraManager.entity.ExportCamera;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @Description: 相机管理
 * @Author: jeecg-boot
 * @Date:   2022-08-23
 * @Version: V1.0
 */
public interface ICameraManagerService extends IService<CameraManager> {


    IPage<CameraManager> pageList(IPage page,CameraManager cameraManager);

    List<CameraManager> getListByDeviceId(String deviceId);

    void updateStatusById(String cameraId, Integer status);

    void removeByDeviceId(String id);

    Integer getCountNum(List<String> treeIds);

    Integer onLineCameraCount(CameraManager cameraManager);

    List<ExportCamera> exportCamera(CameraManager cameraManager);

    String createIdByDevice(String devId);

    void deleteCameraLocation(List<String> treeIds);

    void deleteAlarm(String id);

    List<String> getTreeId(String categoryId);

    List<CameraManager> getAlgorithm();

    List<CameraManager> getCameraByGroup(String deviceId, String groupId);

    List<CameraManager> getCameraByGroups(String deviceId, List<String> groupIds);

    String getStreamAddress(String cameraId);

}
