package org.jeecg.modules.cameraManager.job;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.event.CameraEventMsg;
import org.jeecg.modules.cameraManager.event.CameraEventOptions;
import org.jeecg.modules.cameraManager.event.CameraEventPublisher;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.jeecg.modules.deviceManagement.entity.DeviceManagement;
import org.jeecg.modules.deviceManagement.service.IDeviceManagementService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * 定时检查相机状态
 */
@Slf4j
@DisallowConcurrentExecution
public class CameraStatusCheckJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("定时检查相机状态开始......");

        IDeviceManagementService deviceManagementService = SpringContextUtils.getBean(IDeviceManagementService.class);
        CameraEventPublisher cameraEventPublisher = SpringContextUtils.getBean(CameraEventPublisher.class);
        ICameraManagerService cameraManagerService = SpringContextUtils.getBean(ICameraManagerService.class);

        List<DeviceManagement> deviceManagementList = deviceManagementService.list();
        if (deviceManagementList == null || deviceManagementList.isEmpty()) {
            log.info("未查询到设备，取消检查相机状态");
            return;
        }
        for (DeviceManagement deviceManagement : deviceManagementList) {
            String deviceIp = deviceManagement.getDeviceIp();
            CameraEventOptions cameraEventOptions = new CameraEventOptions();
            cameraEventOptions.setEventType("schedule_job");
            cameraEventOptions.setDeviceIp(deviceIp);

            List<CameraManager> cameraList = cameraManagerService.getListByDeviceId(deviceManagement.getId());
            if (cameraList == null || cameraList.isEmpty()) {
                log.info("未查询到相机，取消检查相机状态");
                return;
            }
            cameraEventOptions.setCameraOriginalList(cameraList);

            List<CameraEventMsg> cameraEventMsgList = new ArrayList<>();
            for (CameraManager camera : cameraList) {
                CameraEventMsg cameraEventMsg = new CameraEventMsg();
                cameraEventMsg.setCameraId(camera.getId());
                cameraEventMsg.setStreamAddress(camera.getStreamAddress());
                cameraEventMsgList.add(cameraEventMsg);
            }

            cameraEventPublisher.publishCameraEvent(cameraEventMsgList, cameraEventOptions);
        }
    }
}

