package org.jeecg.modules.cameraManager.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class CameraCheckEvent extends ApplicationEvent {

    private final List<CameraEventMsg> cameraEventMsgList;

    private final CameraEventOptions eventOptions;

    public CameraCheckEvent(Object source, List<CameraEventMsg> cameraEventMsgList, CameraEventOptions eventOptions) {
        super(source);
        this.cameraEventMsgList = cameraEventMsgList;
        this.eventOptions = eventOptions;
    }
}
