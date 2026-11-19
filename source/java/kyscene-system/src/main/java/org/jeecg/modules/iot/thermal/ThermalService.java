package org.jeecg.modules.iot.thermal;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.iot.thermal.entity.ThermalDeviceInfo;

/**
 * @author： DecoderX
 * @date： 2025/11/18
 */
public interface ThermalService extends IService<ThermalDeviceInfo> {

    IPage<ThermalDeviceInfo> queryPageList(IPage<ThermalDeviceInfo> page);

    void addData(ThermalDeviceInfo thermalDeviceInfo);

    void updateData(ThermalDeviceInfo thermalDeviceInfo);

    void deleteRow(String id);

    void initAllCameraLogin();

    void updateStatusByIp(String pchDVRIP, int i);
}
