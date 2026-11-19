import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/cameraGroup/cameraGroup/list',
  save = '/cameraGroup/cameraGroup/add',
  edit = '/cameraGroup/cameraGroup/edit',
  // 修改相机
  editCamera = '/cameraGroup/cameraGroup/edit',
  //  改变组状态
  changeStatus = '/cameraGroup/cameraGroup/changeStatus',
  // 删除单个算法
  deleteAlg = '/cameraGroupAlgorithm/cameraGroupAlgorithm/delete',
  //删除轮询组
  deleteGroup = '/cameraGroup/cameraGroup/delete',

  deleteBatch = '/cameraGroup/cameraGroup/deleteBatch',
  importExcel = '/cameraGroup/cameraGroup/importExcel',
  exportXls = '/cameraGroup/cameraGroup/exportXls',
  //新建
  addGroup = '/cameraGroup/cameraGroup/add',
  //得到image
  imgInfo = '/algorithmmanager/algorithmManager/getImgInfo',
  //编辑ROI
  editROI = '/cameraGroup/cameraGroup/editRoi',
  //删除算法
  deleteOne = '/algorithmmanager/algorithmManager/delete',
  //配置下发
  saveGroupAll = 'createConfig/saveJson/saveGroupAll',
  //得到配置ROI后的区域
  getROI = '/cameraGroup/cameraGroup/getRoiById',
  //轮询配置
  addAlgorithm = '/cameraGroupAlgorithm/cameraGroupAlgorithm/add',
  //编辑算法
  editAlgorithm = '/cameraGroupAlgorithm/cameraGroupAlgorithm/edit',
  //得到相机组信息
  getGroupInfo = '/cameraGroup/cameraGroup/getGroupAlgInfo',
  //得到算法阈值
  getThreshold = '/algorithmmanager/algorithmManager/getThresholdById',

  getDevIpList = '/cameraGroup/cameraGroup/getDevIpList',
  //编辑组
  editGroup = '/cameraGroup/cameraGroup/editGroup',
}
/**
 * 导出api
 * @param params
 */
export const getExportUrl = Api.exportXls;
/**
 * 导入api
 */
export const getImportUrl = Api.importExcel;

/**
 * 列表接口
 * @param params
 */
export const list = (params) => defHttp.get({ url: Api.list, params });
/**
 * 根据组id获取设备ip列表
 * @param params
 */
export const getDevIpList = (params) => defHttp.get({ url: Api.getDevIpList, params });

/**
 * 删除单个算法
 * @param params
 * @param handleSuccess
 */
export const deleteOne = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteOne, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};
/**
 * 批量删除
 * @param params
 * @param handleSuccess
 */
export const batchDelete = (params, handleSuccess) => {
  Modal.confirm({
    title: '确认删除',
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, data: params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};
/**
 * 保存或者更新
 * @param params
 * @param isUpdate 是否是更新数据
 */
export const saveOrUpdate = (params, isUpdate) => {
  const url = isUpdate ? Api.editAlgorithm : Api.addAlgorithm;
  return defHttp.post({ url: url, params });
};

//新建
export const addGroup = (params) => defHttp.post({ url: Api.addGroup, params });

//getImage
export const imgInfo = (params) => defHttp.get({ url: Api.imgInfo, params });

//删除算法
export const deleteAlg = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteAlg, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

//编辑组
export const editGroup = (params) => defHttp.post({ url: Api.editGroup, params });

////删除组
export const deleteGroup = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteGroup, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

//改变状态
export const changeStatus = (params, handleSuccess) => {
  return defHttp.post({ url: Api.changeStatus, params }).finally(() => {
    handleSuccess();
  });
};

//修改相机
export const editCamera = (params) => defHttp.post({ url: Api.editCamera, params });

//算法ROI
export const editROI = (params) => defHttp.post({ url: Api.editROI, params });

//配置下发
export const saveGroupAll = (params) => defHttp.get({ url: Api.saveGroupAll, params });

//得到ROI区域
export const getROI = (params) => defHttp.get({ url: Api.getROI, params });

//轮询配置点完成
export const addAlgorithm = (params) => defHttp.post({ url: Api.addAlgorithm, params });
//算法编辑更新
export const editAlgorithm = (params) => defHttp.post({ url: Api.editAlgorithm, params });
//得到相机组信息
export const getGroupAlgInfo = (params) => defHttp.get({ url: Api.getGroupInfo, params });

//得到算法阈值
export const getThreshold = (params) => defHttp.get({ url: Api.getThreshold, params });
