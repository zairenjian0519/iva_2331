package org.jeecg.modules.cameraManager.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 相机事件发布者
 */
@RequiredArgsConstructor
@Component
public class CameraEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishCameraEvent(List<CameraEventMsg> cameraEventMsgList, CameraEventOptions eventOptions) {
        CameraCheckEvent cameraCheckEvent = new CameraCheckEvent(this, cameraEventMsgList, eventOptions);
        applicationEventPublisher.publishEvent(cameraCheckEvent);
    }

}
