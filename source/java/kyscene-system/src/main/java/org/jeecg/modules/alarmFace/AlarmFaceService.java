package org.jeecg.modules.alarmFace;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author： DecoderX
 * @date： 2025/4/8
 */
public interface AlarmFaceService extends IService<AlarmFaceDo> {

    void saveAlarmFace(AlarmFaceDo alarmFaceDo);

    IPage<AlarmFaceDo> getPageData(Integer pageNo, Integer pageSize, AlarmFaceParams alarmFaceParams);
}
