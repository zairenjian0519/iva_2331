package org.jeecg.modules.deviceinfo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.deviceinfo.entity.Deviceinfo;
import org.jeecg.modules.deviceinfo.mapper.DeviceinfoMapper;
import org.jeecg.modules.deviceinfo.service.DeviceinfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 
 * @since 2023-12-07
 */
@Service
public class DeviceinfoServiceImpl extends ServiceImpl<DeviceinfoMapper, Deviceinfo> implements DeviceinfoService {
    @Autowired
    private DeviceinfoMapper deviceinfoMapper;
    @Override
    public List<Deviceinfo> getFindAll(String sipid) {
        List<Deviceinfo> deviceinfoList = deviceinfoMapper.getFindAll(sipid);

        return deviceinfoList;
    }

    @Override
    public List<Deviceinfo> getsipidAll(String sipid) {
        List<Deviceinfo> deviceinfoList = deviceinfoMapper.getsipidAll(sipid);
        return deviceinfoList;
    }
}
