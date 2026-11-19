package org.jeecg.modules.iot;

import lombok.Getter;

/**
 * @author： DecoderX
 * @date： 2025/10/13
 */
@Getter
public enum DeviceEnum {
    ELECTRIC_LOCK_HEART("electric_lock_heart", "电磁锁心跳"),
    ELECTRIC_LOCK_REALTIME_DATA("electric_lock_realtime_data", "电磁锁实时数据"),
    ELECTRIC_LOCK_DEVICES("electric_lock_device_list", "电磁锁设备列表"),

    TH_SENSOR_HEART("th_sensor_heart", "温湿度传感器心跳"),
    TH_SENSOR_DEVICE_LIST("th_sensor_device_list", "温湿度传感器列表"),
    TH_SENSOR_REALTIME_DATA("th_sensor_realtime_data", "温湿度传感器实时数据"),

    AUDIO_PICKUP_HEART("audio_pickup_heart", "音频拾取器心跳"),
    AUDIO_PICKUP_REALTIME_DATA("audio_pickup_realtime_data", "音频拾取器实时数据"),
    AUDIO_PICKUP_DEVICE_LIST("audio_pickup_device_list", "音频拾取器设备列表"),

    CAMERA_DEVICE_LIST("camera_device_list", "摄像头设备列表"),
    CAMERA_DEVICE_HEART("camera_device_heart", "摄像头心跳");


    private final String code;
    private final String desc;

    DeviceEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
