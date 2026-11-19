package org.jeecg.modules.kycategory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.kycategory.entity.KyCategory;
import org.jeecg.modules.kycategory.mapper.KyCategoryMapper;
import org.jeecg.modules.kycategory.service.IKyCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @Description: 分类数据
 * @Author: jeecg-boot
 * @Date:   2022-08-08
 * @Version: V1.0
 */
@Service
public class KyCategoryServiceImpl extends ServiceImpl<KyCategoryMapper, KyCategory> implements IKyCategoryService {
    @Autowired
    KyCategoryMapper kyCategoryMapper;

	@Override
	public void addKyCategory(KyCategory kyCategory) {
	   //新增时设置hasChild为0
	    kyCategory.setHasChild(IKyCategoryService.NOCHILD);
		if(oConvertUtils.isEmpty(kyCategory.getPid())){
			kyCategory.setPid(IKyCategoryService.ROOT_PID_VALUE);
		}else{
			//如果当前节点父ID不为空 则设置父节点的hasChildren 为1
			KyCategory parent = baseMapper.selectById(kyCategory.getPid());
			if(parent!=null && !"1".equals(parent.getHasChild())){
				parent.setHasChild("1");
				baseMapper.updateById(parent);
			}
		}
		baseMapper.insert(kyCategory);
	}
	
	@Override
	public void updateKyCategory(KyCategory kyCategory) {
		KyCategory entity = this.getById(kyCategory.getId());
		if(entity==null) {
			throw new JeecgBootException("未找到对应实体");
		}
		String old_pid = entity.getPid();
		String new_pid = kyCategory.getPid();
		if(!old_pid.equals(new_pid)) {
			updateOldParentNode(old_pid);
			if(oConvertUtils.isEmpty(new_pid)){
				kyCategory.setPid(IKyCategoryService.ROOT_PID_VALUE);
			}
			if(!IKyCategoryService.ROOT_PID_VALUE.equals(kyCategory.getPid())) {
				baseMapper.updateTreeNodeStatus(kyCategory.getPid(), IKyCategoryService.HASCHILD);
			}
		}
		baseMapper.updateById(kyCategory);
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteKyCategory(String id) throws JeecgBootException {
		//查询选中节点下所有子节点一并删除
        id = this.queryTreeChildIds(id);
        if(id.indexOf(",")>0) {
            StringBuffer sb = new StringBuffer();
            String[] idArr = id.split(",");
            for (String idVal : idArr) {
                if(idVal != null){
                    KyCategory kyCategory = this.getById(idVal);
                    if(kyCategory!=null) {
                        String pidVal = kyCategory.getPid();
                        //查询此节点上一级是否还有其他子节点
                        List<KyCategory> dataList = baseMapper.selectList(new QueryWrapper<KyCategory>().eq("pid", pidVal).notIn("id", Arrays.asList(idArr)));
                        boolean flag = (dataList == null || dataList.size() == 0) && !Arrays.asList(idArr).contains(pidVal) && !sb.toString().contains(pidVal);
                        if (flag) {
                            //如果当前节点原本有子节点 现在木有了，更新状态
                            sb.append(pidVal).append(",");
                        }
                    }
                }
            }
            //批量删除节点
            baseMapper.deleteBatchIds(Arrays.asList(idArr));
            //修改已无子节点的标识
            String[] pidArr = sb.toString().split(",");
            for(String pid : pidArr){
                this.updateOldParentNode(pid);
            }
        }else{
            KyCategory kyCategory = this.getById(id);
            if(kyCategory==null) {
                throw new JeecgBootException("未找到对应实体");
            }
            updateOldParentNode(kyCategory.getPid());
            baseMapper.deleteById(id);
        }
	}
	
	@Override
    public List<KyCategory> queryTreeListNoPage(QueryWrapper<KyCategory> queryWrapper) {
        List<KyCategory> dataList = baseMapper.selectList(queryWrapper);
        List<KyCategory> mapList = new ArrayList<>();
        for(KyCategory data : dataList){
            String pidVal = data.getPid();
            //递归查询子节点的根节点
            if(pidVal != null && !IKyCategoryService.NOCHILD.equals(pidVal)){
                KyCategory rootVal = this.getTreeRoot(pidVal);
                if(rootVal != null && !mapList.contains(rootVal)){
                    mapList.add(rootVal);
                }
            }else{
                if(!mapList.contains(data)){
                    mapList.add(data);
                }
            }
        }
        return mapList;
    }

    @Override
    public List<SelectTreeModel> queryListByCode(String parentCode, String cate) {
        String pid = ROOT_PID_VALUE;
        if (oConvertUtils.isNotEmpty(parentCode)) {
            LambdaQueryWrapper<KyCategory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(KyCategory::getPid, parentCode);
            queryWrapper.eq(KyCategory::getCategory, cate);
            List<KyCategory> list = baseMapper.selectList(queryWrapper);
            if (list == null || list.size() == 0) {
                throw new JeecgBootException("该编码【" + parentCode + "】不存在，请核实!");
            }
            if (list.size() > 1) {
                throw new JeecgBootException("该编码【" + parentCode + "】存在多个，请核实!");
            }
            pid = list.get(0).getId();
        }
        Map<String, String> query = new HashMap<>(1);
        query.put("category", cate);
        return baseMapper.queryListByPid(pid, query);
    }

    @Override
    public List<SelectTreeModel> queryListByPid(String pid) {
        if (oConvertUtils.isEmpty(pid)) {
            pid = ROOT_PID_VALUE;
        }
        return baseMapper.queryListByPid(pid,null);
    }

    @Override
    public String getAreaListByAreaId(String alarmArea) {
        return kyCategoryMapper.getAreaListByAreaId(alarmArea);
    }

    @Override
    public List<KyCategory> getTreeLocationById(String locationId) {
        return kyCategoryMapper.getTreeLocationById(locationId);
    }

    @Override
    public KyCategory getOneCode(String alarmType) {

        return this.baseMapper.getOneCode(alarmType);
    }

    /**
	 * 根据所传pid查询旧的父级节点的子节点并修改相应状态值
	 * @param pid
	 */
	private void updateOldParentNode(String pid) {
		if(!IKyCategoryService.ROOT_PID_VALUE.equals(pid)) {
			Long count = baseMapper.selectCount(new QueryWrapper<KyCategory>().eq("pid", pid));
			if(count==null || count<=1) {
				baseMapper.updateTreeNodeStatus(pid, IKyCategoryService.NOCHILD);
			}
		}
	}

	/**
     * 递归查询节点的根节点
     * @param pidVal
     * @return
     */
    private KyCategory getTreeRoot(String pidVal){
        KyCategory data =  baseMapper.selectById(pidVal);
        if(data != null && !IKyCategoryService.ROOT_PID_VALUE.equals(data.getPid())){
            return this.getTreeRoot(data.getPid());
        }else{
            return data;
        }
    }

    /**
     * 根据id查询所有子节点id
     * @param ids
     * @return
     */
    private String queryTreeChildIds(String ids) {
        //获取id数组
        String[] idArr = ids.split(",");
        StringBuffer sb = new StringBuffer();
        for (String pidVal : idArr) {
            if(pidVal != null){
                if(!sb.toString().contains(pidVal)){
                    if(sb.toString().length() > 0){
                        sb.append(",");
                    }
                    sb.append(pidVal);
                    this.getTreeChildIds(pidVal,sb);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 递归查询所有子节点
     * @param pidVal
     * @param sb
     * @return
     */
    private StringBuffer getTreeChildIds(String pidVal,StringBuffer sb){
        List<KyCategory> dataList = baseMapper.selectList(new QueryWrapper<KyCategory>().eq("pid", pidVal));
        if(dataList != null && dataList.size()>0){
            for(KyCategory tree : dataList) {
                if(!sb.toString().contains(tree.getId())){
                    sb.append(",").append(tree.getId());
                }
                this.getTreeChildIds(tree.getId(),sb);
            }
        }
        return sb;
    }

}
