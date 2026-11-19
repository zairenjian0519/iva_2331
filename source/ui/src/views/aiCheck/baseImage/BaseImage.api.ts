import {defHttp} from '/@/utils/http/axios';

enum Api {
  upload ='/basicImage/upload',
  getPage = '/basicImage/page',
  delete = '/basicImage/delete',
  list = '/basicImage/list',
}

export const saveData = (params) => defHttp.post({url: Api.upload, headers: { 'Content-Type': 'multipart/form-data' }, params});

export const listPage = (params) => defHttp.get({url: Api.getPage, params});

export const deleteRecord = (params) => defHttp.get({ url: Api.delete, params });

export const getAllData = (params) => defHttp.get({ url: Api.list, params });
