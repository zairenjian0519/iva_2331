package org.jeecg.modules.face.service;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.face.entity.FaceDetailDo;
import org.jeecg.modules.face.entity.FaceQueryParams;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface FaceDetailService extends IService<FaceDetailDo> {

    FaceDetailDo getFaceDetail(String id);

    void addFaceDetail(FaceDetailDo faceDetailDo, MultipartFile file);

    IPage<FaceDetailDo> getByGroupId(FaceQueryParams faceQueryParams, int pageNo, int pageSize);

    void updateFaceDetail(FaceDetailDo faceDetailDo, MultipartFile file);

    void deleteImg(String id);

    void deleteFaceDetail(String id);

    void batchDeleteFaceDetail(String ids);

    void exportXls(Integer startNum, Integer exportNum, HttpServletResponse response);

    List<String> importExcel(String groupId, MultipartFile file);

    void importSingleFace(String alarmDetailId, FaceDetailDo faceDetailDo);

    void batchImportFace(String faceGroup, String ids);

    void batchImportFromRemote(JSONArray faceData);

    void clearAllData();

    void syncBaiduYun(String groupKey);
}
