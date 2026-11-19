package org.jeecg.modules.algAdapter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.algAdapter.entity.AlgorithmMethodReg;

import java.util.List;
import java.util.Map;

/**
 * @author： DecoderX
 * @date： 2025/7/18
 */
@Mapper
public interface AlgorithmMethodRegMapper extends BaseMapper<AlgorithmMethodReg> {

    IPage<AlgorithmMethodReg> selectPageRows(IPage<AlgorithmMethodReg> page);

    List<Map<String, String>> queryInitAiParams();

    Integer queryMaxId();
}
