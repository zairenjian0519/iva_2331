package org.jeecg.modules.thirdApi.pojo;

import lombok.Data;

@Data
public class WebSocketmsg {

    private String camera_id;

    private String msg;

    private int type_id;
}
