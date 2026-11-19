package org.jeecg.modules.deviceManagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.deviceManagement.entity.SoftUpdateDo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/5/27
 */
public interface ISoftUpdateService extends IService<SoftUpdateDo> {

    /**
     * 上传文件
     */
    Result<?> uploadUpdateFile(MultipartFile file, SoftUpdateDo softUpdateDo);

    /**
     * 分页查询, 带搜索
     * @param softUpdateDo SoftUpdateDo
     * @param pageNo
     * @param pageSize
     * @return IPage<SoftUpdateDo>
     */
    IPage<SoftUpdateDo> getUpdatePage(SoftUpdateDo softUpdateDo, Integer pageNo, Integer pageSize);

    /**
     * 删除版本
     * @param id String
     */
     Result<?> deleteUploadSoft(String id);

    /**
     * 发布版本
     * @param id String
     */
     Result<?> publishSoft(String id);

    /**
     * 升级撤回
     */
     Result<?> withdrawSoft(String softType);

     /**
     * 获取所有版本，前端，后端，算法
     */
     Result<?> getAllSoftVersion();
}
