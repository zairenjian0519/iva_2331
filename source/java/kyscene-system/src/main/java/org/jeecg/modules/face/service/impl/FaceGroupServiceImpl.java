package org.jeecg.modules.face.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.face.baidu.UserGroupService;
import org.jeecg.modules.face.entity.FaceDetailDo;
import org.jeecg.modules.face.entity.FaceGroupDo;
import org.jeecg.modules.face.mapper.FaceGroupMapper;
import org.jeecg.modules.face.properties.FaceConfigProperties;
import org.jeecg.modules.face.service.FaceDetailService;
import org.jeecg.modules.face.service.FaceGroupService;
import org.jeecg.modules.system.util.CmdUti;
import org.jeecg.modules.system.util.SysParameterUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FaceGroupServiceImpl extends ServiceImpl<FaceGroupMapper, FaceGroupDo> implements FaceGroupService {

    private final FaceConfigProperties faceConfigProperties;

    private final UserGroupService userGroupService;

    @Override
    public List<FaceGroupDo> getList() {
        return this.list();
    }

    @Override
    public void addFaceGroup(FaceGroupDo faceGroupDo) {
        String groupName = faceGroupDo.getGroupName();
        // 判断组名是否存在
        LambdaQueryWrapper<FaceGroupDo> queryWrapper = Wrappers.lambdaQuery(FaceGroupDo.class)
                .eq(FaceGroupDo::getGroupName, groupName)
                .or()
                .eq(StrUtil.isNotBlank(faceGroupDo.getGroupId()), FaceGroupDo::getGroupId, faceGroupDo.getGroupId());
        if (this.count(queryWrapper) > 0) {
            throw new JeecgBootException("组名已存在或组ID重复");
        }

        // 创建目录
        String groupPathName = "group" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss");
        String groupPath = faceConfigProperties.getBaseFacePath() + groupPathName;
        File groupPathFile = new File(groupPath);
        boolean b = groupPathFile.mkdirs();
        if (!b) {
            throw new JeecgBootException("创建组文件失败");
        }
        faceGroupDo.setGroupPath(groupPathName);
        if (StrUtil.isBlank(faceGroupDo.getGroupId())) {
            faceGroupDo.setGroupId(groupPathName);
        }
        this.save(faceGroupDo);

        // 初始化redis中的face group 路径
        initFaceGroup(faceGroupDo.getId());

        // 创建百度人脸库
        if (SysParameterUtil.isBaiduFaceEnabled()) {
            try {
                userGroupService.createUserGroup(faceGroupDo.getGroupId());
            }catch (Exception e) {
                throw new JeecgBootException("创建人脸库失败: " + e.getMessage());
            }
        }

    }

    @Override
    public void updateFaceGroup(FaceGroupDo faceGroupDo) {
        this.updateById(faceGroupDo);
    }

    @Override
    public Result<?> deleteFaceGroup(String id) {
        FaceGroupDo faceGroupDo = this.getById(id);
        if (faceGroupDo == null) return Result.error("组不存在");
        // 删除组下的所有图片
        String delPath = faceConfigProperties.getBaseFacePath() + faceGroupDo.getGroupPath();
        boolean del = FileUtil.del(delPath);
        if (!del) {
            return Result.error("删除文件失败");
        }
        // 删除数据库中的记录
        FaceDetailService faceDetailService = SpringContextUtils.getBean(FaceDetailService.class);
        faceDetailService.removeBatchByIds(
                faceDetailService.listObjs(
                        Wrappers.lambdaQuery(FaceDetailDo.class)
                                .eq(FaceDetailDo::getFaceGroup, id),
                        Object::toString
                )
        );

        // 删除组信息
        this.removeById(id);

        // 删除百度人脸库
        if (SysParameterUtil.isBaiduFaceEnabled()) {
            Result<?> response = userGroupService.deleteUserGroup(faceGroupDo.getGroupId());
            if (!response.isSuccess()) return response;
        }
        return Result.OK("删除成功");


    }

    @Override
    public void initFaceGroup(String faceGroupId) {
        FaceGroupDo faceGroupDo = this.getById(faceGroupId);
        if (faceGroupDo != null) {
            String groupPath = faceConfigProperties.getBaseFacePath() + faceGroupDo.getGroupPath();
            // 写入 redis
            RedisUtil redisUtil = SpringContextUtils.getBean(RedisUtil.class);
            redisUtil.set("init_group_path", new String[]{groupPath});
            // 如果不是百度face, 重启 ai engine
            if (!SysParameterUtil.isBaiduFaceEnabled()) {
                CmdUti.restartAiEngine();
            }
        } else {
            throw new JeecgBootException("组不存在");
        }
    }

    @Override
    public FaceGroupDo getOrCreateDefaultGroup() {
        FaceGroupDo faceGroupDo = this.getOne(Wrappers.lambdaQuery(FaceGroupDo.class)
                .eq(FaceGroupDo::getGroupName, "默认人员组"), false);
        if (faceGroupDo == null) {
            String deviceRealSn = SysParameterUtil.getDeviceRealSn();
            if (deviceRealSn == null) {
                throw new JeecgBootException("未获取到设备编号");
            }
            // 创建默认组
            FaceGroupDo defaultFaceGroupDo = new FaceGroupDo();
            defaultFaceGroupDo.setGroupName("默认人员组");
            defaultFaceGroupDo.setGroupId(deviceRealSn);
            this.addFaceGroup(defaultFaceGroupDo);
            return defaultFaceGroupDo;
        }
        return faceGroupDo;
    }

    @Override
    public void initDefaultGroup() {
        getOrCreateDefaultGroup();
        // FaceGroupDo faceGroupDo = getOrCreateDefaultGroup();
        // initFaceGroup(faceGroupDo.getId());
    }

    @Override
    public Result<?> initGroup() {
        String deviceRealSn = SysParameterUtil.getDeviceRealSn();
        if (StrUtil.isBlank(deviceRealSn)) {
            return Result.error("获取设备SN失败");
        }
        FaceGroupDo faceGroupDo = new FaceGroupDo();
        faceGroupDo.setGroupName("默认人员组");
        faceGroupDo.setGroupId(deviceRealSn);
        addFaceGroup(faceGroupDo);
        return Result.OK("初始化组信息成功");
    }
}
