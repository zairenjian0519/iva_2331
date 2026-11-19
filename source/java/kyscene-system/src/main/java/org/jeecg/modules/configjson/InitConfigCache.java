package org.jeecg.modules.configjson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.util.CmdUti;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 开机-初始化配置缓存
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class InitConfigCache implements CommandLineRunner {

    private final CreateConfigJsonService createConfigJsonService;
    private final CameraDeploymentConfigService cameraDeploymentConfigService;

    @Override
    public void run(String... args) throws Exception {
        try {
            cameraDeploymentConfigService.reconcile();
        } catch (Exception error) {
            log.error("清理已删除相机的推理部署映射失败", error);
        }
        // 初始化相机配置缓存
        createConfigJsonService.initCameraConfigCache();
        // 初始化系统配置缓存
        createConfigJsonService.initSystemConfigCache();
        // 初始化算法配置缓存
        createConfigJsonService.initAlgConfigCache();
        // 初始化组算法配置缓存
        createConfigJsonService.initGroupAlgConfigCache();
        // 重启AI服务
        CmdUti.restartAiServer();
    }
}
