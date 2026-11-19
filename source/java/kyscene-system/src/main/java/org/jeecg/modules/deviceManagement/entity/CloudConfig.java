package org.jeecg.modules.deviceManagement.entity;

import lombok.Data;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.cameraFactory.entity.CameraFactory;
import org.jeecg.modules.cameraGroup.entity.CameraGroup;
import org.jeecg.modules.cameraGroupAlgorithm.entity.CameraGroupAlgorithm;
import org.jeecg.modules.cameraGroupCamera.entity.CameraGroupCamera;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.kycategory.entity.KyCategory;

import java.util.List;
@Data
public class CloudConfig {
    private String deviceIp;
    private String deviceId;
    private List<CameraFactory> cameraFactoryList;
    private List<KyCategory> areaList;
    private List<KyCategory> alarmTypeList;
    private List<CameraManager> cameraManagerList;
    private List<CameraManager> cameraByDeviceIdList;
    private List<AlgorithmManager> algorithmManagerList;
    private List<AlgorithmManager> cloudAlgorithmManagerList;
    private List<CameraGroup> cameraGroupList;
    private List<CameraGroupCamera> cameraGroupCameraList;
    private List<CameraGroupCamera> cameraGroupCameraByDeviceIdList;
    private List<CameraGroupAlgorithm> cameraGroupAlgorithmList;
    private List<KyCategory> cameraLocationList;
}
