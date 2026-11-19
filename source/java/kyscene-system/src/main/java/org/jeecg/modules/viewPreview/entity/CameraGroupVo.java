package org.jeecg.modules.viewPreview.entity;

import lombok.Data;

import java.util.List;
@Data
public class CameraGroupVo {
    List<ViewPreview> byLocationList;
    List<ViewPreview> byOwnershipList;
    private int total;
}
