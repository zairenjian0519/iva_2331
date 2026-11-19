import {defHttp} from '/@/utils/http/axios';

enum Api {
  List = '/faceGroup/list',
  Add = '/faceGroup/add',
  Update = '/faceGroup/update',
  Delete = '/faceGroup/delete',
  Get = '/faceGroup/get',
  InitFaceGroup = '/faceGroup/initFaceGroup',
  InitDefaultGroup = '/faceGroup/initGroup',
}

export const getList = () => defHttp.get({url: Api.List});

export const getObj = (params: any) => defHttp.get({url: Api.Get, params: params});

export const add = (params: any) => defHttp.post({url: Api.Add, data: params});

export const update = (params: any) => defHttp.post({url: Api.Update, data: params});

export const deleteRow = (params: any) => defHttp.get({url: Api.Delete, params: params});

export const initFaceGroup = (params: any) => defHttp.get({url: Api.InitFaceGroup, params: params});

export const initDefaultGroup = () => defHttp.get({url: Api.InitDefaultGroup});
