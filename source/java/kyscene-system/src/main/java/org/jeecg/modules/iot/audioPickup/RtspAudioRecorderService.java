/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.iot.audioPickup.entity.AudioPickup;
import org.jeecg.modules.iot.audioPickup.entity.RecordingTask;
import org.jeecg.modules.iot.audioPickup.service.IAudioPickupService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author： DecoderX
 * @date： 2025/6/30
 */
@Slf4j
@Component
public class RtspAudioRecorderService {

    private PublishAudioRecordMsg publishAudioRecordMsg;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final ConcurrentHashMap<String, RecordingTask> recordingTasks = new ConcurrentHashMap<>();

    @Autowired
    @Lazy
    public void setPublishAudioRecordMsg(PublishAudioRecordMsg publishAudioRecordMsg) {
        this.publishAudioRecordMsg = publishAudioRecordMsg;
    }

    /**
     * 启动音频录制任务
     *
     * @param rtspUrl   RTSP 流地址
     * @param outputDir 保存目录（必须存在）
     * @param taskId    任务唯一 ID，用于后续停止控制
     */
    public Result<?> startRecording(String rtspUrl, Integer durationSeconds, String outputDir, String taskId) {
        if (recordingTasks.containsKey(taskId)) {
            log.warn("录制任务 {} 已经存在，跳过启动。", taskId);
            return Result.OK("录制任务已经存在，跳过启动。");
        }

        File dir = new File(outputDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("创建输出目录成功: {}", outputDir);
            } else {
                throw new IllegalArgumentException("无法创建输出目录: " + outputDir);
            }
        } else if (!dir.isDirectory()) {
            throw new IllegalArgumentException("指定的输出路径不是目录: " + outputDir);
        }

        RecordingTask task = new RecordingTask();
        Future<?> future = executorService.submit(() -> {
            String lastFilePath = null;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 录制文件数量超过5个自动清理
                    // cleanOldAudioFiles(dir, 5);

                    log.info("启动 FFmpeg 录制任务，taskId={}, rtsp={}", taskId, rtspUrl);
                    Process process = getProcess(rtspUrl, durationSeconds, outputDir);
                    task.setProcess(process);

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // log.info("[FFmpeg-{}] {}", taskId, line);
                            // 检测 FFmpeg 写入新文件的日志行
                            if (line.contains("Opening '") && line.contains("' for writing")) {
                                String currentFilePath = line.substring(line.indexOf("Opening '") + 9, line.indexOf("' for writing"));

                                // 上一个文件已写完，发送 MQTT
                                if (lastFilePath != null && FileUtil.exist(lastFilePath)) {
                                    publishAudioRecordMsg.publishMsg(taskId, lastFilePath);
                                }

                                lastFilePath = currentFilePath;
                            }
                        }
                    }

                    int exitCode = process.waitFor();
                    log.warn("FFmpeg 任务 {} 退出，退出码={}，3 秒后重启...", taskId, exitCode);
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("录制任务 {} 被中断，准备退出。", taskId);
                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Stream closed")) {
                        log.info("录制任务 {} 的流已关闭。", taskId);
                    } else {
                        log.error("录制任务 {} 读取流时异常：{}", taskId, e.getMessage(), e);
                    }
                } catch (Exception e) {
                    log.error("录制任务 {} 出现异常：{}", taskId, e.getMessage(), e);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        task.setFuture(future);
        recordingTasks.put(taskId, task);
        return Result.OK("任务已开始执行");
    }

    @NotNull
    private Process getProcess(String rtspUrl, Integer recordSegmentSeconds, String outputDir) throws IOException {
        rtspUrl = URLUtil.encode(rtspUrl);
        String command = "sudo -E LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/lynxi/lynffmpeg/lib " +
                "/opt/lynxi/lynffmpeg/bin/ffmpeg -re -rtsp_transport tcp -i '" + rtspUrl + "' " +
                "-vn -acodec pcm_s16le -ar 48000 -ac 1 -f segment -segment_time " + recordSegmentSeconds +
                " -strftime 1 " + outputDir + "/audio_%Y%m%d_%H%M%S.wav";
        log.info("Starting recording with command: {}", command);
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
        builder.redirectErrorStream(true);
        return builder.start();
    }

    /**
     * 停止录制任务
     *
     * @param taskId 任务 ID
     */
    public Result<?> stopRecording(String taskId) {
        RecordingTask task = recordingTasks.remove(taskId);
        if (task != null) {
            try {
                // 优雅中断线程
                task.getFuture().cancel(true);

                // 尝试销毁 FFmpeg 进程
                Process process = task.getProcess();
                if (process != null && process.isAlive()) {
                    process.destroy();
                    // 等待最多3秒，确保进程结束
                    boolean exited = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (!exited) {
                        log.warn("FFmpeg 进程未正常退出，强制杀死：{}", taskId);
                        process.destroyForcibly();
                    }
                }

                log.info("录制任务 {} 已停止。", taskId);

                // 异步调用 shell 脚本杀死残留的 ffmpeg 进程
                executorService.submit(() -> {
                    try {
                        String scriptPath = "/home/super/web/kill_pickup_ffmpeg.sh";
                        ProcessBuilder builder = new ProcessBuilder("bash", scriptPath, taskId);
                        builder.inheritIO(); // 让脚本输出在控制台显示
                        Process killProcess = builder.start();
                        int exitCode = killProcess.waitFor();
                        log.info("脚本 {} 执行完成，退出码={}", scriptPath, exitCode);
                    } catch (Exception e) {
                        log.error("执行 kill_pickup_ffmpeg.sh 脚本失败：{}", e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                log.error("停止录制任务 {} 时出现异常：{}", taskId, e.getMessage(), e);
                return Result.error("停止录制任务时发生错误：" + e.getMessage());
            }
        }
        return Result.OK("录制任务已停止。");
    }

    /**
     * 停止所有录制任务
     */
    @PreDestroy
    public void shutdownAll() {
        log.info("应用退出，正在停止所有录制任务...");

        // 准备需要更新数据库的列表list
        List<AudioPickup> audioPickups = new ArrayList<>();
        recordingTasks.keySet().forEach(taskId -> {
            AudioPickup audioPickup = new AudioPickup();
            audioPickup.setId(taskId);
            audioPickup.setCollectStatus(0);
            audioPickups.add(audioPickup);
        });

        // 停止所有录制任务
        recordingTasks.keySet().forEach(this::stopRecording);

        // 更新数据库
        IAudioPickupService audioPickupService = SpringContextUtils.getBean(IAudioPickupService.class);
        audioPickupService.updateBatchById(audioPickups);

        // executorService.shutdownNow();
    }

    private void cleanOldAudioFiles(File dir, int maxFiles) {
        File[] audioFiles = dir.listFiles((d, name) -> name.matches("audio_\\d{8}_\\d{6}\\.wav"));

        if (audioFiles == null || audioFiles.length <= maxFiles) return;

        // 按最后修改时间升序排序（旧的在前）
        Arrays.sort(audioFiles, Comparator.comparingLong(File::lastModified));

        int filesToDelete = audioFiles.length - maxFiles;
        for (int i = 0; i < filesToDelete; i++) {
            boolean deleted = audioFiles[i].delete();
            if (deleted) {
                log.info("已删除旧音频文件: {}", audioFiles[i].getAbsolutePath());
            } else {
                log.warn("删除音频文件失败: {}", audioFiles[i].getAbsolutePath());
            }
        }
    }

}