package org.jeecg.modules.serial.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.serial.entity.SerialPortVoiceDo;
import org.jeecg.modules.serial.mapper.xml.SerialPortVoiceMapper;
import org.jeecg.modules.serial.service.SerialPortVoiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SerialPortVoiceServiceImpl extends ServiceImpl<SerialPortVoiceMapper, SerialPortVoiceDo> implements SerialPortVoiceService {

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateData(SerialPortVoiceDo serialPortVoiceDo) {
        this.saveOrUpdate(serialPortVoiceDo);
//        if (ObjectUtil.isNotEmpty(serialPortVoiceDo.getId())) {
//            this.updateById(serialPortVoiceDo);
//        }
//        if (ObjectUtil.isEmpty(serialPortVoiceDo.getId())) {
//            serialPortVoiceDo.setId(IdUtil.fastSimpleUUID());
//            this.save(serialPortVoiceDo);
//        }
    }

    @Override
    public String checkSaveData(String serialPortId, Integer algorithm) {
        LambdaQueryWrapper<SerialPortVoiceDo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SerialPortVoiceDo::getAlgorithm, algorithm);
        queryWrapper.eq(SerialPortVoiceDo::getSerialPortId, serialPortId);
        SerialPortVoiceDo one = this.getOne(queryWrapper, false);
        if (ObjectUtil.isNotEmpty(one)) return one.getId();
        return null;
    }
}
