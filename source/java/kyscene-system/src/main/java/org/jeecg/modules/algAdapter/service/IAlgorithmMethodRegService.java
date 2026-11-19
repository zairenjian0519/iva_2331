package org.jeecg.modules.algAdapter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.algAdapter.entity.AlgorithmClsReg;
import org.jeecg.modules.algAdapter.entity.AlgorithmMethodReg;
import org.jeecg.modules.kycategory.entity.KyCategory;

import java.util.List;
import java.util.Map;

/**
 * @author： DecoderX
 * @date： 2025/7/18
 */
public interface IAlgorithmMethodRegService extends IService<AlgorithmMethodReg> {
    IPage<AlgorithmMethodReg> getPage(Integer pageNo, Integer pageSize);

    List<KyCategory> getAlgorithmList();

    List<AlgorithmClsReg> getClassNameList();

    Result<?> saveData(AlgorithmMethodReg algorithmMethodReg);

    Result<?> updateData(AlgorithmMethodReg algorithmMethodReg);

    void initAiEngine();
}
