package org.jeecg.modules.viewPreview.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.Constant;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.CameraStreamHandler;
import org.jeecg.modules.viewPreview.entity.AlarmCountVo;
import org.jeecg.modules.viewPreview.entity.ViewPreview;
import org.jeecg.modules.viewPreview.mapper.ViewPreviewMapper;
import org.jeecg.modules.viewPreview.service.IViewPreviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 实时预览
 * @Author: jeecg-boot
 * @Date: 2022-08-16
 * @Version: V1.0
 */
@Service
public class ViewPreviewServiceImpl extends ServiceImpl<ViewPreviewMapper, ViewPreview> implements IViewPreviewService {

    @Autowired
    ViewPreviewMapper viewPreviewMapper;

    @Autowired
    private CameraStreamHandler cameraStreamHandler;

    @Override
    public List<ViewPreview> getSelectCameraList(List<String> ids, List<String> categorys) {
        return viewPreviewMapper.selectCameraList(ids, categorys);
    }

    @Override
    public String getAlgorithmById(String cameraId) {
        return viewPreviewMapper.getAlgorithm(cameraId);
    }

    @Override
    public String getAllAlgorithmById(String cameraId) {
        return viewPreviewMapper.getAllAlgorithm(cameraId);
    }

    @Override
    public List<AlarmCountVo> getAlarmCountByCamera() {
        return viewPreviewMapper.getAlarmCountByCamera();
    }

    @Override
    public boolean enableAlgStream() {
        SysParameter sysParameter = SysParameterUtil.getSysParameter(Constant.ALG_STREAM_PAGE_SYS_CODE);
        if (null == sysParameter) return false;
        String paramVal = sysParameter.getParamVal();
        if (StrUtil.isBlank(paramVal)) return false;
        return Constant.ALG_STREAM_PAGE_SYS_FLAG.equals(paramVal);
    }
}
