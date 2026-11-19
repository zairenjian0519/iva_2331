package org.jeecg.modules.viewPreview.entity;

import lombok.Data;

import java.util.List;
@Data
public class LocationVo {
    private String id;

    private String cameraLocation;

    private String cameraLocationDict;

    private List<ViewPreview> cameraLocationList;


}
