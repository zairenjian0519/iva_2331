import {defHttp} from '/@/utils/http/axios';

enum Api {
  list = '/wireless/pillar/listAudioFiles',
  syncAudioFiles = '/wireless/pillar/syncAudioFiles',
  getAlgorithmList = '/wireless/pillar/getAlgorithmList',
  updateVoiceAlgorithm = '/wireless/pillar/updateVoiceAlgorithm',
  audioList = '/wireless/pillar/getAudioList',
  playAudioFile = '/wireless/pillar/playAudioFile',
  deleteById = '/wireless/pillar/deleteById',
}

export const listPage = (params) => defHttp.get({url: Api.list, params});

export const audioList = (params) => defHttp.get({url: Api.audioList, params});

export const getAudioFiles = (params) => defHttp.get({url: Api.syncAudioFiles, params});

export const getAlgorithmList = (params) => defHttp.get({url: Api.getAlgorithmList, params});

export const updateVoiceAlgorithm = (params) => defHttp.post({url: Api.updateVoiceAlgorithm, params});

export const playAudioFile = (params) => defHttp.post({url: Api.playAudioFile, params});

export const deleteRecord = (params) => defHttp.get({url: Api.deleteById, params});






