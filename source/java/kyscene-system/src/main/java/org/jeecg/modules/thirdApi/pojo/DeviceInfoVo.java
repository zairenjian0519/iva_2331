package org.jeecg.modules.thirdApi.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 设备信息接口
 * 返回Vo
 */
@Data
public class DeviceInfoVo {

    /**
     * 主键
     */
    private String id;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备ip
     */
    private String deviceIp;

    /**
     * 类别
     */
    @Dict(dicCode = "device_category")
    private String category;

    /**
     * 状态
     */
    @Dict(dicCode = "device_status")
    private String status;

}
