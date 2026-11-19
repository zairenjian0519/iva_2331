import {defHttp} from '/@/utils/http/axios';

enum API {
  getAlgorithmList = '/didoAlgorithm/getAlgorithmList',
  getTableData = '/didoAlgorithm/getTableData',
  addData = '/didoAlgorithm/addData',
  updateDate = '/didoAlgorithm/updateDate',
  deleteRow = '/didoAlgorithm/deleteRow',
}

// 获取算法列表
export const getAlgorithmList = () => defHttp.get({url: API.getAlgorithmList})

export const listPage = (params) => defHttp.post({url: API.getTableData, params})

export const addData = (params) => defHttp.post({url: API.addData, params})

export const updateDate = (params) => defHttp.post({url: API.updateDate, params})

export const deleteRow = (params) => defHttp.get({url: API.deleteRow, params})

