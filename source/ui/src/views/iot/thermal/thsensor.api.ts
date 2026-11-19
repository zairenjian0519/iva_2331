import {defHttp} from '/@/utils/http/axios';

enum API {
  listPage = '/thermal/listPage',
  add = '/thermal/add',
  update = '/thermal/edit',
  delete = '/thermal/delete',
  getMeterTypeList = '/thermal/getMeterTypeList',

  lastRecord = '/thermalData/lastRecord',
  realTimeData = '/thermalData/realData',
  historyData = '/thermalData/page',
  deleteHistoryData = '/thermalData/delete',

}



export const getMeterTypeList = () => defHttp.get({url: API.getMeterTypeList})

export const listPage = (params) => defHttp.get({url: API.listPage, params})

export const addDevice = (params) => defHttp.post({url: API.add, params})

export const updateDevice = (params) => defHttp.post({url: API.update, params})

export const deleteDevice = (params) => defHttp.get({url: API.delete, params})

export const getTHRealTimeData = (params) => defHttp.get({url: API.realTimeData, params})

export const getLastThermalRecord = (params) => defHttp.get({url: API.lastRecord, params})

export const getTHHistoryData = (params) => defHttp.get({url: API.historyData, params})

export const deleteTHHistoryData = (params) => defHttp.get({url: API.deleteHistoryData, params})
