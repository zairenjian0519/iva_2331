package org.jeecg.modules.wireless;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.wireless.entity.AudioFileInfo;
import org.jeecg.modules.wireless.entity.SoundDeviceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 无线音柱controller
 */
@RestController
@RequestMapping("/wireless/pillar")
public class SoundPillarController {

    @Autowired
    private SoundPillarService soundPillarService;

    @Autowired
    private AudioFileService audioFileService;

    /**
     * 获取音频文件列表
     */
    @GetMapping("/listAudioFiles")
    public Result<IPage<AudioFileInfo>> listAudioFiles(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return audioFileService.listAudioFiles(pageNo, pageSize);
    }


    /**
     * 同步音频文件到数据库
     */
    @GetMapping("/syncAudioFiles")
    public Result<?> syncAudioFiles() {
        audioFileService.syncAudioFiles();
        return Result.OK();
    }

    /**
     * 获取所有的音频文件
     */
    @GetMapping("/getAudioList")
    public Result<?> getAudioList() {
        return Result.OK(audioFileService.getAvailableAudioFiles());
    }

    /**
     * 重启设备
     */
    @PostMapping("/restart")
    public Result<?> restartDevice() {
        soundPillarService.restartDevice();
        return Result.OK();
    }

    /**
     * 新增或者更新音柱列表
     */
    @PostMapping("/updateSoundDeviceInfo")
    public Result<?> updateSoundDeviceInfo(@RequestBody List<SoundDeviceInfo> list) {
        soundPillarService.updateSoundDeviceInfo(list);
        return Result.OK();
    }

    /**
     * 获取列表
     */
    @GetMapping("/list")
    public Result<IPage<SoundDeviceInfo>> getDeviceList(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return soundPillarService.getDeviceList(pageNo, pageSize);
    }

    /**
     * 启用或者禁用音柱
     */
    @GetMapping("/updateStatus")
    public Result<?> updateStatus(Integer status, String id) {
        soundPillarService.updateStatus(status, id);
        return Result.OK();
    }

    /**
     * 获取算法列表
     */
    @GetMapping("/getAlgorithmList")
    public Result<?> getAlgorithmList() {
        List<KyCategory> algorithmList = soundPillarService.getAlgorithmList();
        return Result.OK(algorithmList);
    }

    /**
     * 绑定音频算法
     */
    @PostMapping("/updateVoiceAlgorithm")
    public Result<?> updateVoiceAlgorithm(@RequestBody AudioFileInfo audioFileInfo) {
        audioFileService.updateVoiceAlgorithm(audioFileInfo);
        return Result.OK();
    }

    /**
     * 播放音频文件
     */
    @PostMapping("/playAudioFile")
    public Result<?> playAudioFile(@RequestBody JSONObject params) {
        String taskCode = soundPillarService.playAudioFile(params);
        JSONObject data = new JSONObject();
        data.put("taskCode", taskCode);
        return Result.ok(data);
    }

    /**
     * 更新设备音量
     */
    @GetMapping("/updateVolume")
    public Result<?> updateVolume(Integer deviceId, Integer volume) {
        soundPillarService.updateVolume(deviceId, volume);
        return Result.ok();
    }

    @GetMapping("/closePlayTask")
    public Result<?> closePlayTask(String taskCode){
        soundPillarService.closePlayTask(taskCode);
        return Result.ok();
    }

    @GetMapping("/getAccountInfo")
    public Result<?> getSoundPillarAccountInfo(){
        JSONObject result = soundPillarService.getSoundPillarAccountInfo();
        return Result.ok(result);
    }

    @GetMapping("/getProjectInfo")
    public Result<?> getProjectInfo(){
        JSONObject result = soundPillarService.getProjectInfo();
        return Result.ok(result);
    }

    @GetMapping("/deleteById")
    public Result<?> deleteById(@RequestParam String id){
        soundPillarService.removeById(id);
        return Result.ok();
    }

    @PostMapping("/updateSoundPillar")
    public Result<?> updateSoundPillar(@RequestBody SoundDeviceInfo soundDeviceInfo){
        soundPillarService.updateById(soundDeviceInfo);
        return Result.ok("操作成功");
    }

    @GetMapping("/syncDevices")
    public Result<?> syncDevices(@RequestParam(required = false) String soundPillarDir){
        soundPillarService.syncDevices(soundPillarDir);
        return Result.ok("操作成功");
    }
}