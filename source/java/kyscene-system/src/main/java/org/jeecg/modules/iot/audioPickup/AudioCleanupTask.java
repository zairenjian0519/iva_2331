/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 定时任务，清理录音文件
 * @author： DecoderX
 * @date： 2025/7/1
 */
@Slf4j
@Component
public class AudioCleanupTask {

    // 支持的音频文件后缀
    private static final String[] AUDIO_EXTENSIONS = {".wav", ".aac", ".mp3"};

    @Autowired
    private AudioPickupProperties audioPickupProperties;

    /**
     * 每 5 分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanupOldAudioFiles() {
        File baseDir = new File(audioPickupProperties.getPath());
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.warn("音频主目录不存在或不是文件夹: {}", audioPickupProperties.getPath());
            return;
        }

        File[] subDirs = baseDir.listFiles(File::isDirectory);
        if (subDirs == null) return;

        for (File subDir : subDirs) {
            File[] audioFiles = subDir.listFiles(file ->
                    file.isFile() && isAudioFile(file.getName())
            );

            if (audioFiles == null || audioFiles.length <= audioPickupProperties.getClearLimit()) continue;

            // 按最后修改时间升序（最旧的排前面）
            Arrays.sort(audioFiles, Comparator.comparingLong(File::lastModified));

            int deleteCount = audioFiles.length - audioPickupProperties.getClearLimit();
            for (int i = 0; i < deleteCount; i++) {
                boolean deleted = audioFiles[i].delete();
                if (deleted) {
                    log.info("已删除旧音频文件: {}", audioFiles[i].getAbsolutePath());
                } else {
                    log.warn("删除失败: {}", audioFiles[i].getAbsolutePath());
                }
            }
        }
    }

    private boolean isAudioFile(String name) {
        return Arrays.stream(AUDIO_EXTENSIONS).anyMatch(name::endsWith);
    }
}