import {defHttp} from '/@/utils/http/axios'

export const Api = {
  getPage: '/elock/page',
  save: '/elock/add',
  edit: '/elock/edit',
  deleteById: '/elock/delete',
  realData: '/elock/realtime/data',
  historyData: '/elock/realtime/page',
  deleteHistory: '/elock/realtime/delete',
}

export const getPage = (params: any) => defHttp.get({ url: Api.getPage, params})

export const addDevice = (params: any) => defHttp.post({ url: Api.save, params})

export const updateDevice = (params: any) => defHttp.post({ url: Api.edit, params})

export const deleteDevice = (params: any) => defHttp.get({ url: Api.deleteById, params})

export const getRealData = (params: any) => defHttp.get({ url: Api.realData, params})

export const getHistoryData = (params: any) => defHttp.get({ url: Api.historyData, params})

export const deleteHistory = (params: any) => defHttp.get({ url: Api.deleteHistory, params})

