package org.jeecg.modules.serial.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gnu.io.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.serial.config.DataAvailableListener;
import org.jeecg.modules.serial.config.SerialPortUtil;
import org.jeecg.modules.serial.entity.*;
import org.jeecg.modules.serial.mapper.xml.SerialPortMapper;
import org.jeecg.modules.serial.service.SerialPortService;
import org.jeecg.modules.serial.service.SerialPortVoiceService;
import org.jeecg.modules.serial.service.SoundPillarVoiceService;
import org.jeecg.modules.serial.threads.CloseTimerTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SerialPortServiceImpl extends ServiceImpl<SerialPortMapper, SerialPortDo> implements SerialPortService {

    @Autowired
    private IKyCategoryService kyCategoryService;

    @Autowired
    private SerialPortVoiceService serialPortVoiceService;

    @Autowired
    private SoundPillarVoiceService soundPillarVoiceService;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${Serial.closeTime:5000}")
    private long closeTime;

    private final Timer timer = new Timer();

    @Override
    public List<KyCategory> getAlgorithmList() {
        LambdaQueryWrapper<KyCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KyCategory::getCategory, 0);
        queryWrapper.eq(KyCategory::getHasChild, 0);
        return kyCategoryService.list(queryWrapper);
    }

    @Override
    public void playSound(SoundPillarParam soundPillarParam) {
        int cacheCmdSize = SerialPortUtil.VOICE_PLAY_QUEUE.size();
        if (cacheCmdSize > 100) {
            log.error("播放指令缓存超过100，疑似音柱出现问题");
            return;
        }
        // 播放指令存入音频播放队列 FIFO
        SerialPortUtil.VOICE_PLAY_QUEUE.offer(soundPillarParam);
        log.info("缓存指令成功");
    }

    @Override
    public void closeVoice(String serialName, String closeCode) {
        Optional<Map<String, Object>> optional = SerialPortUtil.SERIAL_PORT_LIST.stream().filter(tempMap -> {
            String portName = tempMap.get("Name").toString();
            return serialName.equals(portName);
        }).findFirst();
        optional.ifPresent(map -> {
            SerialPort serialPort = (SerialPort) map.get("SerialPort");
            SerialPortUtil.sendData(serialPort, hexStringToByteArray(closeCode));
            log.info("串口 {} 发送close指令 {} 成功", serialName, closeCode);
        });
    }

    private SerialPortDo checkSerialPort(SerialSettingsVo serialSettingsVo) {
        // 查询串口表
        LambdaQueryWrapper<SerialPortDo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SerialPortDo::getSerialName, serialSettingsVo.getSerialName());
        SerialPortDo serialPortDo = this.getOne(queryWrapper, false);
        if (ObjectUtil.isNotEmpty(serialPortDo)) return serialPortDo;
        return null;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateSettings(SerialSettingsVo serialSettingsVo) {
        SerialPortDo serialPortDo = checkSerialPort(serialSettingsVo);
        if (serialPortDo == null) {
            // 插入
            serialPortDo = new SerialPortDo();
            BeanUtil.copyProperties(serialSettingsVo, serialPortDo, CopyOptions.create().ignoreNullValue());
            String generateId = IdUtil.fastSimpleUUID();
            serialPortDo.setId(generateId);
            serialPortDo.setStatus(serialSettingsVo.getSerialPortStatus());
            serialPortDo.setCreateTime(new Date());
            this.save(serialPortDo);
        } else{
            // 更新
            BeanUtil.copyProperties(serialSettingsVo, serialPortDo, CopyOptions.create().ignoreNullValue());
            serialPortDo.setUpdateTime(new Date());
            this.updateById(serialPortDo);
        }

        String voiceId = IdUtil.fastSimpleUUID();
        serialSettingsVo.setSerialPortVoiceId(voiceId);
        // 一种算法一个串口只能包含一种音频
        String portVoiceId = serialPortVoiceService.checkSaveData(serialPortDo.getId(), serialSettingsVo.getAlgorithm());
        if (portVoiceId != null) serialSettingsVo.setSerialPortVoiceId(portVoiceId);

        // 保存/更新数据到串口和音频的关联表
        SerialPortVoiceDo serialPortVoiceDo = new SerialPortVoiceDo();
        serialPortVoiceDo.setVoiceFileId(serialSettingsVo.getVoiceFileId());
        serialPortVoiceDo.setSerialPortId(serialPortDo.getId());
        serialPortVoiceDo.setId(serialSettingsVo.getSerialPortVoiceId());
        serialPortVoiceDo.setAlgorithm(serialSettingsVo.getAlgorithm());
        serialPortVoiceDo.setSoundName(serialSettingsVo.getSoundName());
        serialPortVoiceService.saveOrUpdate(serialPortVoiceDo);

        //刷新缓存
        this.refreshRedisCache();
    }

    @Override
    public void refreshRedisCache() {
        List<SerialPortVoiceDo> serialPortVoiceDos = serialPortVoiceService.list();
        List<SerialPortDo> serialPortDos = this.list();
        List<SoundPillar485> soundPillarVoices = soundPillarVoiceService.getSoundPillarVoices();
        List<SoundPillarParam> soundPillarParams = serialPortVoiceDos.stream().map(serialPortVoiceDo -> {
            SoundPillarParam soundPillarParam = new SoundPillarParam();
            soundPillarParam.setAlgorithm(serialPortVoiceDo.getAlgorithm());
            soundPillarParam.setSoundName(serialPortVoiceDo.getSoundName());
            for (SerialPortDo serialPortDo : serialPortDos) {
                if (serialPortDo.getId().equals(serialPortVoiceDo.getSerialPortId())) {
                    BeanUtil.copyProperties(serialPortDo, soundPillarParam, CopyOptions.create().ignoreNullValue().ignoreError());
                    break;
                }
            }
            for (SoundPillar485 soundPillar485 : soundPillarVoices) {
                if (ObjectUtil.equal(serialPortVoiceDo.getVoiceFileId(), soundPillar485.getId())) {
                    soundPillarParam.setVoiceFile(soundPillar485.getOpenCode());
                    break;
                }
            }
            return soundPillarParam;
        }).collect(Collectors.toList());
        redisUtil.set(SerialPortUtil.SERIAL_VOICE_ALGORITHM, soundPillarParams);
    }

    @Override
    public Result<IPage<SerialSettingsVo>> getSettingList(Integer pageNo, Integer pageSize) {
        Result<IPage<SerialSettingsVo>> result = new Result<>();

        Page<SerialPortVoiceDo> page = new Page<>(pageNo, pageSize);
        IPage<SerialPortVoiceDo> pageList = serialPortVoiceService.page(page);
        IPage<SerialSettingsVo> newPageList = new Page<>();
        BeanUtil.copyProperties(pageList, newPageList, CopyOptions.create().ignoreNullValue().ignoreError().setIgnoreProperties("records"));

        List<SerialPortVoiceDo> records = pageList.getRecords();
        if (ObjectUtil.isEmpty(records)) return result;

        List<SoundPillar485> voiceFiles = soundPillarVoiceService.list();
        List<SerialPortDo> serialPortDos = this.list();

        List<SerialSettingsVo> newRecords = records.stream().map(serialPortVoiceDo -> {
            SerialSettingsVo serialSettingsVo = new SerialSettingsVo();
            // 查询串口信息
            Optional<SerialPortDo> serialPortDo = serialPortDos.stream().filter(item -> item.getId().equals(serialPortVoiceDo.getSerialPortId())).findFirst();
            serialPortDo.ifPresent(item -> {
                BeanUtil.copyProperties(item, serialSettingsVo, CopyOptions.create().ignoreNullValue());
                serialSettingsVo.setSerialPortStatus(item.getStatus());
            });

            // 查询音频表
            Optional<SoundPillar485> soundPillar485 = voiceFiles.stream().filter(item -> item.getId().equals(serialPortVoiceDo.getVoiceFileId())).findFirst();
            soundPillar485.ifPresent(item -> serialSettingsVo.setVoiceFile(item.getOpenCode()));

            serialSettingsVo.setSerialPortVoiceId(serialPortVoiceDo.getId());
            serialSettingsVo.setSerialPortId(serialPortVoiceDo.getSerialPortId());
            serialSettingsVo.setVoiceFileId(serialPortVoiceDo.getVoiceFileId());
            serialSettingsVo.setAlgorithm(serialPortVoiceDo.getAlgorithm());
            serialSettingsVo.setSoundName(serialPortVoiceDo.getSoundName());
            return serialSettingsVo;
        }).collect(Collectors.toList());

        newPageList.setRecords(newRecords);
        result.setSuccess(true);
        result.setResult(newPageList);
        return result;
    }

    @Override
    public void deleteSetting(String id) {
        serialPortVoiceService.removeById(id);
    }

    @Override
    public void updateBySerialStatus(String serialName, Integer status) {
        // 数据库设置串口状态为离线
        LambdaQueryWrapper<SerialPortDo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SerialPortDo::getSerialName, serialName);
        SerialPortDo serialPortDo = this.getOne(queryWrapper);
        if (serialPortDo == null) return;
        serialPortDo.setStatus(status);
        serialPortDo.setUpdateTime(new Date());
        this.updateById(serialPortDo);
    }

    /**
     * 将命令转换为byte数组
     */
    private byte[] hexStringToByteArray(String s) {
        String[] hexBytes = s.split(" ");
        byte[] data = new byte[hexBytes.length];
        for (int i = 0; i < hexBytes.length; i++) {
            data[i] = (byte) Integer.parseInt(hexBytes[i], 16);
        }
        return data;
    }

    @Override
    public void playVoice(SoundPillarParam soundPillarParam) {
        SerialPort serialPort = getSerialPort(soundPillarParam);
        if (serialPort == null) {
            log.error("串口打开失败：{}", soundPillarParam.getSerialName());
            return;
        }
        SerialPortUtil.sendData(serialPort, hexStringToByteArray(soundPillarParam.getVoiceFile()));
        log.info("串口 {} 发送指令 {} 成功", soundPillarParam.getSerialName(), soundPillarParam.getVoiceFile());
        setRedisCache(soundPillarParam);
    }

    /**
     * 音频播放成功后缓存两个key
     */
    private void setRedisCache(SoundPillarParam soundPillarParam) {
        List<SoundPillar485> voices;
        try {
            Object cacheObj = redisUtil.get(SerialPortUtil.SERIAL_VOICE_CODE_LIST);
            if (ObjectUtil.isEmpty(cacheObj)) {
                SoundPillarVoiceService pillarVoiceService = SpringContextUtils.getBean(SoundPillarVoiceService.class);
                cacheObj = pillarVoiceService.getSoundPillarVoices();
                // 一小时过期
                if (ObjectUtil.isNotEmpty(cacheObj)) {
                    log.info("缓存音频文件成功，key {}", SerialPortUtil.SERIAL_VOICE_CODE_LIST);
                    redisUtil.set(SerialPortUtil.SERIAL_VOICE_CODE_LIST, cacheObj, 60 * 60);
                }
            }
            voices = (List<SoundPillar485>) cacheObj;
            Optional<SoundPillar485> pillar485 = voices.stream()
                    .filter(soundPillar485 -> soundPillar485.getOpenCode().equals(soundPillarParam.getVoiceFile()))
                    .findFirst();
            pillar485.ifPresent(soundPillar485 -> {
                // 发送指令5秒过期，通过监听此key,判断串口指令是否返回，返回会删除此key，未返回，那么key过期监听会触发离线事件
                String openKey = SerialPortUtil.SERIAL_VOICE_OPEN_PREFIX + soundPillarParam.getSerialName();
                redisUtil.set(openKey, soundPillarParam.getSerialName(), 5);
                // 5秒默认，定时关闭音柱
                timer.schedule(new CloseTimerTask(soundPillarParam.getSerialName(), soundPillar485.getCloseCode()), closeTime);
                // 内存回收
                timer.purge();
            });
        } catch (Exception e) {
            log.error("设置redis缓存失败 {}", e.getLocalizedMessage());
        }

    }

    /**
     * 获取可用串口
     */
    public SerialPort getSerialPort(SoundPillarParam soundPillarParam) {
        String serialPortName = soundPillarParam.getSerialName();
        Integer baudRate = soundPillarParam.getBaudRate();
        Integer dataBits = soundPillarParam.getDataBits();
        Integer stopBits = soundPillarParam.getStopBits();
        Integer parityBit = soundPillarParam.getParityBit();
        Optional<Map<String, Object>> optional = SerialPortUtil.SERIAL_PORT_LIST.stream()
                .filter(map -> serialPortName.equals(String.valueOf(map.get("Name"))))
                .findFirst();
        if (optional.isPresent()) return (SerialPort) optional.get().get("SerialPort");
        SerialPort serialPort = SerialPortUtil.openPort(serialPortName, baudRate, dataBits, stopBits, parityBit);
        if (serialPort == null) return null;
        // 添加监听器
        try {
            SerialPortUtil.addListener(serialPort, new DataAvailableListener(serialPort));
        } catch (TooManyListenersException e) {
            log.error("串口{}增加数据可用监听器错误", serialPort.getName(), e);
            return null;
        }
        //保存串口到内存
        Map<String, Object> serialMap = new HashMap<>();
        serialMap.put("Name", serialPortName);
        serialMap.put("SerialPort", serialPort);
        SerialPortUtil.SERIAL_PORT_LIST.add(serialMap);
        return serialPort;
    }
}
