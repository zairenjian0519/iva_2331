package org.jeecg.modules.kycategory.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.kycategory.entity.KyCategory;

import java.util.List;

/**
 * @Description: 分类数据
 * @Author: jeecg-boot
 * @Date: 2022-08-08
 * @Version: V1.0
 */
public interface IKyCategoryService extends IService<KyCategory> {

    /**
     * 根节点父ID的值
     */
    public static final String ROOT_PID_VALUE = "0";

    /**
     * 树节点有子节点状态值
     */
    public static final String HASCHILD = "1";

    /**
     * 树节点无子节点状态值
     */
    public static final String NOCHILD = "0";

    /**
     * 新增节点
     *
     * @param kyCategory
     */
    void addKyCategory(KyCategory kyCategory);

    /**
     * 修改节点
     *
     * @param kyCategory
     * @throws JeecgBootException
     */
    void updateKyCategory(KyCategory kyCategory) throws JeecgBootException;

    /**
     * 删除节点
     *
     * @param id
     * @throws JeecgBootException
     */
    void deleteKyCategory(String id) throws JeecgBootException;

    /**
     * 查询所有数据，无分页
     *
     * @param queryWrapper
     * @return List<KyCategory>
     */
    List<KyCategory> queryTreeListNoPage(QueryWrapper<KyCategory> queryWrapper);

    /**
     * 【vue3专用】根据父级编码加载分类字典的数据
     *
     * @param parentCode
     * @return
     */
    List<SelectTreeModel> queryListByCode(String parentCode, String cate);

    /**
     * 【vue3专用】根据pid查询子节点集合
     *
     * @param pid
     * @return
     */
    List<SelectTreeModel> queryListByPid(String pid);

    //根据自定义分组id查询所有父级 拼接显示
    String getAreaListByAreaId(String alarmArea);

    List<KyCategory> getTreeLocationById(String locationId);

    KyCategory getOneCode(String alarmType);

}
