package org.jeecg.modules.face.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.face.entity.FaceRemoteUpdateMsg;

/**
 * @author： DecoderX
 * @date： 2025/5/21
 */
public interface FaceRemoteUpdateMsgService extends IService<FaceRemoteUpdateMsg> {

    void saveData(FaceRemoteUpdateMsg faceRemoteUpdateMsg);

    //  获取最近一条数据
    FaceRemoteUpdateMsg getRecentData();
}
