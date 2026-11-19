import {defHttp} from '/@/utils/http/axios';

enum API {
  listPage = '/serial485Reader/queryPage',
  add = '/serial485Reader/add',
  update = '/serial485Reader/edit',
  delete = '/serial485Reader/delete',
  getCommPorts = '/serial485Reader/getCommPorts',
  connectCommPort = '/serial485Reader/saveCommAndOpen',
  disconnectCommPort = '/serial485Reader/closeCommPort',
  getCommSetting = '/serial485Reader/getCommInfo',
  sendCommand = '/serial485Reader/sendCommand',

  realTimeData = '/thRealData/realData',
  historyData = '/thRealData/page',
  deleteHistoryData = '/thRealData/delete',
}

export const getSystemCommPorts = () => defHttp.get({url: API.getCommPorts})

export const connectCommPort = (params) => defHttp.post({url: API.connectCommPort, params})

export const disconnectCommPort = () => defHttp.get({url: API.disconnectCommPort})

export const getCommSetting = () => defHttp.get({url: API.getCommSetting})

export const listPage = (params) => defHttp.get({url: API.listPage, params})

export const addDevice = (params) => defHttp.post({url: API.add, params})

export const deleteDevice = (params) => defHttp.get({url: API.delete, params})

export const updateDevice = (params) => defHttp.post({url: API.update, params})

export const sendCommand = (params) => defHttp.get({url: API.sendCommand, params})

export const getTHRealTimeData = (params) => defHttp.get({url: API.realTimeData, params})

export const getTHHistoryData = (params) => defHttp.get({url: API.historyData, params})

export const deleteTHHistoryData = (params) => defHttp.get({url: API.deleteHistoryData, params})
