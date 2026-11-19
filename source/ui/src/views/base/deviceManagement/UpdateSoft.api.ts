import {defHttp} from '/@/utils/http/axios';
import type {UploadFileCallBack, UploadFileParams} from "/#/axios";

import {useGlobSetting} from '/@/hooks/setting';

const globSetting = useGlobSetting();
let baseUploadUrl = globSetting.uploadUrl;
if (window.location.host !== 'localhost:3100') {
  const index = window.location.host.indexOf(':');
  const hostIp = window.location.host.slice(0, index);
  baseUploadUrl = 'http://' + hostIp + ':8088/kyscene';
}

export const Api = {
  uploadSoftFile: '/deviceManagement/softUpdate/update',
  deleteSoftFile: '/deviceManagement/softUpdate/delete',
  publishSoftFile: '/deviceManagement/softUpdate/publish',
  withdraw: '/deviceManagement/softUpdate/withdraw',
  pageData: '/deviceManagement/softUpdate/page',
  getAllVersion: '/deviceManagement/softUpdate/version',
}

export const uploadUpdateFile = (params: UploadFileParams, callback?: UploadFileCallBack) => defHttp.uploadFile({
  onUploadProgress: (progressEvent: ProgressEvent) => {
    const percentCompleted = Math.round(
      (progressEvent.loaded * 100) / progressEvent.total
    )
    // 触发回调或更新 UI
    callback?.onProgress?.(percentCompleted, progressEvent);
  },
  url: baseUploadUrl + Api.uploadSoftFile
}, params, callback)

export const getPageData = (params: any) => defHttp.get({params, url: Api.pageData})

export const deleteSoftFile = (params: any) => defHttp.get({url: Api.deleteSoftFile, params})

export const publishSoftFile = (params: any) => defHttp.get({url: Api.publishSoftFile, params})

export const withdraw = (params: any) => defHttp.get({url: Api.withdraw, params})

export const getAllVersion = () => defHttp.get({url: Api.getAllVersion})
