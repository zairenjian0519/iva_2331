package org.jeecg.modules.configjson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.util.CmdUti;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@Api(tags = "配置生成")
@RequiredArgsConstructor
@RestController
@RequestMapping("/createConfig/saveJson")
public class CreateConfigJson {

    private final CreateConfigJsonService createConfigJsonService;

    @ApiOperation(value = "生成配置-相机", notes = "生成配置-相机")
    @GetMapping("camera")
    public Result<String> saveCameraConfig(@RequestParam(name = "deviceId") String deviceId, @RequestParam(name = "ip") String ip) {
        return createConfigJsonService.saveCameraConfig(deviceId, ip);
    }

    @ApiOperation(value = "生成配置-算法", notes = "生成配置-算法")
    @GetMapping("alg")
    public Result<String> saveAlgConfig(@RequestParam(name = "deviceId") String deviceId, @RequestParam(name = "ip") String ip) {
        Result<String> result = createConfigJsonService.updateAiServerDetectConfig(deviceId, ip);
        // 重启 ai_engine
        CmdUti.restartAiEngine();
        return result;
    }

    @ApiOperation(value = "生成配置-系统", notes = "生成配置-系统")
    @GetMapping("/sys")
    public Result<String> saveSystemConfig(@RequestParam(name = "deviceId") String deviceId, @RequestParam(name = "ip") String ip) {
        return createConfigJsonService.updateAiServerSystemConfig(deviceId, ip);
    }

    @ApiOperation(value = "生成相机和算法配置-通过相机id", notes = "生成相机和算法配置-通过相机id")
    @GetMapping("/savaAll")
    public Result<String> savaAllConfig(@RequestParam(name = "cameraId") String cameraId) {
        return createConfigJsonService.updateAiServerConfigByCameraId(cameraId);
    }

    @ApiOperation(value = "生成相机和算法配置-通过设备id", notes = "生成相机和算法配置-通过设备id")
    @GetMapping("/savaAllByDev")
    public Result<String> savaAllConfigByDev(@RequestParam(name = "devId") String devId) {
        return createConfigJsonService.updateAiServerConfigByDeviceId(devId);
    }

    @ApiOperation(value = "生成配置-保存组配置并下发", notes = "生成配置-保存组配置并下发")
    @GetMapping("/saveGroupAll")
    public Result<String> saveGroupAll() {
        return createConfigJsonService.saveGroupAll();
    }

    @ApiOperation(value = "生成配置-组算法", notes = "生成配置-组算法")
    @GetMapping("groupAlg")
    public Result<String> saveGroupAlgConfig(@RequestParam(name = "deviceId") String deviceId, @RequestParam(name = "ip") String ip) {
        return createConfigJsonService.saveGroupAlgConfig(deviceId, ip);
    }

}
