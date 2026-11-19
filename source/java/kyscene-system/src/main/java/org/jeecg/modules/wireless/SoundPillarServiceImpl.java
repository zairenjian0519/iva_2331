package org.jeecg.modules.wireless;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
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
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.jeecg.modules.wireless.entity.SoundDeviceInfo;
import org.jeecg.modules.wireless.mapper.SoundDeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 4G音柱服务类
 */
@Service
@Slf4j
public class SoundPillarServiceImpl extends ServiceImpl<SoundDeviceMapper, SoundDeviceInfo> implements SoundPillarService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SoundPillarProperties soundPillarProperties;

    @Override
    public void restartDevice() {

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateSoundDeviceInfo(List<SoundDeviceInfo> listInfos) {
        List<SoundDeviceInfo> soundDeviceInfos = this.list();
        List<SoundDeviceInfo> collect = listInfos.stream().peek(soundDeviceInfo -> {
            soundDeviceInfo.setUpdateTime(new Date());
            soundDeviceInfos.stream()
                    .filter(soundDeviceInfo1 -> soundDeviceInfo1.getDeviceId().equals(soundDeviceInfo.getDeviceId()))
                    .findFirst()
                    .ifPresent(soundDeviceInfo1 -> {
                        soundDeviceInfo.setId(soundDeviceInfo1.getId());
                        soundDeviceInfo.setEnable(soundDeviceInfo1.getEnable());
                        soundDeviceInfo.setVolume(soundDeviceInfo1.getVolume());
                    });
        }).collect(Collectors.toList());
        this.baseMapper.truncateDeviceInfo();
        this.saveBatch(collect);
    }

    @Override
    public Result<IPage<SoundDeviceInfo>> getDeviceList(Integer pageNo, Integer pageSize) {
        Result<IPage<SoundDeviceInfo>> result = new Result<>();
        Page<SoundDeviceInfo> page = new Page<>(pageNo, pageSize);
        Page<SoundDeviceInfo> deviceInfoPage = this.page(page);
        result.setSuccess(true);
        result.setResult(deviceInfoPage);
        return result;
    }

    @Override
    public void updateStatus(Integer status, String id) {
        SoundDeviceInfo soundDeviceInfo = new SoundDeviceInfo();
        soundDeviceInfo.setId(id);
        soundDeviceInfo.setEnable(status);
        soundDeviceInfo.setUpdateTime(new Date());
        this.updateById(soundDeviceInfo);
    }

    @Override
    public List<KyCategory> getAlgorithmList() {
        LambdaQueryWrapper<KyCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KyCategory::getCategory, 0);
        queryWrapper.eq(KyCategory::getHasChild, 0);
        IKyCategoryService kyCategoryService = SpringContextUtils.getBean(IKyCategoryService.class);
        return kyCategoryService.list(queryWrapper);
    }

    @Override
    public String playAudioFile(JSONObject params) {
        String token = SosHttpApiUtil.getToken();
        if (token == null) throw new JeecgBootException("获取token失败");
        JSONObject playParams = SosHttpApiUtil.buildAudioPlayParams(params);
        if (!playParams.getBoolean("status")) throw new JeecgBootException(playParams.getString("msg"));
        String responseStr;
        try {
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("Token", token);
            responseStr = HttpClientUtil.requestPostJsonWithHeader(Constant.AUDIO_FILE_PLAY, playParams.get("data"), headerMap);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new JeecgBootException("请求播放失败");
        }
        if (StrUtil.isNotEmpty(responseStr)) {
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            if (responseObj.getInteger("Status") != 0)
                throw new JeecgBootException("播放失败：" + responseObj.getString("StatusMsg"));
            return responseObj.getString("TaskCode");
        }
        return null;
    }

    @Override
    public void updateVolume(Integer deviceId, Integer volume) {
        SoundDeviceInfo soundDeviceInfo = new SoundDeviceInfo();
        LambdaQueryWrapper<SoundDeviceInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SoundDeviceInfo::getDeviceId, deviceId);
        SoundDeviceInfo deviceInfo = getOne(queryWrapper, false);
        if (deviceInfo == null) throw new JeecgBootException("没有找到相关设备，保存数据失败");
        soundDeviceInfo.setId(deviceInfo.getId());
        soundDeviceInfo.setVolume(volume);
        updateById(soundDeviceInfo);
    }

    @Override
    public void closePlayTask(String taskCode) {
        String token = SosHttpApiUtil.getToken();
        if (token == null) throw new JeecgBootException("获取token失败");
        JSONObject params = new JSONObject();
        params.put("TaskCode", taskCode);
        String responseStr;
        try {
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("Token", token);
            responseStr = HttpClientUtil.requestPostJsonWithHeader(Constant.AUDIO_FILE_STOP, params, headerMap);
        } catch (Exception e) {
            log.error("关闭音频请求失败：{}", e.getLocalizedMessage());
            throw new JeecgBootException("请求失败");
        }
        if (StrUtil.isNotEmpty(responseStr)) {
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            if (responseObj.getInteger("Status") != 0) {
                throw new JeecgBootException(responseObj.getString("StatusMsg"));
            }
        }
    }

    @Override
    public JSONObject getSoundPillarAccountInfo() {
        JSONObject json = new JSONObject();

        if (ObjectUtil.hasEmpty(soundPillarProperties.getDefaultAccount(), soundPillarProperties.getDefaultPassword()))
            throw new JeecgBootException("未在系统中配置该设备绑定的音柱账号密码");
        json.put("AudioAccount", soundPillarProperties.getDefaultAccount());
        json.put("AudioPassword", soundPillarProperties.getDefaultPassword());
        return json;
    }

    @Override
    public JSONObject getProjectInfo() {
        JSONObject json = new JSONObject();
        SysParameter sysParameter = SysParameterUtil.getSysParameter("SoundPillarDir");
        if (ObjectUtil.isEmpty(sysParameter))
            throw new JeecgBootException("未在系统设置中配置音柱所属项目");
        json.put("SoundPillarDir", sysParameter.getParamVal());
        return json;
    }

    @Override
    public boolean isEnableSoundPillar() {
        SysParameter parameter = SysParameterUtil.getSysParameter("SoundPillarEnable");
        if (parameter == null) {
            log.error("未在系统中配置是否启用音柱告警参数");
            return false;
        }
        String paramVal = parameter.getParamVal();
        JSONObject jsonObject = JSONObject.parseObject(paramVal);
        return jsonObject.getBoolean("enable");
    }

    @Override
    public void truncateDeviceInfo() {
        this.baseMapper.truncateDeviceInfo();
    }

    @Override
    public void syncDevices(String soundPillarDir) {
        // 如果参数为空
        if (StrUtil.isBlank(soundPillarDir)) {
            // 查询当前盒子配置的音柱分组名称
            SysParameter sysParameter = SysParameterUtil.getSysParameter("SoundPillarDir");
            if (sysParameter == null) {
                log.info("没查询到音柱分组名称，请在系统参数中配置 SoundPillarDir");
                return;
            } else if (StrUtil.isBlank(sysParameter.getParamVal())) {
                log.info("音柱分组名称为空，请在系统参数中配置 SoundPillarDir 的参数值");
                return;
            }
            soundPillarDir = sysParameter.getParamVal();
        }

        // 将soundPillarDir写入到系统参数中
        SysParameter sysParameter = new SysParameter();
        sysParameter.setParamCode("SoundPillarDir");
        sysParameter.setParamName("音柱所在目录");
        sysParameter.setDescription("音柱所在目录");
        sysParameter.setParamVal(soundPillarDir);
        sysParameter.setDelFlag(0);
        SysParameterUtil.addOrUpdateSysParameter(sysParameter);

        String token = SosHttpApiUtil.getToken();
        if (StrUtil.isBlank(token)) {
            log.error("获取sos token 失败");
            return;
        }
        JSONObject params = new JSONObject();
        params.put("TreeType", "all");
        String responseStr;
        try {
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("Token", token);
            responseStr = HttpClientUtil.requestPostJsonWithHeader(Constant.SOUND_PILLAR_LIST_TREE, params, headerMap);
        } catch (Exception e) {
            log.error("请求设备树接口失败：{}", e.getMessage());
            return;
        }
        JSONObject responseObj = JSONObject.parseObject(responseStr);
        if (responseObj.getInteger("Status") != 0) {
            log.error("获取设备树返回状态异常：{}, Message: {}", responseObj.getInteger("Status"), responseObj.getString("StatusMsg"));
            return;
        }

        //从设备树中解析设备信息
        JSONObject tree = responseObj.getJSONObject("Tree");
        if (ObjectUtil.isEmpty(tree)) {
            log.info("设备树Tree节点列表为空");
            return;
        }

        JSONArray children = tree.getJSONArray("Children");
        if (ObjectUtil.isEmpty(children)) {
            log.info("Tree节点下Children为空");
            return;
        }
        processTree(children, soundPillarDir);
    }

    public void processTree(JSONArray nodes, String targetName) {
        if (ObjectUtil.isEmpty(nodes)) {
            return;
        }

        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            String name = node.getString("Name");

            if (targetName.equals(name)) {
                JSONArray children = node.getJSONArray("Children");
                if (ObjectUtil.isNotEmpty(children)) {
                    handleDeviceInfo(children);
                }
                return;
            }

            // 递归处理子节点
            JSONArray children = node.getJSONArray("Children");
            if (ObjectUtil.isNotEmpty(children)) {
                processTree(children, targetName);
            }
        }
    }

    // 处理设备数组
    private void handleDeviceInfo(JSONArray deviceInfoJsonArray) {
        List<String> deviceIds = new ArrayList<>();
        for (int i = 0; i < deviceInfoJsonArray.size(); i++) {
            JSONObject deviceInfoJson = deviceInfoJsonArray.getJSONObject(i);
            JSONObject deviceInfo = deviceInfoJson.getJSONObject("DeviceInfo");
            if (ObjectUtil.isEmpty(deviceInfo)) continue;
            // 获取设备id，拼接为请求参数 二维数组。
            String deviceId = deviceInfo.getString("Id");
            deviceIds.add(deviceId);
        }
        if (ObjectUtil.isEmpty(deviceIds)) {
            log.info("没有获取到任何音柱设备id, deviceIds 为空");
            return;
        }
        // 请求设备详细信息列表
        JSONObject params = SosHttpApiUtil.buildGetDeviceListParams(deviceIds);
        try {
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("Token", SosHttpApiUtil.getToken());
            String responseStr = HttpClientUtil.requestPostJsonWithHeader(Constant.SOUND_PILLAR_LIST_URL, params, headerMap);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            Integer status = responseObj.getInteger("Status");
            if (status != 0) {
                log.error("获取音柱设备列表异常: {}", responseObj.getString("StatusMsg"));
                return;
            }
            JSONArray recordList = responseObj.getJSONArray("RecordList");
            if (ObjectUtil.isEmpty(recordList)) {
                log.info("请求音柱设备列表的返回结果为空");
                return;
            }

            List<SoundDeviceInfo> devicesFromDb = this.list();

            // 构建 deviceId -> DeviceInfo map 提高查询效率
            Map<Integer, SoundDeviceInfo> deviceMap = devicesFromDb.stream()
                    .collect(Collectors.toMap(SoundDeviceInfo::getDeviceId, Function.identity()));

            List<SoundDeviceInfo> soundDeviceInfoList = new ArrayList<>();
            for (int i = 0; i < recordList.size(); i++) {
                JSONObject deviceInfo = recordList.getJSONObject(i);
                SoundDeviceInfo soundDeviceInfo = SosHttpApiUtil.convertDeviceInfoToEntity(deviceInfo);

                Integer deviceId = soundDeviceInfo.getDeviceId();
                if (deviceId == null) continue;

                SoundDeviceInfo dbDeviceInfo = deviceMap.get(deviceId);
                if (dbDeviceInfo != null) {
                    BeanUtil.copyProperties(soundDeviceInfo, dbDeviceInfo, CopyOptions.create().ignoreNullValue());
                    soundDeviceInfoList.add(dbDeviceInfo);
                } else {
                    soundDeviceInfoList.add(soundDeviceInfo);
                }
            }
            // 清空记录, 再保存，避免重复
            this.baseMapper.truncateDeviceInfo();
            // 存入数据库
            this.saveBatch(soundDeviceInfoList);
        } catch (Exception e) {
            log.error("请求音柱设备列表失败：{}", e.getMessage());
        }
    }
}