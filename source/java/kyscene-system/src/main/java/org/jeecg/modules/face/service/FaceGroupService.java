package org.jeecg.modules.face.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.face.entity.FaceGroupDo;

import java.util.List;

public interface FaceGroupService extends IService<FaceGroupDo> {

    void addFaceGroup(FaceGroupDo faceGroupDo);

    void updateFaceGroup(FaceGroupDo faceGroupDo);

    Result<?> deleteFaceGroup(String id);

    List<FaceGroupDo> getList();

    void initFaceGroup(String faceGroupId);

    void initDefaultGroup();

    FaceGroupDo getOrCreateDefaultGroup();

    Result<?> initGroup();
}
