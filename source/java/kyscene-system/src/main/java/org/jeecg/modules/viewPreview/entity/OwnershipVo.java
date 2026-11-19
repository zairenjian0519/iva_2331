package org.jeecg.modules.viewPreview.entity;

import lombok.Data;

import java.util.List;

@Data
public class OwnershipVo {

    private String id;

    private String cameraOwnership;

    private String cameraOwnershipDict;

    private List<ViewPreview> cameraOwnershipList;
}
