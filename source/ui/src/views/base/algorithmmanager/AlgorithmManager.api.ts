import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createConfirm } = useMessage();

enum Api {
  list = '/algorithmmanager/algorithmManager/list',
  save = '/algorithmmanager/algorithmManager/add',
  edit = '/algorithmmanager/algorithmManager/edit',
  deleteOne = '/algorithmmanager/algorithmManager/delete',
  deleteBatch = '/algorithmmanager/algorithmManager/deleteBatch',
  importExcel = '/algorithmmanager/algorithmManager/importExcel',
  exportXls = '/algorithmmanager/algorithmManager/exportXls',
  cameraList = '/algorithmmanager/algorithmManager/cameraList',
  cameraTree = '/cameraManager/cameraManager/cameraTree',
  restarstPythone = '/algorithmmanager/algorithmManager/restarstPythone',
  cameraNums = '/algorithmmanager/algorithmManager/cameraNums',
  saveConfig = '/createConfig/saveJson/savaAll',
  cameraRunStaus = '/algorithmmanager/algorithmManager/setStatus',
  getcameraRunStaus = '/algorithmmanager/algorithmManager/getStatus',
  imgInfo = '/algorithmmanager/algorithmManager/getImgInfo',
  //授权相关
  algLicenseNum = '/cameraManager/cameraManager/getAlgLicenseNum',
  //得到算法阈值
  getThreshold = '/algorithmmanager/algorithmManager/getThresholdById',
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

export const imgInfo = (params) => defHttp.get({ url: Api.imgInfo, params });

export const cameraList = (params) => defHttp.get({ url: Api.cameraList, params });

export const cameraTree = () => defHttp.get({ url: Api.cameraTree });

export const cameraNums = () => defHttp.get({ url: Api.cameraNums });


export const saveConfig = (params) => defHttp.get({ url: Api.saveConfig, params });
export const restarstPythone = (handle) => 
      defHttp.get({ url: Api.restarstPythone })
      .then(()=>{
        handle();
      })
      .catch(() => {
        handle();
      });


export const setRunStatus = (params, handle) =>
  defHttp
    .get({ url: Api.cameraRunStaus, params })
    .then(() => {
      handle();
    })
    .catch(() => {
      handle();
    });

export const getRunStatus = (params) => defHttp.get({ url: Api.getcameraRunStaus, params });
/**
 * 删除单个
 */
export const deleteOne = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteOne, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};
/**
 * 批量删除
 * @param params
 */
export const batchDelete = (params, handleSuccess) => {
  createConfirm({
    iconType: 'warning',
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
 */
export const saveOrUpdate = (params, isUpdate) => {
  let url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({ url: url, params });
};

//得到授权相关
export const algLicenseNum = (params) => defHttp.get({ url: Api.algLicenseNum, params });

//得到算法阈值
export const getThreshold = (params) => defHttp.get({ url: Api.getThreshold, params });
