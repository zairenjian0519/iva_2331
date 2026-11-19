/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.deviceManagement.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.deviceManagement.config.UploadUpdateProperties;
import org.jeecg.modules.deviceManagement.entity.SoftUpdateDo;
import org.jeecg.modules.deviceManagement.mapper.SoftUpdateMapper;
import org.jeecg.modules.deviceManagement.service.ISoftUpdateService;
import org.jeecg.modules.system.model.CommandResult;
import org.jeecg.modules.system.util.CmdUti;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @author： DecoderX
 * @date： 2025/5/27
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SoftUpdateServiceImpl extends ServiceImpl<SoftUpdateMapper, SoftUpdateDo> implements ISoftUpdateService {

    private final UploadUpdateProperties uploadUpdateProperties;

    @Override
    public IPage<SoftUpdateDo> getUpdatePage(SoftUpdateDo softUpdateDo, Integer pageNo, Integer pageSize) {
        Page<SoftUpdateDo> page = new Page<>(pageNo, pageSize);
        return this.page(page, Wrappers.lambdaQuery(SoftUpdateDo.class)
                .eq(SoftUpdateDo::getSoftType, softUpdateDo.getSoftType())
                .eq(StrUtil.isNotBlank(softUpdateDo.getAlgorithmId()),SoftUpdateDo::getAlgorithmId, softUpdateDo.getAlgorithmId())
                .orderByDesc(SoftUpdateDo::getCreateTime));
    }

    @Override
    public Result<?> deleteUploadSoft(String id) {
        SoftUpdateDo softUpdateDo = this.getById(id);
        // 发布状态不允许删除
        if (softUpdateDo.getStatus() == 1) {
            return Result.error("发布状态不允许删除");
        }
        try {
            // 删除文件
            FileUtil.del(softUpdateDo.getFilePath());
            // 删除数据库记录
            this.removeById(id);
            return Result.OK("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败");
        }
    }

    @Override
    public Result<?> publishSoft(String id) {
        SoftUpdateDo softUpdateDo = this.getById(id);
        if (softUpdateDo.getStatus() == 1) {
            return Result.error("该升级包已发布");
        }
        try {
            // 解压到目标路径
            Result<String> extractResult = executeUnzipScript(softUpdateDo);
            if (!extractResult.isSuccess()) return extractResult;

            // 执行更新脚本
            Result<?> updateResult = handleUpdateLogic(softUpdateDo);
            if (!updateResult.isSuccess()) return updateResult;
            softUpdateDo.setStatus(1);

            // 获取当前已经发布的版本
            SoftUpdateDo publishedSoft = this.getOne(Wrappers.lambdaQuery(SoftUpdateDo.class)
                    .eq(SoftUpdateDo::getSoftType, softUpdateDo.getSoftType())
                    .eq(SoftUpdateDo::getStatus, 1), false);
            softUpdateDo.setPrevSoftVersion("");
            if (publishedSoft != null) {
                softUpdateDo.setPrevSoftVersion(publishedSoft.getSoftVersion());
                // 把已发布的版本记录状态改为 0
                publishedSoft.setStatus(0);
                this.updateById(publishedSoft);
            }
            this.updateById(softUpdateDo);
            return Result.OK("发布成功");
        } catch (Exception e) {
            log.error("发布失败：{}", e.getMessage());
            return Result.error("发布失败，服务端错误");
        }
    }

    @Override
    public Result<?> withdrawSoft(String softType) {
        try {
            // 获取当前已经发布的版本
            SoftUpdateDo publishedSoft = this.getOne(Wrappers.lambdaQuery(SoftUpdateDo.class)
                    .eq(SoftUpdateDo::getSoftType, softType)
                    .eq(SoftUpdateDo::getStatus, 1), false);
            if (publishedSoft != null) {
                publishedSoft.setStatus(0); // 把已发布的版本记录状态改为 0
                this.updateById(publishedSoft);
                // 获取前一个版本
                SoftUpdateDo prevSoft = this.getOne(Wrappers.lambdaQuery(SoftUpdateDo.class)
                        .eq(SoftUpdateDo::getSoftType, softType)
                        .eq(SoftUpdateDo::getSoftVersion, publishedSoft.getPrevSoftVersion()));
                if (prevSoft != null) {
                    // 执行撤回脚本
                    handleRollback(softType);
                    prevSoft.setStatus(1);
                    this.updateById(prevSoft);
                    return Result.OK("撤回成功");
                }
            } else {
                // 直接执行回滚脚本
                handleRollback(softType);
            }

            return Result.OK("撤回成功");
        } catch (Exception e) {
            return Result.error("撤回失败，服务端错误");
        }
    }

    @Override
    public Result<?> uploadUpdateFile(MultipartFile file, SoftUpdateDo softUpdateDo) {
        // 参数校验
        if (StrUtil.isBlank(uploadUpdateProperties.getPath())) {
            return Result.error("请配置基础文件上传路径");
        }

        Result<?> pathValidation = validatePaths(softUpdateDo);
        if (!pathValidation.isSuccess()) return pathValidation;

        String originalFilename = file.getOriginalFilename();
        // 支持 tar/tar.gz/zip 格式的文件
        List<String> fileExts = List.of("tar", "tar.gz", "zip");
        if (!fileExts.contains(FileUtil.extName(originalFilename))) {
            return Result.error("只允许上传.tar/.tar.gz/.zip格式的文件");
        }

        // 判断文件类型，同种类型不能超过5条记录
        long count = this.count(Wrappers.lambdaQuery(SoftUpdateDo.class)
                .eq(SoftUpdateDo::getSoftType, softUpdateDo.getSoftType()));
        if (count >= 5) {
            return Result.error("同种类型最多保留5条升级记录，请删除旧记录");
        }

        // 构建上传路径并保存文件
        buildUploadInfo(originalFilename, softUpdateDo);
        try {
            File dest = new File(softUpdateDo.getFilePath());
            File parentDir = dest.getParentFile();
            if (!parentDir.exists()) parentDir.mkdirs(); // 创建多级目录
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("上传文件失败", e);
            return Result.error("上传文件失败");
        }

        // 解压并获取版本信息
        Result<String> execResult = getVersionInfo(softUpdateDo);
        if (!execResult.isSuccess()) return execResult;
        JSONObject json = JSONObject.parseObject(execResult.getResult());
        String version = json.getString("version");
        String remark = json.getString("remark");
        log.info("解压文件成功，版本号：{}，备注：{}", version, remark);
        if (StrUtil.isBlank(version)) {
            return Result.error("获取版本号失败");
        }
        // 如果存在相同版本，上传失败
        long checkVersionCount = this.count(Wrappers.lambdaQuery(SoftUpdateDo.class)
                .eq(SoftUpdateDo::getSoftType, softUpdateDo.getSoftType())
                .eq(SoftUpdateDo::getSoftVersion, version));
        if (checkVersionCount > 0) {
            FileUtil.del(softUpdateDo.getFilePath());  // 删除文件
            return Result.error("已经存在相同版本号");
        }
        // 如果是算法更新，判断是否是更新插件
        if ("algorithm_pkg".equals(softUpdateDo.getSoftType())) {
            if (StrUtil.contains(version, "plugin")) {
                softUpdateDo.setAlgorithmType("plugin");
            } else {
                softUpdateDo.setAlgorithmType("model");
            }
        }
        softUpdateDo.setSoftVersion(version);
        softUpdateDo.setRemark(remark);

        // 保存数据库记录
        softUpdateDo.setFileSize(file.getSize());
        softUpdateDo.setStatus(0); // 状态：0 - 未发布
        this.save(softUpdateDo);
        return Result.OK("上传成功");
    }

    private Result<?> validatePaths(SoftUpdateDo softUpdateDo) {
        String type = softUpdateDo.getSoftType();
        if ("web_front".equals(type) && StrUtil.isBlank(uploadUpdateProperties.getWebFrontPath())) {
            return Result.error("请配置web前端文件上传路径");
        }
        if ("web_backend".equals(type) && StrUtil.isBlank(uploadUpdateProperties.getWebBackendPath())) {
            return Result.error("请配置web后端文件上传路径");
        }
        if ("algorithm_pkg".equals(type) && StrUtil.isBlank(uploadUpdateProperties.getAlgorithmPath())) {
            return Result.error("请配置算法文件上传路径");
        }
        return Result.OK();
    }

    private void buildUploadInfo(String originalFilename, SoftUpdateDo softUpdateDo) {
        String basePath = uploadUpdateProperties.getPath();
        String subDir = switch (softUpdateDo.getSoftType()) {
            case "web_front" -> "web_front/";
            case "web_backend" -> "web_backend/";
            case "algorithm_pkg" -> "algorithm_pkg/";
            default -> throw new IllegalArgumentException("未知的类型: " + softUpdateDo.getSoftType());
        };
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String newFileName = uuid + ext;

        softUpdateDo.setFileExt(ext);
        softUpdateDo.setFileName(newFileName);
        softUpdateDo.setFilePath(basePath + subDir + newFileName);
    }

    /**
     * 从压缩包中获取版本信息
     *
     * @param softUpdateDo SoftUpdateDo
     * @return Result
     */
    private Result<String> getVersionInfo(SoftUpdateDo softUpdateDo) {
        try {
            CommandResult commandResult = CmdUti.execByShell("/home/super/update/read_meta.sh", softUpdateDo.getFilePath());
            if (ObjectUtil.isEmpty(commandResult)) {
                return Result.error("没有获取到版本信息");
            }
            return Result.ok(commandResult.getOutput());
        } catch (Exception e) {
            return Result.error("获取版本信息失败：" + e.getMessage());
        }
    }

    /**
     * 执行解压脚本
     *
     * @param softUpdateDo SoftUpdateDo
     * @return Result
     */
    private Result<String> executeUnzipScript(SoftUpdateDo softUpdateDo) {
        try {
            String unzipTargetPath = switch (softUpdateDo.getSoftType()) {
                case "web_front" -> uploadUpdateProperties.getWebFrontPath();
                case "web_backend" -> uploadUpdateProperties.getWebBackendPath();
                case "algorithm_pkg" -> {
                    if (softUpdateDo.getAlgorithmType().equals("plugin")) {
                        yield uploadUpdateProperties.getAlgorithmPluginPath();
                    } else if (softUpdateDo.getAlgorithmType().equals("model")){
                        yield uploadUpdateProperties.getAlgorithmPath();
                    } else {
                        yield "";
                    }
                }
                default -> "";
            };
            if (StrUtil.isBlank(unzipTargetPath)) {
                return Result.error("请配置解压路径");
            }
            CommandResult result = CmdUti.execByShell("/home/super/update/extract_payload.sh", softUpdateDo.getFilePath(), unzipTargetPath);
            if (result.getExitCode() != 0) {
                return Result.error("解压脚本执行失败：" + result.getOutput());
            }
            return Result.ok(result.getOutput());
        } catch (Exception e) {
            return Result.error("解压脚本执行失败：" + e.getMessage());
        }
    }

    /**
     * 执行更新逻辑
     *
     * @param softUpdateDo 类型
     * @return Result
     */
    private Result<?> handleUpdateLogic(SoftUpdateDo softUpdateDo) {
        String softType = softUpdateDo.getSoftType();
        switch (softType) {
            case "web_front":
                int webFrontExitCode = CmdUti.execCmdWithRealTimeOutput(new String[]{"bash", "/home/super/update/update_html.sh"});
                if (webFrontExitCode != 0) {
                    handleRollback(softType);
                    return Result.error("文件更新失败，已回退");
                }
                return Result.OK();
            case "web_backend":
                CmdUti.restartJava();
                return Result.OK();
            case "algorithm_pkg":
                int algExitCode = CmdUti.execCmdWithRealTimeOutput(new String[]{"bash", "/home/super/update/update_weights.sh", softUpdateDo.getAlgorithmType()});
                if (algExitCode != 0) {
                    handleRollback(softType);
                    return Result.error("文件更新失败，已回退");
                }
                CmdUti.restartAiEngine();
                return Result.OK();
            default:
                return Result.error("不支持的类型");
        }
    }

    private void handleRollback(String softType) {
        switch (softType) {
            case "web_front":
                CmdUti.execCmdWithRealTimeOutput(new String[]{"bash", "/home/super/update/process_rollback.sh", uploadUpdateProperties.getWebFrontPath()});
                break;
            case "web_backend":
                CmdUti.execCmdWithRealTimeOutput(new String[]{"bash", "/home/super/update/process_rollback.sh", uploadUpdateProperties.getWebBackendPath()});
                CmdUti.restartJava();
                break;
            case "algorithm_pkg":
                CmdUti.execCmdWithRealTimeOutput(new String[]{"bash", "/home/super/update/process_rollback.sh", uploadUpdateProperties.getAlgorithmPath()});
                CmdUti.restartAiEngine();
            default:
                break;
        }
    }

    @Override
    public Result<?> getAllSoftVersion() {
        JSONObject  json = new JSONObject();
        json.put("web_front", "");
        json.put("web_backend", "");
        json.put("algorithm_pkg", "");

        List<SoftUpdateDo> softUpdateDos = this.list(Wrappers.lambdaQuery(SoftUpdateDo.class).eq(SoftUpdateDo::getStatus, 1));
        for (SoftUpdateDo softUpdateDo : softUpdateDos) {
            json.put(softUpdateDo.getSoftType(), softUpdateDo.getSoftVersion());
        }

        return Result.OK(json);
    }
}
