package org.jeecg.modules.viewPreview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.viewPreview.entity.AlarmCountVo;
import org.jeecg.modules.viewPreview.entity.ViewPreview;

import java.util.List;

/**
 * @Description: 实时预览
 * @Author: jeecg-boot
 * @Date:   2022-08-16
 * @Version: V1.0
 */
public interface IViewPreviewService extends IService<ViewPreview> {

    List<ViewPreview> getSelectCameraList(List<String> ids, List<String> categorys);

    String getAlgorithmById(String cameraId);
    String getAllAlgorithmById(String cameraId);

    List<AlarmCountVo> getAlarmCountByCamera();

    boolean enableAlgStream();
}
