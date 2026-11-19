package org.jeecg.modules.kycategory.entity;
import lombok.Data;

import java.util.List;

@Data
public class TreeVo {


    private String title;

    private String value;

    private List<TreeVo> children;
}
