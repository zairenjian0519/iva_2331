package org.jeecg.modules.algAdapter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.algAdapter.entity.MainFileInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author： DecoderX
 * @date： 2025/7/15
 */
public interface IMainFileService extends IService<MainFileInfo> {

    IPage<MainFileInfo> getPageData(int pageNo, int pageSize);

    Result<?> upload(String type, MultipartFile file);

    Result<?> deleteFileById(String id);

    Result<?> applyFileById(String id);
}
