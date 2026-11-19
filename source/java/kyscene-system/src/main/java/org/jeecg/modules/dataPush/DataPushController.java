package org.jeecg.modules.dataPush;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.api.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 数据推送Controller
 */
@RestController
@RequestMapping("/dataPush")
public class DataPushController {

    @Autowired
    private IDataPushService dataPushService;

    /**
     * 保存数据推送配置
     */
    @PostMapping("/saveSetting")
    public Result<?> saveSetting(@RequestBody JSONObject params) {
        dataPushService.saveSetting(params);
        return Result.OK(Boolean.TRUE);
    }

    /**
     * 获取数据推送配置
     */
    @GetMapping("/getSetting")
    public Result<?> getSetting() {
        return Result.ok(dataPushService.getSetting());
    }

    /**
     * 保存视频流业务地址
     */
    @PostMapping("/saveStreamSetting")
    public Result<?> saveStreamSetting(@RequestBody StreamBizInfo streamBizInfo) {
        dataPushService.saveStreamSetting(streamBizInfo);
        return Result.OK();
    }

    /**
     * 获取视频流业务配置
     */
    @GetMapping("/getStreamSetting")
    public Result<?> getStreamSetting() {
        return Result.ok(dataPushService.getStreamSetting());
    }
}
