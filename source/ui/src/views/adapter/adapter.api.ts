import {defHttp} from '/@/utils/http/axios'
import {useGlobSetting} from "/@/hooks/setting";
import type {UploadFileParams} from "/#/axios";

const globSetting = useGlobSetting();
const locationHost = window.location.host
let baseUploadUrl = globSetting.domainUrl;
if (locationHost.indexOf('3100') < 0 ) {
  const index = window.location.host.indexOf(':');
  const hostIp = window.location.host.slice(0, index);
  baseUploadUrl = 'http://' + hostIp + ':8088/kyscene';
}

export const Api = {
  uploadFile: '/mainFile/upload',
  getPage: '/mainFile/tableData',
  deleteFile: '/mainFile/deleteById',
  applyFileById: '/mainFile/applyFileById',
  getClassPage: '/algAdapter/algorithmClsReg/page',
  updateClassName: '/algAdapter/algorithmClsReg/update',
  addClassName: '/algAdapter/algorithmClsReg/add',
  deleteClass: '/algAdapter/algorithmClsReg/delete',
  getMethodPage: '/algAdapter/algorithmMethodReg/page',
  getAlgorithmList: '/algAdapter/algorithmMethodReg/getAlgorithmList',
  getClassNameList: '/algAdapter/algorithmMethodReg/getClassNameList',
  registerAlgorithm: '/algAdapter/algorithmMethodReg/add',
  updateMethodTable: '/algAdapter/algorithmMethodReg/update',
  deleteMethod: '/algAdapter/algorithmMethodReg/delete',

  initAiEngine: '/algAdapter/algorithmMethodReg/initAiEngine',
}


export const uploadModelFile = (params: UploadFileParams, onUploadProgress?: (progressEvent: ProgressEvent) => void) => defHttp.uploadFile({
  onUploadProgress,
  url: baseUploadUrl + Api.uploadFile + "/model"
}, params, { isReturnResponse: true })

export const uploadMainFile = (params: UploadFileParams, onUploadProgress?: (progressEvent: ProgressEvent) => void) => defHttp.uploadFile({
  onUploadProgress,
  url: baseUploadUrl + Api.uploadFile + "/main"
}, params, { isReturnResponse: true })

export const getPage = (params: any) => defHttp.get({ url: Api.getPage, params})

export const deleteFileById = (params: any) => defHttp.get({ url: Api.deleteFile, params})

export const applyFileById = (params: any) => defHttp.get({url: Api.applyFileById, params});

export const getClassNamePage = (params: any) => defHttp.get({ url: Api.getClassPage, params})

export const updateClass = (params: any) => defHttp.post({ url: Api.updateClassName, params})

export const addClass = (params: any) => defHttp.post({ url: Api.addClassName, params})

export const deleteClass = (params: any) => defHttp.get({ url: Api.deleteClass, params})

export const getMethodPage = (params: any) => defHttp.get({ url: Api.getMethodPage, params})

export const getAlgorithmList = (params: any) => defHttp.get({ url: Api.getAlgorithmList, params})

export const getClassNameList = (params: any) => defHttp.get({ url: Api.getClassNameList, params})

export const registerAlgorithm = (params: any) => defHttp.post({ url: Api.registerAlgorithm, params})

export const updateMethodTable = (params: any) => defHttp.post({ url: Api.updateMethodTable, params})

export const deleteMethod = (params: any) => defHttp.get({ url: Api.deleteMethod, params})

export const initAiEngine = () => defHttp.get({ url: Api.initAiEngine})


