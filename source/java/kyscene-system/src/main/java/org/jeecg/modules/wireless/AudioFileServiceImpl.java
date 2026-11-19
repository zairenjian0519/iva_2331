package org.jeecg.modules.wireless;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.jeecg.modules.wireless.entity.AudioFileInfo;
import org.jeecg.modules.wireless.mapper.AudioFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AudioFileServiceImpl extends ServiceImpl<AudioFileMapper, AudioFileInfo> implements AudioFileService {
    @Override
    public Result<IPage<AudioFileInfo>> listAudioFiles(Integer pageNo, Integer pageSize) {
        Result<IPage<AudioFileInfo>> result = new Result<>();
        Page<AudioFileInfo> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<AudioFileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(AudioFileInfo::getUploadTime);
        Page<AudioFileInfo> deviceInfoPage = this.page(page, queryWrapper);
        result.setSuccess(true);
        result.setResult(deviceInfoPage);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void syncAudioFiles() {
        String token = SosHttpApiUtil.getToken();
        if (token == null) throw new JeecgBootException("获取token失败");
        try {
            JSONObject params = new JSONObject();
            params.put("PageSize", 100);
            params.put("PageNum", 1);
            params.put("Desc", 1);
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("Token", token);
            String responseStr = HttpClientUtil.requestPostJsonWithHeader(Constant.AUDIO_FILE_LIST_URL, params, headerMap);
            if (StrUtil.isEmpty(responseStr)) throw new JeecgBootException("音频文件请求返回为空");
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            Integer status = responseObj.getInteger("Status");
            String statusMsg = responseObj.getString("StatusMsg");
            if (status != 0) throw new JeecgBootException(statusMsg);
            JSONArray jsonArray = responseObj.getJSONArray("RecordList");
            if (ObjectUtil.isEmpty(jsonArray)) throw new JeecgBootException("音频文件为空");
            List<AudioFileInfo> targetList = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                AudioFileInfo audioFileInfo = SosHttpApiUtil.convertAudioInfoToEntity(jsonArray.getJSONObject(i));
                targetList.add(audioFileInfo);
            }
            // 同步数据库配置
            List<AudioFileInfo> audioFileInfoListDb = this.list();
            List<AudioFileInfo> collect = targetList.stream().peek(audioFileInfo -> audioFileInfoListDb.stream()
                    .filter(audioFileInfo1 -> audioFileInfo1.getAudioId().equals(audioFileInfo.getAudioId()))
                    .findFirst()
                    .ifPresent(audioFileInfo1 -> {
                        audioFileInfo.setStatus(audioFileInfo1.getStatus());
                        audioFileInfo.setAlgorithm(audioFileInfo1.getAlgorithm());
                    })).collect(Collectors.toList());
            // 先清空数据库
            this.baseMapper.truncateAudioInfo();
            // 保存
            this.saveBatch(collect);

        } catch (Exception e) {
            log.error("获取音频文件列表异常：{}", e.getLocalizedMessage());
        }
    }

    @Override
    public void updateVoiceAlgorithm(AudioFileInfo audioFileInfo) {
        if (audioFileInfo.getAudioId() == null) throw new JeecgBootException("音频文件id不能为空");
        LambdaQueryWrapper<AudioFileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AudioFileInfo::getAudioId, audioFileInfo.getAudioId());
        AudioFileInfo audioInfo = this.getOne(queryWrapper, false);
        if (ObjectUtil.isEmpty(audioInfo)) throw new JeecgBootException("未查询到该条记录");

        // 根据算法去查询，一种算法智能绑定一个音频
        if (audioFileInfo.getAlgorithm() != null) {
            LambdaQueryWrapper<AudioFileInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(AudioFileInfo::getAlgorithm, audioFileInfo.getAlgorithm());
            lambdaQueryWrapper.eq(AudioFileInfo::getStatus, 1);
            lambdaQueryWrapper.ne(AudioFileInfo::getAudioId, audioFileInfo.getAudioId());
            List<AudioFileInfo> audioFileInfoList = this.list(lambdaQueryWrapper);
            if (ObjectUtil.isNotEmpty(audioFileInfoList)) throw new JeecgBootException("每种算法只能绑定一种音频，请禁用其他音频");
            audioInfo.setAlgorithm(audioFileInfo.getAlgorithm());
        }
        if (audioFileInfo.getStatus() != null) audioInfo.setStatus(audioFileInfo.getStatus());
        this.updateById(audioInfo);
    }

    @Override
    public List<AudioFileInfo> getAvailableAudioFiles() {
        LambdaQueryWrapper<AudioFileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AudioFileInfo::getStatus, 1);
        return list(queryWrapper);
    }
}
