/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.algAdapter.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.algAdapter.entity.MainFileInfo;
import org.jeecg.modules.algAdapter.mapper.MainFileMapper;
import org.jeecg.modules.algAdapter.properties.ConfigProperties;
import org.jeecg.modules.algAdapter.service.IMainFileService;
import org.jeecg.modules.system.model.CommandResult;
import org.jeecg.modules.system.util.CmdUti;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author： DecoderX
 * @date： 2025/7/15
 */
@Slf4j
@Service
public class MainFileServiceImpl extends ServiceImpl<MainFileMapper, MainFileInfo> implements IMainFileService {

    @Autowired
    private ConfigProperties uploadUpdateProperties;

    @Override
    public IPage<MainFileInfo> getPageData(int pageNo, int pageSize) {
        return this.page(new Page<>(pageNo, pageSize));
    }

    @Override
    public Result<?> upload(String type, MultipartFile file) {

        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 参数校验
        if (!StrUtil.containsAny(type, "model", "main")) {
            return Result.error("上传文件的类型错误");
        }
        if ("model".equalsIgnoreCase(type) && StrUtil.isBlank(uploadUpdateProperties.getModelUploadPath())) {
            return Result.error("请配置上传模型文件路径");
        }
        if ("main".equalsIgnoreCase(type) && StrUtil.isBlank(uploadUpdateProperties.getMainUploadPath())) {
            return Result.error("请配置上传主文件路径");
        }

        String originalFilename = file.getOriginalFilename();
        // 支持 tar/tar.gz/zip 格式的文件
        List<String> fileExits = List.of("tar", "tar.gz", "zip");
        if (!fileExits.contains(FileUtil.extName(originalFilename))) {
            return Result.error("只允许上传.tar/.tar.gz/.zip格式的文件");
        }

        // 判断文件类型，同种类型不能超过5条记录
        long count = this.count(Wrappers.lambdaQuery(MainFileInfo.class)
                .eq(MainFileInfo::getFileType, type));
        if (count >= 5) {
            return Result.error("同种类型最多保留5条升级记录，请删除旧记录");
        }

        // 重命名文件
        String fileExt = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = IdUtil.fastSimpleUUID() + fileExt;
        String basePath = type.equalsIgnoreCase("model") ? uploadUpdateProperties.getModelUploadPath() : uploadUpdateProperties.getMainUploadPath();
        String fileUploadPath = basePath + newFileName;
        String absolutePath;
        try {
            File file1 = FileUtil.writeBytes(file.getBytes(), fileUploadPath);
            absolutePath = file1.getAbsolutePath();
            log.info("上传文件成功，文件路径：{}", absolutePath);
        } catch (IOException e) {
            log.error("上传文件失败", e);
            return Result.error("上传文件失败");
        }

        long fileSize = file.getSize();
        MainFileInfo mainFileInfo = new MainFileInfo();
        mainFileInfo.setFileNewName(newFileName);
        mainFileInfo.setFileOriginalName(originalFilename);
        mainFileInfo.setFileExt(fileExt);
        mainFileInfo.setFileSize((double) fileSize);
        mainFileInfo.setStatus(0);
        mainFileInfo.setFilePath(absolutePath);
        mainFileInfo.setFileType(type);
        this.save(mainFileInfo);
        log.info("上传文件成功，文件名：{}，文件大小：{}，文件路径：{}", originalFilename, fileSize, fileUploadPath);
        Map<String, String> map = new HashMap<>();
        map.put("url", "/algAdapter/" + newFileName);
        return Result.OK(map);
    }

    @Override
    public Result<?> deleteFileById(String id) {
        MainFileInfo mainFileInfo = this.getById(id);
        if (mainFileInfo == null) {
            return Result.error("文件不存在");
        }
        // 正在应用状态的文件不能删除
        if (mainFileInfo.getStatus() == 1 && "main".equals(mainFileInfo.getFileType())) {
            return Result.error("正在应用状态的文件不能删除");
        }

        // 删除文件
        String filePath = StrUtil.blankToDefault(mainFileInfo.getFilePath(), "").trim();
        String workPath = StrUtil.blankToDefault(mainFileInfo.getWorkPath(), "").trim();
        String workPathBak = workPath.isEmpty() ? "" : (workPath + ".bak");
        log.info("删除文件：filePath = {}, workPath = {}, workPathBak = {}", filePath, workPath, workPathBak);

        List<String> cmdList = new ArrayList<>();
        cmdList.add("rm");
        cmdList.add("-rf");
        if (!filePath.isEmpty()) cmdList.add(filePath);
        if (!workPath.isEmpty()) cmdList.add(workPath);
        if (!workPathBak.isEmpty()) cmdList.add(workPathBak);
        try {
            String[] cmd = cmdList.toArray(new String[0]);
            CommandResult commandResult = CmdUti.executeCmd2(cmd);
            if (commandResult.getExitCode() != 0) {
                log.error("删除文件失败：{}", commandResult.getOutput());
                return Result.error("删除文件失败");
            }
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return Result.error("删除文件失败");
        }

        log.info("删除文件成功");

        // 删除数据库记录
        this.removeById(id);
        return Result.OK("删除成功");
    }

    @Override
    public Result<?> applyFileById(String id) {
        MainFileInfo mainFileInfo = this.getById(id);
        if (mainFileInfo == null) {
            return Result.error("文件不存在");
        }
        String type = mainFileInfo.getFileType();
        String execPath = StrUtil.equals("model", type)? uploadUpdateProperties.getModelExecutePath() : uploadUpdateProperties.getMainExecutePath();
        if (StrUtil.equals("main", type) && StrUtil.isBlank(execPath)) {
            return Result.error("请配置主文件工作路径");
        }
        if (StrUtil.equals("model", type) && StrUtil.isBlank(execPath)) {
            return Result.error("请配置模型文件工作路径");
        }
        try {
            CommandResult commandResult = CmdUti.execByShell("/home/super/alg_adapter/scripts/apply_file.sh", mainFileInfo.getFilePath(), execPath, mainFileInfo.getFileType());
            String output = commandResult.getOutput();
            if (output.contains("failed")) {
                String msg = "压缩文件内容只能包含一个目录";
                if (StrUtil.equals("main", type)) {
                    msg = "压缩文件内容只能包含一个文件";
                }
                return Result.error(msg);
            }

            // 主文件只能有一个正在应用状态
            if (type.equals("main")) {
                List<MainFileInfo> list = this.list(Wrappers.lambdaQuery(MainFileInfo.class)
                        .eq(MainFileInfo::getStatus, 1)
                        .ne(MainFileInfo::getId, id));
                for (MainFileInfo mainFileInfoTemp : list) {
                    mainFileInfoTemp.setStatus(0);
                    this.updateById(mainFileInfoTemp);
                }
            }

            MainFileInfo mainFileInfoTemp = new MainFileInfo();
            mainFileInfoTemp.setId(id);
            mainFileInfoTemp.setStatus(1);

            String workPath = execPath + output;
            mainFileInfoTemp.setWorkPath(workPath);
            this.updateById(mainFileInfoTemp);
            return Result.OK("应用成功");
        } catch (Exception e) {
            log.error("执行脚本失败", e);
            return Result.error("执行失败，请联系管理员");
        }
    }
}
