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
  list = '/deviceManagement/deviceManagement/list',
  logList = '/devicelog/deviceLog/list',
  save = '/deviceManagement/deviceManagement/add',
  edit = '/deviceManagement/deviceManagement/edit',
  modifyIp = '/deviceManagement/deviceManagement/modifyIp',
  // devInfo = '/deviceManagement/deviceManagement/devInfo',
  devInfo = '/deviceManagement/deviceManagement/deviceInfo',
  verInfo = '/deviceManagement/deviceManagement/devVerInfo',
  setVerInfo = '/deviceManagement/deviceManagement/setDevVerInfo',
  deleteOne = '/deviceManagement/deviceManagement/delete',
  deleteBatch = '/deviceManagement/deviceManagement/deleteBatch',
  uploadAlgFile = '/deviceManagement/deviceManagement/uploadAlgFile',
  updateAlg = '/deviceManagement/deviceManagement/updateAlg',
  reboot = '/deviceManagement/deviceManagement/reboot',
  rebootDM = '/deviceManagement/deviceManagement/rebootDM',
  savaByDev = '/createConfig/saveJson/savaAllByDev',
  exportImg = '/deviceManagement/deviceManagement/exportImg',
  machineId = '/deviceManagement/deviceManagement/getMachineId',
  deviceSn = '/deviceManagement/deviceManagement/getDeviceSn',
  authInfo = '/deviceManagement/deviceManagement/getAuthInfo',
  upLicense = '/deviceManagement/deviceManagement/uploadLicense',
  algLog = '/devicelog/deviceLog/algLog',
  // configSyncUrl = '/deviceManagement/deviceManagement/configSync',
  //编辑组
  changeStatus = '/deviceManagement/deviceManagement/changeStatus',
  getProjectKey = '/deviceManagement/deviceManagement/getProjectKey',
  restartMqtt = '/deviceManagement/deviceManagement/restartCustomMqtt'
}

export const uploadUrl = `${baseUploadUrl}` + Api.uploadAlgFile;

export const updateAlg = (params, handelFun, handelError) => {
  return defHttp
    .get({ url: Api.updateAlg, params })
    .then(() => handelFun())
    .catch(() => handelError());
};
export const setAIVer = (params, handelFun, handelError) => {
  return defHttp
    .get({ url: Api.setVerInfo, params })
    .then(() => handelFun())
    .catch(() => handelError());
};
export const uploadLicense = (params, handelFun, handelError) => {
  return defHttp
    .get({ url: Api.upLicense, params })
    .then(() => handelFun())
    .catch(() => handelError());
};
/**
 * 列表接口
 * @param params
 */
export const list = (params) => defHttp.get({ url: Api.list, params });
// export const configSync = (params) => defHttp.post({ url: Api.configSyncUrl, params });

export const logList = (params) => defHttp.get({ url: Api.logList, params });

/**
 * 获取设备运行信息
 * */
export const getDevInfo = () => defHttp.get({ url: Api.devInfo });
export const getMachineId = (params) => defHttp.get({ url: Api.machineId, params });
export const getDeviceSn = () => defHttp.get({ url: Api.deviceSn });
export const getAuthInfo = (params) => defHttp.get({ url: Api.authInfo, params });

export const getVerInfo = (params) => defHttp.get({ url: Api.verInfo, params });

export const rebootDev = (params, handelOK) => defHttp.get({ url: Api.reboot, params }).then(() => handelOK());
export const rebootDM = (params) => defHttp.get({ url: Api.rebootDM, params });
export const saveByDev = (params) => defHttp.get({ url: Api.savaByDev, params });
export const changeStatus = (params) => defHttp.post({ url: Api.changeStatus, params });
export const exportImg = (params) => {
  return defHttp.get({ url: Api.exportImg, params: params }, { isTransformResponse: false });
};

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
export const saveUpdate = (item) => {
  return defHttp.post({ url: Api.modifyIp, params:{host_ip:item} });
};

//获取升级信息
export const upgradeInfo = (params) => {
  return defHttp.post({ url: Api.algLog, params });
};

export const projectKey = () => {
  return defHttp.get({ url: Api.getProjectKey });
};

export const restartMqtt = () => {
  return defHttp.get({ url: Api.restartMqtt });
};
