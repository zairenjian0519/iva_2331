/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.qwenVL;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.algorithmmanager.entity.AlgorithmManager;
import org.jeecg.modules.algorithmmanager.mapper.AlgorithmManagerMapper;
import org.jeecg.modules.baseImage.entity.BasicImageDo;
import org.jeecg.modules.baseImage.service.IBasicImageService;
import org.jeecg.modules.mqtt.MqttClientService;
import org.jeecg.modules.system.model.CommandResult;
import org.jeecg.modules.system.util.CmdUti;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author： DecoderX
 * @date： 2025/7/25
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MultiModalFenceCall {

    private final QianwenModal qianwenModal;

    private final AlgorithmManagerMapper algorithmManagerMapper;

    private final Properties properties;

    private final IBasicImageService basicImageService;

    // 使用自定义的固定容量队列，支持自动移除旧数据
    private final BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>(20) {
        @Override
        public boolean offer(Map<String, Object> event) {
            // 如果队列满了，移除最旧的元素后再添加新元素
            if (remainingCapacity() <= 0) {
                Map<String, Object> oldest = poll(); // 移除最旧元素
                if (oldest != null) {
                    log.debug("队列已满，移除最旧元素: cameraId={}",
                            oldest.get("cameraId"));
                }
            }
            return super.offer(event);
        }
    };

    public void addEvent(Map<String, Object> event) {
        queue.offer(event); // 非阻塞入队，队列满时自动移除旧数据
    }

    public Map<String, Object> popEvent() {
        return queue.poll(); // 非阻塞出队
    }

    @PostConstruct
    public void startConsumer() {
        // 创建固定大小的线程池
        int threadPoolSize = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        // 启动多个消费者线程
        for (int i = 0; i < threadPoolSize; i++) {
            executorService.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Map<String, Object> event = popEvent();
                        if (event == null) {
                            // 如果队列为空，短暂休眠避免忙等待
                            Thread.sleep(100);
                            continue;
                        }

                        // 从队列中取出事件
                        String algorithmId = event.get("algorithmId").toString();
                        String cameraId = event.get("cameraId").toString();
                        String filePath = event.get("filePath").toString();
                        log.info("原图路径 filePath: {}", filePath);
                        String savePath = event.get("savePath").toString();
                        log.info("切图路径 savePath: {}", savePath);
                        Integer isRotate = (Integer) event.get("isRotate");
                        String time = event.get("time").toString();



                        // 查看roi信息是否为空
                        AlgorithmManager algorithmManager = algorithmManagerMapper.getByCameraIdAndAlgorithmCode(cameraId, algorithmId);
                        if (ObjectUtil.isNotEmpty(algorithmManager)) {
                            String checkArea = algorithmManager.getCheckArea();
                            log.info("获取到ROI信息: {}", checkArea);
                            // roi不为空，调用切图脚本
                            if (StrUtil.isNotBlank(checkArea)) {
                                String cropperSaveFilePath = FileUtil.getParent(filePath, 1) + File.separator + "cropper" + File.separator + FileUtil.getName(filePath);
                                log.info("切图保存路径: {}", cropperSaveFilePath);
                                String parent = FileUtil.getParent(cropperSaveFilePath, 1);
                                if (!FileUtil.exist(parent)) {
                                    FileUtil.mkdir(parent);
                                    CmdUti.executeCmd(new String[]{"chmod", "-R", "777", parent});
                                }

                                String shellPath = properties.getScriptPath() + "process_roi";
                                log.info("执行切图脚本路径: {}", shellPath);
                                // cmd命令执行切图
                                String[] cmd = new String[]{shellPath, filePath, cropperSaveFilePath, checkArea};
                                CommandResult commandResult = CmdUti.executeCmd2(cmd);
                                // 循环打印命令执行日志
                                log.info("切图脚本执行结果: {}", commandResult.getOutput());
                                if (commandResult.getExitCode() == 0) {
                                    savePath = cropperSaveFilePath ;
                                    log.info("切图成功, 图片保存路径：{}", cropperSaveFilePath);
                                } else {
                                    log.error("切图失败");
                                    return;
                                }
                            }
                        }

                        // 底图
                        String sourcePath = "";
                        if (qianwenModal.enableBaseImg()) {
                            String basePath = "/home/super/web/template/";
                            String sourceImage = algorithmManager.getSourceImage();
                            if (StrUtil.isBlank(sourceImage)) {
                                log.error("该算法未配置底图图片");
                                return;
                            }
                            BasicImageDo basicImageDo = basicImageService.getDataById(sourceImage);
                            if (basicImageDo == null) {
                                log.error("底图不存在");
                                return;
                            }
                            sourcePath = basePath + basicImageDo.getReName();
                        }
                        JSONObject jsonObject = qianwenModal.MultiModalConversationCall(sourcePath, savePath, filePath);
                        if (jsonObject == null || !jsonObject.getBooleanValue("result")) continue;
                        log.info("返回结果：{}", jsonObject);

                        JSONObject jsonResult = new JSONObject();
                        jsonResult.put("multi_modal", true);
                        jsonResult.put("id", cameraId);

                        Map<String, Object> dataMap = new HashMap<>();
                        dataMap.put("key", "");
                        dataMap.put("value", algorithmId);
                        dataMap.put("grade", "1");
                        dataMap.put("isRotate", isRotate);
                        dataMap.put("targetCount", jsonObject.getIntValue("count"));
                        dataMap.put("time", time);
                        dataMap.put("picUrl", filePath);
                        dataMap.put("videoUrl", "");
                        List<Map<String, Object>> data = Collections.singletonList(dataMap);

                        jsonResult.put("data", data);
                        log.info("MultiModal mqtt message：{}", jsonResult);

                        MqttClientService.getInstance()
                                .getMqttClient("127.0.0.1")
                                .pub("alarm_result", jsonResult.toJSONString());

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("处理事件失败：{}", e.getMessage(), e);
                    }
                }
            });
        }

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }));
    }
}