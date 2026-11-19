package org.jeecg.modules.network.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Desc:
 * @Author:lvgh
 * @Date:2024/5/17 14:45
 **/
@Data
public class NetworkInfoVo {

    private String networkCard;
    private String name;
    private String ipMethod;
    private String ipAddress;
    private String subnetMask;
    private String gateway;
    private List<String> dns = new ArrayList<>();

}
