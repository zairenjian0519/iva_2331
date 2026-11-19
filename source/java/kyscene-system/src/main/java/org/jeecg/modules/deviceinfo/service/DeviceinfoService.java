package org.jeecg.modules.deviceinfo.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.deviceinfo.entity.Deviceinfo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 
 * @since 2023-12-07
 */
public interface DeviceinfoService extends IService<Deviceinfo> {

    List<Deviceinfo> getFindAll(String sipid);

    List<Deviceinfo> getsipidAll(String sipid);
}
