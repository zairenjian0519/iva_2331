import {defHttp} from '/@/utils/http/axios';

enum API {
  getPinSettingList = '/serialPin/getPinSettingTable',
  addPinSetting = '/serialPin/addPinSetting',
  editPinSetting = '/serialPin/editPinSetting',
  deletePinSetting = '/serialPin/deletePinSetting',
  list = '/serialPin/list',
  testPinVal = '/serialPin/testPin'
}

export const listPage = (params) => defHttp.post({url: API.getPinSettingList, params})

export const addPinSetting = (params) => defHttp.post({url: API.addPinSetting, params})

export const editPinSetting = (params) => defHttp.post({url: API.editPinSetting, params})

export const deletePinSetting = (params) => defHttp.get({url: API.deletePinSetting, params})

export const list = () => defHttp.get({url: API.list})

export const testPinVal = (params) => defHttp.get({url: API.testPinVal, params})


