package org.jeecg.modules.dataPush;

import com.alibaba.fastjson.JSONObject;

public interface IDataPushService {

    void saveSetting(JSONObject params);

    DataPushVo getSetting();

    void saveStreamSetting(StreamBizInfo streamBizInfo);

    StreamBizInfo getStreamSetting();
}
