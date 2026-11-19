package org.jeecg.modules.viewPreview.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.viewPreview.entity.AlarmCountVo;
import org.jeecg.modules.viewPreview.entity.ViewPreview;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 实时预览
 * @Author: jeecg-boot
 * @Date:   2022-08-16
 * @Version: V1.0
 */
public interface ViewPreviewMapper extends BaseMapper<ViewPreview> {

    List<ViewPreview> selectCameraList(List<String> ids, List<String> categorys);

    String getAlgorithm(@Param("cameraId") String cameraId);
    String getAllAlgorithm(@Param("cameraId") String cameraId);

    List<AlarmCountVo> getAlarmCountByCamera();
}
