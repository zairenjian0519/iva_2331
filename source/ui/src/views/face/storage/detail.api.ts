import {defHttp} from '/@/utils/http/axios';
import type {UploadFileCallBack} from "/#/axios";

enum Api {
  Add = '/faceDetail/add',
  Update = '/faceDetail/update',
  Delete = '/faceDetail/delete',
  BatchDelete = '/faceDetail/batchDelete',
  ListByGroupId = '/faceDetail/getByGroupId',
  DeleteImg = '/faceDetail/deleteImg',
  ImportExcel = '/faceDetail/importExcel',
  ImportSingleFace = '/faceDetail/importSingleFace',
  BatchImportFace = '/faceDetail/batchImportFace',
  SyncBaiduYun = '/faceDetail/syncBaiduYun',
}

export const faceDetailAdd = (params: any) => defHttp.uploadFile({url: Api.Add}, params);

export const faceDetailDelete = (params: any) => defHttp.get({url: Api.Delete, params});

export const faceDetailBatchDelete = (params: any) => defHttp.get({url: Api.BatchDelete, params});

export const faceDetailUpdate = (params: any) => defHttp.uploadFile({url: Api.Update}, params);

export const deleteLocalImg = (params: any) => defHttp.get({url: Api.DeleteImg, params});

export const listByGroupId = (params: any) => defHttp.get({url: Api.ListByGroupId, params});

export const importExcel = (params: any, callback: UploadFileCallBack) => defHttp.uploadFile({url: Api.ImportExcel}, params, callback);

export const importSingleFace = (params) => defHttp.get({ url: Api.ImportSingleFace, params });

export const batchImportFace = (params) => defHttp.get({ url: Api.BatchImportFace, params });

export const syncBaiduYun = (params) => defHttp.get({ url: Api.SyncBaiduYun, params });
