package org.jeecg.modules.dataPush;

import lombok.Data;

@Data
public class StreamBizInfo {

    private boolean enable;

    private String bizIp;

    private Integer bizPort;
}
