/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.face.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.face.entity.FaceRemoteUpdateMsg;
import org.jeecg.modules.face.mapper.FaceRemoteUpdateMsgMapper;
import org.jeecg.modules.face.service.FaceRemoteUpdateMsgService;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author： DecoderX
 * @date： 2025/5/21
 */
@Service
public class FaceRemoteUpdateMsgServiceImpl extends ServiceImpl<FaceRemoteUpdateMsgMapper, FaceRemoteUpdateMsg> implements FaceRemoteUpdateMsgService {

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveData(FaceRemoteUpdateMsg faceRemoteUpdateMsg) {
        long count = this.count();
        if (count >= 1000) {
            // 获取超出部分的数量
            int deleteCount = (int) (count - 999);

            // 查询出最旧的N条记录并删除
            List<FaceRemoteUpdateMsg> oldestRecords = this.list(Wrappers.lambdaQuery(FaceRemoteUpdateMsg.class)
                    .select(FaceRemoteUpdateMsg::getId)
                    .orderByAsc(FaceRemoteUpdateMsg::getUpdateTime)
                    .last("LIMIT " + deleteCount));

            if (!oldestRecords.isEmpty()) {
                List<String> idsToDelete = oldestRecords.stream()
                        .map(FaceRemoteUpdateMsg::getId)
                        .collect(Collectors.toList());

                this.removeByIds(idsToDelete);
            }
        }

        this.save(faceRemoteUpdateMsg);
    }

    @Override
    public FaceRemoteUpdateMsg getRecentData() {
        // 获取当前配置的 projectKey
        String projectCode = SysParameterUtil.getProjectCode();
        if (projectCode == null) return null;
        // 按照时间倒序，取第一个
        return this.getOne(Wrappers.lambdaQuery(FaceRemoteUpdateMsg.class)
                .eq(FaceRemoteUpdateMsg::getProjectKey, projectCode)
                .orderByDesc(FaceRemoteUpdateMsg::getUpdateTime), false);
    }

}
