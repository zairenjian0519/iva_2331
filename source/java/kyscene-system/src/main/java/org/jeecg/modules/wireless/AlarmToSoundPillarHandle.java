package org.jeecg.modules.wireless;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.jeecg.modules.wireless.entity.AudioFileInfo;
import org.jeecg.modules.wireless.entity.SoundDeviceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AlarmToSoundPillarHandle {

    private SoundPillarService soundPillarService;

    private AudioFileService audioFileService;

    @Autowired
    public void setAudioFileService(AudioFileService audioFileService) {
        this.audioFileService = audioFileService;
    }

    @Autowired
    public void setSoundPillarService(SoundPillarService soundPillarService) {
        this.soundPillarService = soundPillarService;
    }

    @Async
    public void handleSoundPillarPlay(List<JSONObject> alarmList) {
        for (JSONObject alarm : alarmList) {
            String algorithmCode = alarm.getString("algorithmCode");
            // 获取音频文件
            LambdaQueryWrapper<AudioFileInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AudioFileInfo::getAlgorithm, algorithmCode);
            queryWrapper.eq(AudioFileInfo::getStatus, 1);
            List<AudioFileInfo> audioFileInfoList = audioFileService.list(queryWrapper);
            if (ObjectUtil.isEmpty(audioFileInfoList)) {
                log.error("算法没有绑定音频或者音频未启用");
                continue;
            }

            // 获取音柱设备
            LambdaQueryWrapper<SoundDeviceInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(SoundDeviceInfo::getAppState, "online");
            lambdaQueryWrapper.eq(SoundDeviceInfo::getEnable, 1);
            List<SoundDeviceInfo> soundDeviceInfos = soundPillarService.list(lambdaQueryWrapper);
            if (ObjectUtil.isEmpty(soundDeviceInfos)) {
                log.error("没有查询到音柱或者音柱未启用，离线");
                return;
            }

            String cameraId = alarm.getString("cameraId");
            List<SoundDeviceInfo> devices = soundDeviceInfos.stream().filter(soundDeviceInfo -> {
                String cameraListStr = soundDeviceInfo.getCameraList();
                if (StrUtil.isEmpty(cameraListStr)) return false;
                JSONArray cameraList = JSONObject.parseArray(cameraListStr);
                return cameraList != null && cameraList.contains(cameraId);
            }).collect(Collectors.toList());
            if (ObjectUtil.isEmpty(devices)) {
                log.error("相机 {} 未绑定音柱或音柱未启用", cameraId);
                return;
            }

            JSONObject params = new JSONObject();
            SysParameter sysParameter = SysParameterUtil.getSysParameter("TaskName");
            if (ObjectUtil.isEmpty(sysParameter)) {
                log.error("没查询到音频播放任务名，请在系统参数中配置");
                return;
            }

            String taskName = sysParameter.getParamVal();
            if (StrUtil.isEmpty(taskName)) {
                log.error("音频播放任务名为空，请在系统参数中配置");
                return;
            }
            params.put("TaskName", taskName);
            params.put("PlayTimes", 1);
            params.put("Volume", 0);
            List<Integer> audioDevices = devices.stream().map(SoundDeviceInfo::getDeviceId).collect(Collectors.toList());
            params.put("DeviceIds", audioDevices);
            List<JSONObject> audioFilesInfos = audioFileInfoList.stream().map(audioFileInfo -> {
                JSONObject audioFileJson = new JSONObject();
                audioFileJson.put("FileId", audioFileInfo.getAudioId());
                audioFileJson.put("Seq", 0);
                return audioFileJson;
            }).collect(Collectors.toList());
            params.put("FileInfos", audioFilesInfos);

            String token = SosHttpApiUtil.getToken();
            if (token == null) return;
            String responseStr;
            try {
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("Token", token);
                responseStr = HttpClientUtil.requestPostJsonWithHeader(Constant.AUDIO_FILE_PLAY, params, headerMap);
            } catch (Exception e) {
                log.error("音柱请求播放失败：{}", e.getLocalizedMessage());
                return;
            }
            if (StrUtil.isNotEmpty(responseStr)) {
                JSONObject responseObj = JSONObject.parseObject(responseStr);
                if (responseObj.getInteger("Status") != 0)
                    log.error("播放失败：" + responseObj.getString("StatusMsg"));
            }

        }
    }
}
