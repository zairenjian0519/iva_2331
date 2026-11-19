import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { useGlobSetting } from '/@/hooks/setting';
const globSetting = useGlobSetting();
let baseUploadUrl = globSetting.uploadUrl;
if (window.location.host !== 'localhost:3100') {
  const index = window.location.host.indexOf(':');
  const hostIp = window.location.host.slice(0, index);
  baseUploadUrl = 'http://' + hostIp + ':8088/kyscene';
}
const { createConfirm } = useMessage();

enum Api {
  list = '/cameraManager/cameraManager/list',
  save = '/cameraManager/cameraManager/add',
  edit = '/cameraManager/cameraManager/edit',
  deleteOne = '/cameraManager/cameraManager/delete',
  deleteBatch = '/cameraManager/cameraManager/deleteBatch',
  importExcel = '/cameraManager/cameraManager/importExcel',
  exportXls = '/cameraManager/cameraManager/exportXls',
  cameraTree = '/cameraManager/cameraManager/cameraGroup',
  cameraManager = '/cameraManager/cameraManager/cameraTree',
  checkLicense = '/cameraManager/cameraManager/checkLicense',
  queryByDeviceId = '/cameraFactory/cameraFactory/queryById',
  savaAllByDev = '/createConfig/saveJson/savaAllByDev',
  onLineCount = '/cameraManager/cameraManager/getOnLineCameraCount',
  checkCameraName = '/cameraManager/cameraManager/checkCameraName',
  checkCameraIp = '/cameraManager/cameraManager/checkCameraIp',
  algLicenseNum = '/cameraManager/cameraManager/getAlgLicenseNum',
  saveOrUpdateBatch = '/cameraManager/cameraManager/addNvrCameras',
  getDictItemsByCode = '/sys/dict/getDictItemsByCode',
  checkCameraStatus = '/cameraManager/cameraManager/checkCameraStatus',
  restartAi = '/cameraManager/cameraManager/restartAiServer',
  FaceAlgSwitch = '/cameraManager/cameraManager/faceAlgSwitch',
  FaceAlgStatus = '/cameraManager/cameraManager/getFaceAlgStatus',
  SaveFaceGroup = '/cameraManager/cameraManager/saveFaceGroup',
  GetById = '/cameraManager/cameraManager/getById',
}

export const cameraTree = () => defHttp.get({ url: Api.cameraTree });
export const cameraManager = () => defHttp.get({ url: Api.cameraManager });
/**
 * 根据ID获取数据
 * */
export const checkLicense = (params) => defHttp.get({ url: Api.checkLicense, params: params });

/**
 * 根据ID获取数据
 * */
export const savaAllByDev = (params) => defHttp.get({ url: Api.savaAllByDev, params: params });

/**
 * 根据ID获取模板数据
 * */
export const queryByDeviceId = (params) => defHttp.get({ url: Api.queryByDeviceId, params: params });

export const checkCameraName = (params) => defHttp.get({ url: Api.checkCameraName, params: params });
export const checkCameraIp = (params) => defHttp.get({ url: Api.checkCameraIp, params: params });

/**
 * 导出api
 * @param params
 */
export const getExportUrl = Api.exportXls;
/**
 * 导入api
 */
export const getImportUrl = `${baseUploadUrl}` + Api.importExcel;

export const importXlsx = async (url, file, handleSuccess) => {
  return await defHttp.uploadFile({ url }, { file: file }, { success: handleSuccess });
};

export const exportXlsx = (params) => {
  return defHttp.get({ url: Api.exportXls, params: params, responseType: 'blob' }, { isTransformResponse: false });
};

/**
 * 列表接口
 * @param params
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 获取在线相机在线数
 * @param params
 */
export const onLineCount = (params) => defHttp.get({ url: Api.onLineCount, params });
/**
 * 可配算法的相机剩余数量
 * @param params
 */
export const getAlgLicenseNum = (params) => defHttp.get({ url: Api.algLicenseNum, params });

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
  const url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({ url: url, params });
};

export const saveOrUpdateBatch = (params) => {
  return defHttp.post({ url: Api.saveOrUpdateBatch, params });
};

export const getDictItemsByCode = (params) => {
  return defHttp.get({url: Api.getDictItemsByCode, params})
}

export const reconnectCamera = (params) => {
  return defHttp.post({ url: Api.checkCameraStatus, params });
};

export const restartAi = () => {
  return defHttp.get({url: Api.restartAi})
}

export const faceAlgSwitch = (params) => {
  return defHttp.get({ url: Api.FaceAlgSwitch, params });
};

export const faceAlgSwitchStatus = () => {
  return defHttp.get({ url: Api.FaceAlgStatus });
};

export const saveFaceGroup = (params: any) => {
  return defHttp.get({ url: Api.SaveFaceGroup, params });
};

export const getById = (params: any) => {
  return defHttp.get({ url: Api.GetById, params });
};
