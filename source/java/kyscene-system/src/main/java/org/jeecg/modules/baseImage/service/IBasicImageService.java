package org.jeecg.modules.baseImage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.baseImage.entity.BasicImageDo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/8/12
 */
public interface IBasicImageService extends IService<BasicImageDo> {

    Result<?> upload(MultipartFile file);

    IPage<BasicImageDo> pageList(IPage<BasicImageDo> page, BasicImageDo basicImageDo);

    Result<?> deleteById(String id);

    List<BasicImageDo> getList();

    BasicImageDo getDataById(String id);
}
