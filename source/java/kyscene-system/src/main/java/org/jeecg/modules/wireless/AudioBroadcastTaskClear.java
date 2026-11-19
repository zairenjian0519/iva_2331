package org.jeecg.modules.wireless;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.entity.SysParameter;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.jeecg.modules.thirdApi.HttpClientUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

/**
 * 定时清除广播任务的消息
 */
@Slf4j
public class AudioBroadcastTaskClear implements Job {

    private Timer timer = null;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (timer == null) timer = new Timer();
        timer.purge();
        JSONObject params = new JSONObject();
        params.put("PageSize", 50);
        params.put("PageNum", 1);
        params.put("OrderKey", "Id");
        params.put("Desc", 0);
        SysParameter sysParameter = SysParameterUtil.getSysParameter("TaskName");
        if (ObjectUtil.isEmpty(sysParameter)) throw new JobExecutionException("没查询到音频播放任务名，请在系统参数中配置");
        String taskName = sysParameter.getParamVal();
        JSONArray array = JSON.parseArray("[[{\"Key\":\"TaskName\",\"Type\":\"str\",\"Value\":\"" + taskName + "\",\"Op\":\"=\"}]]");
        params.put("Filters", array);
        String responseStr = null;
        try {
            String token = SosHttpApiUtil.getToken();
            if (token == null) throw new JobExecutionException("获取sos token 失败");
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("Token", token);
            responseStr = HttpClientUtil.requestPostJsonWithHeader(Constant.AUDIO_BROADCAST_TASK_URL, params, headerMap);
        } catch (Exception e) {
            log.error("请求广播任务列表失败：{}", e.getLocalizedMessage());
        }
        if (StrUtil.isBlank(responseStr)) return;
        JSONObject responseObj = JSONObject.parseObject(responseStr);
        if (responseObj.getInteger("Status") != 0) return;
        JSONArray recordList = responseObj.getJSONArray("RecordList");
        if (ObjectUtil.isEmpty(recordList) || recordList.size() <= 5) return;
        // 大于 5 条就删除
        for (int i = 6; i < recordList.size(); i++) {
            JSONObject jsonObject = recordList.getJSONObject(i);
            String taskCode = jsonObject.getString("TaskCode");
            AudioPlayClearSchedule.addTask(taskCode);
        }
    }
}
