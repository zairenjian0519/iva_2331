/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.baseImage.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.baseImage.entity.BasicImageDo;
import org.jeecg.modules.baseImage.mapper.BasicImageMapper;
import org.jeecg.modules.baseImage.service.IBasicImageService;
import org.jeecg.modules.qwenVL.Properties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author： DecoderX
 * @date： 2025/8/12
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BasicImageServiceImpl extends ServiceImpl<BasicImageMapper, BasicImageDo> implements IBasicImageService {

    private final Properties properties;

    @Override
    public Result<?> upload(MultipartFile file) {
        if (file == null) {
            return Result.error("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            return Result.error("文件名为空");
        }
        String fileExt = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = IdUtil.fastSimpleUUID() + fileExt;
        BasicImageDo basicImageDo = new BasicImageDo();
        basicImageDo.setFileName(originalFilename);
        basicImageDo.setReName(newFileName);
        basicImageDo.setFileType(fileExt);
        basicImageDo.setFileSize((double) file.getSize());
        if (StrUtil.isBlank(properties.getTemplatePath())) {
            return Result.error("请配置上传文件路径");
        }
        basicImageDo.setFilePath(properties.getTemplatePath() + newFileName);

        try {
            FileUtil.writeBytes(file.getBytes(), basicImageDo.getFilePath());
        } catch (IOException e) {
            log.error("上传文件失败: {}", e.getMessage());
            return Result.error("上传文件失败");
        }
        this.save(basicImageDo);
        return Result.ok("上传成功");
    }

    @Override
    public IPage<BasicImageDo> pageList(IPage<BasicImageDo> page, BasicImageDo basicImageDo) {
        IPage<BasicImageDo> imageDoIPage = this.page(page);
        for (BasicImageDo v : imageDoIPage.getRecords()) {
            v.setThumbnail("/sourceImage/" + v.getReName());
        }
        return imageDoIPage;
    }

    @Override
    public Result<?> deleteById(String id) {
        BasicImageDo basicImageDo = this.getById(id);
        if (basicImageDo != null) {
            this.removeById(id);
            FileUtil.del(basicImageDo.getFilePath());
            return Result.ok("删除成功");
        }
        return Result.error("删除失败");
    }

    @Override
    public List<BasicImageDo> getList() {
        List<BasicImageDo> list = this.list();
        for (BasicImageDo v : list) {
            v.setThumbnail("/sourceImage/" + v.getReName());
        }
        return list;
    }

    @Override
    public BasicImageDo getDataById(String id) {
        BasicImageDo basicImageDo = this.getById(id);
        basicImageDo.setThumbnail("/sourceImage/" + basicImageDo.getReName());
        return basicImageDo;
    }
}
