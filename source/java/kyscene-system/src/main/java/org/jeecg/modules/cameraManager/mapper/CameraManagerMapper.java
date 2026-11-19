package org.jeecg.modules.cameraManager.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.cameraManager.entity.ExportCamera;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 相机管理
 * @Author: jeecg-boot
 * @Date:   2022-08-23
 * @Version: V1.0
 */
public interface CameraManagerMapper extends BaseMapper<CameraManager> {

    IPage<CameraManager> pageList(IPage page, @Param("cameraManager")CameraManager cameraManager);

    List<CameraManager> getListByDeviceId(String deviceId);

    void updateStatusById(String cameraId, Integer status);

    Integer getCountNum(List<String> ids);

    Integer onLineCameraCount(@Param("cameraManager")CameraManager cameraManager);

    List<ExportCamera> exportCamera(@Param("cameraManager")CameraManager cameraManager);

    CameraManager getLastByDeviceId(String devId);

    void deleteLocation(@Param("treeIds") List<String> treeIds);

    void deleteAlarm(String cameraId);

    List<String> getTreeId(String categoryId);

    List<CameraManager> getAlgorithm();

    List<CameraManager> getCameraByGroup(@Param("deviceId") String deviceId, @Param("groupId") String groupId);

    List<CameraManager> getCameraByGroups(@Param("deviceId") String deviceId, @Param("groupIds") List<String> groupIds);
}
