package org.jeecg.modules.algAdapter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.algAdapter.entity.AlgorithmClsReg;

/**
 * @author： DecoderX
 * @date： 2025/7/18
 */
public interface IAlgorithmClsRegService extends IService<AlgorithmClsReg> {

    IPage<AlgorithmClsReg> getPage(Integer pageNo, Integer pageSize);

    Result<?> addData(AlgorithmClsReg algorithmClsReg);

    Result<?> updateData(AlgorithmClsReg algorithmClsReg);

    Result<?> deleteRow(String id);
}
