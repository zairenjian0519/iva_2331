package org.jeecg.modules.dataPush;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.service.ISysParameterService;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DataPushServiceImpl implements IDataPushService {

    @Autowired
    private ISysParameterService sysParameterService;

    @Override
    public void saveSetting(JSONObject params) {
        if (null == params) throw new JeecgBootException("参数不能为空");
        LambdaQueryWrapper<SysParameter> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysParameter::getParamCode, Constant.DATA_PUSH_CODE);
        SysParameter sysParameter = sysParameterService.getOne(lambdaQueryWrapper, false);
        SysParameter sysParameterDo = new SysParameter();
        sysParameterDo.setParamName(Constant.PARAM_NAME);
        sysParameterDo.setParamCode(Constant.DATA_PUSH_CODE);
        sysParameterDo.setDescription(Constant.PARAM_DESCRIPTION);
        sysParameterDo.setCreateTime(new Date());
        sysParameterDo.setDelFlag(0);
        sysParameterDo.setParamVal(params.toJSONString());
        if (null == sysParameter) {
            sysParameterService.save(sysParameterDo);
            SysParameterUtil.cacheSystemParams();
        } else {
            sysParameterDo.setId(sysParameter.getId());
            sysParameterService.updateById(sysParameterDo);
            SysParameterUtil.cacheSystemParams();
        }
    }

    @Override
    public DataPushVo getSetting() {
        LambdaQueryWrapper<SysParameter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysParameter::getParamCode, Constant.DATA_PUSH_CODE);
        SysParameter sysParameter = sysParameterService.getOne(queryWrapper, false);
        if (null == sysParameter) return null;
        if (null == sysParameter.getParamVal()) return null;
        return JSONObject.parseObject(sysParameter.getParamVal(), DataPushVo.class);
    }

    @Override
    public void saveStreamSetting(StreamBizInfo streamBizInfo) {
        String ip = streamBizInfo.getBizIp();
        Integer port = streamBizInfo.getBizPort();
        if (ObjectUtil.hasEmpty(ip, port)) throw new JeecgBootException("业务IP和端口不能为空");
        LambdaQueryWrapper<SysParameter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysParameter::getParamCode, Constant.STREAM_BIZ_ADDRESS);
        SysParameter sysParameter = sysParameterService.getOne(queryWrapper, false);
        if (ObjectUtil.isEmpty(sysParameter)) {
            sysParameter = new SysParameter();
            sysParameter.setCreateTime(new Date());
            sysParameter.setParamName("视频流业务地址");
            sysParameter.setDescription("视频流业务地址配置，配置影响 webrtc 返回地址");
            sysParameter.setParamCode(Constant.STREAM_BIZ_ADDRESS);
            sysParameter.setDelFlag(0);
            sysParameter.setParamVal(JSON.toJSONString(streamBizInfo));
            sysParameterService.save(sysParameter);
            return;
        }
        sysParameter.setParamVal(JSON.toJSONString(streamBizInfo));
        sysParameter.setCreateTime(new Date());
        sysParameterService.updateById(sysParameter);
    }

    @Override
    public StreamBizInfo getStreamSetting() {
        LambdaQueryWrapper<SysParameter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysParameter::getParamCode, Constant.STREAM_BIZ_ADDRESS);
        SysParameter sysParameter = sysParameterService.getOne(queryWrapper, false);
        if (ObjectUtil.isEmpty(sysParameter)) return null;
        if (ObjectUtil.isEmpty(sysParameter.getParamVal())) return null;
        return JSONObject.parseObject(sysParameter.getParamVal(), StreamBizInfo.class);
    }
}
