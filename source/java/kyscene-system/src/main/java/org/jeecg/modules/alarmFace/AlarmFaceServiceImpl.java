/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.alarmFace;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.alarmFace.mapper.AlarmFaceMapper;
import org.springframework.stereotype.Service;

/**
 * @author： DecoderX
 * @date： 2025/4/8
 */
@Service
public class AlarmFaceServiceImpl extends ServiceImpl<AlarmFaceMapper, AlarmFaceDo> implements AlarmFaceService {
    @Override
    public void saveAlarmFace(AlarmFaceDo alarmFaceDo) {
        this.save(alarmFaceDo);
    }

    @Override
    public IPage<AlarmFaceDo> getPageData(Integer pageNo, Integer pageSize, AlarmFaceParams alarmFaceParams) {
        Page<AlarmFaceDo> page = new Page<>(pageNo, pageSize);
        return this.baseMapper.selectPageData(page, alarmFaceParams);
    }
}
