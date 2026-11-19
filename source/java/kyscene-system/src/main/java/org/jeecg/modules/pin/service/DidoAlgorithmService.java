package org.jeecg.modules.pin.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.pin.entity.DidoAlgorithmDO;
import org.jeecg.modules.pin.entity.PageVo;

import java.util.LinkedList;
import java.util.List;

public interface DidoAlgorithmService extends IService<DidoAlgorithmDO> {
    List<KyCategory> getAlgorithmList();

    IPage<PageVo> getTableData(Integer pageNo, Integer pageSize);

    void saveData(DidoAlgorithmDO didoAlgorithmDO);

    boolean enableDido();

    void syncCache();

    void executeGpoCommand(List<JSONObject> algorithms);

}
