import {defHttp} from '/@/utils/http/axios';

enum API {
  getSerialList = '/serial/getSerialPortList',
  getAlgorithmList = '/serial/getAlgorithmList',
  getSoundPillarVoices = '/soundPillar/listVoices',
  testSoundPillar = '/serial/testVoice',
  saveSettings = '/serial/saveSettings',
  getSettingList = '/serial/getSettingList',
  closeVoice = '/serial/closeVoice',
  deleteSetting = '/serial/deleteSetting'
}

// 获取系统串口号数组
export const getSystemSerialArr = () => defHttp.get({url: API.getSerialList})

// 获取算法列表
export const getAlgorithmList = () => defHttp.get({url: API.getAlgorithmList})

export const getSoundPillarVoices = () => defHttp.get({url: API.getSoundPillarVoices})

export const checkSoundPillar = (params) => defHttp.post({url: API.testSoundPillar, params})

export const closeVoice = (params) => defHttp.post({url: API.closeVoice, params})

export const saveSettings = (params) => defHttp.post({url: API.saveSettings, params})

export const listPage = (params) => defHttp.post({url: API.getSettingList, params})

export const deleteRecord = (params) => defHttp.get({url: API.deleteSetting, params})

