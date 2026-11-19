package org.jeecg.modules.wireless;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.jeecg.modules.wireless.entity.AudioFileInfo;
import org.jeecg.modules.wireless.entity.SoundDeviceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Sos api 工具类
 */
public class SosHttpApiUtil {

    private static final Logger log = LoggerFactory.getLogger(SosHttpApiUtil.class);

    /**
     * 获取token
     */
    public static String getToken() {
        // 先从redis中获取
        RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
        Object o = redisUtil.get(Constant.WIRELESS_PILLAR_TOKEN);
        if (ObjectUtil.isNotEmpty(o)) return (String) o;

        SoundPillarProperties soundPillarProperties = SpringContextUtils.getBean(SoundPillarProperties.class);
        if (StrUtil.hasEmpty(soundPillarProperties.getAppId(), soundPillarProperties.getAppCode())) {
            log.error("4g 音柱 appId appCode为空，请在配置文件中配置");
            return null;
        }

        // 没有获取到，从api获取
        JSONObject params = new JSONObject();

        params.put("AppId", soundPillarProperties.getAppId());
        SysParameter soundAppIdParam = SysParameterUtil.getSysParameter("SoundAppId");
        if (ObjectUtil.isNotEmpty(soundAppIdParam)) {
            String paramVal = soundAppIdParam.getParamVal();
            if (ObjectUtil.isNotEmpty(paramVal)) params.put("AppId", paramVal);
        }

        params.put("AppCode", soundPillarProperties.getAppCode());
        SysParameter soundAppCodeParam = SysParameterUtil.getSysParameter("SoundAppCode");
        if (ObjectUtil.isNotEmpty(soundAppCodeParam)) {
            String paramVal = soundAppCodeParam.getParamVal();
            if (ObjectUtil.isNotEmpty(paramVal)) params.put("AppCode", paramVal);
        }

        try {
            String responseStr = HttpClientUtil.requestPostJson(Constant.TOKEN_URL, params);
            log.info("4g 音柱 token 请求返回结果：{}", responseStr);
            JSONObject responseObj = JSONObject.parseObject(responseStr);
            int status = responseObj.getInteger("Status");
            if (status != 0) {
                log.error("4g 音柱 token 获取失败：{}", responseStr);
                return null;
            }
            String token = responseObj.getString("Token");
            long expire = responseObj.getLong("ExpireIn");
            // 存入redis 缓存
            redisUtil.set(Constant.WIRELESS_PILLAR_TOKEN, token, expire - 10);
            return token;
        } catch (Exception e) {
            log.error("4g 音柱token请求失败：{}", e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * 音频文件信息转实体类
     */
    public static AudioFileInfo convertAudioInfoToEntity(JSONObject audioJson) {
        AudioFileInfo audioFileInfo = new AudioFileInfo();
        audioFileInfo.setAudioId(audioJson.getInteger("Id"));
        audioFileInfo.setDisplayName(audioJson.getString("DisplayName"));
        audioFileInfo.setFileSize(audioJson.getString("FileSize"));
        audioFileInfo.setDuration(audioJson.getString("Duration"));
        audioFileInfo.setSampleRate(audioJson.getInteger("SampleRate"));
        audioFileInfo.setMd5(audioJson.getString("Md5"));
        audioFileInfo.setDescription(audioJson.getString("Description"));
        audioFileInfo.setAccessUrl(audioJson.getString("AccessUrl"));
        audioFileInfo.setIntranetUrl(audioJson.getString("IntranetUrl"));
        audioFileInfo.setRemark(audioJson.getString("Remark"));
        audioFileInfo.setUploadTime(audioJson.getDate("UploadTime"));
        audioFileInfo.setFileUuid(audioJson.getString("FileUuid"));
        audioFileInfo.setWirelessUrl(audioJson.getString("WirelessUrl"));
        return audioFileInfo;
    }

    /**
     * 构造音频播放请求参数
     */
    public static JSONObject buildAudioPlayParams(JSONObject params) {
        JSONObject result = new JSONObject();
        result.put("status", false);
        Integer audioId = params.getInteger("audioId");
        Integer soundDeviceId = params.getInteger("soundDeviceId");
        log.info("音频id：{}, 音柱id：{}", audioId, soundDeviceId);
        if (ObjectUtil.hasEmpty(audioId, soundDeviceId)) {
            result.put("msg", "音频或音柱id为空");
            return result;
        }
        JSONObject data = new JSONObject();
        SysParameter sysParameter = SysParameterUtil.getSysParameter("TaskName");
        if (ObjectUtil.isEmpty(sysParameter)) {
            result.put("msg", "没查询到音频播放任务名，请在系统参数中配置");
            return result;
        }
        String taskName = sysParameter.getParamVal();
        data.put("TaskName", taskName);
        data.put("PlayTimes", 1);
        data.put("Volume", 0);
        data.put("DeviceIds", ListUtil.toList(soundDeviceId));
        JSONObject audioFileInfo = new JSONObject();
        audioFileInfo.put("FileId", audioId);
        audioFileInfo.put("Seq", 0);
        data.put("FileInfos", ListUtil.toList(audioFileInfo));
        result.put("data", data);
        result.put("status", true);
        return result;
    }

    /**
     * json convert entity
     */
    public static SoundDeviceInfo convertDeviceInfoToEntity(JSONObject jsonObject) {
        SoundDeviceInfo soundDeviceInfo = new SoundDeviceInfo();
        soundDeviceInfo.setDeviceId(jsonObject.getInteger("Id"));
        soundDeviceInfo.setName(jsonObject.getString("Name"));
        soundDeviceInfo.setSerialNum(jsonObject.getString("SerialNum"));
        soundDeviceInfo.setLicenseId(jsonObject.getString("LicenseId"));
        soundDeviceInfo.setAppState(jsonObject.getString("AppState"));
        soundDeviceInfo.setSignalStrength(jsonObject.getString("SignalStrength"));
        soundDeviceInfo.setCity(jsonObject.getString("City"));
        soundDeviceInfo.setDistrict(jsonObject.getString("District"));
        soundDeviceInfo.setTown(jsonObject.getString("Town"));
        soundDeviceInfo.setProvince(jsonObject.getString("Province"));
        soundDeviceInfo.setOnlineTime(jsonObject.getDate("OnlineTime"));
        soundDeviceInfo.setUpdateTime(new Date());
        return soundDeviceInfo;
    }

    /**
     * 拼接请求音柱列表信息的参数
     */
    public static JSONObject buildGetDeviceListParams(List<String> deviceIds) {
        JSONObject root = new JSONObject();
        // 设置 PageSize, PageNum, Desc
        root.put("PageSize", 30);
        root.put("PageNum", 1);
        root.put("Desc", 1);
        // 创建 Filters 数组
        JSONArray filtersArray = new JSONArray();
        for (String deviceId : deviceIds) {
            JSONArray filterGroup = new JSONArray();
            // 创建第一个 Filter
            JSONObject filter = new JSONObject();
            filter.put("key", "Id");
            filter.put("Type", "str");
            filter.put("Value", deviceId);
            filter.put("Op", "=");
            filterGroup.add(filter);
            filtersArray.add(filterGroup);
        }
        // 将 Filters 数组添加到根对象
        root.put("Filters", filtersArray);
        return root;
    }
}