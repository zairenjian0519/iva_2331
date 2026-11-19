import {defHttp} from '/@/utils/http/axios';

enum Api {
  // 相机列表
  // syncDeviceInfo = '/wireless/pillar/updateSoundDeviceInfo',
  syncDeviceInfo = '/wireless/pillar/syncDevices',
  list = '/wireless/pillar/list',
  updateStatus = '/wireless/pillar/updateStatus',
  updateVolume = '/wireless/pillar/updateVolume',
  closePlayTask = '/wireless/pillar/closePlayTask',
  getAccountInfo = '/wireless/pillar/getAccountInfo',
  getProjectInfo = '/wireless/pillar/getProjectInfo',
  updateSoundPillar = '/wireless/pillar/updateSoundPillar',
}

export const sycDeviceInfo = (params) => defHttp.get({url: Api.syncDeviceInfo, params});

export const listPage = (params) => defHttp.get({url: Api.list, params});

export const updateStatus = (params) => defHttp.get({url: Api.updateStatus, params});

export const updateVolume = (params) => defHttp.get({url: Api.updateVolume, params});

export const closePlayTask = (params) => defHttp.get({url: Api.closePlayTask, params});

export const getAccountInfo = (params) => defHttp.get({url: Api.getAccountInfo, params});

export const getProjectInfo = (params) => defHttp.get({url: Api.getProjectInfo, params});

export const updateSoundPillar = (params) => defHttp.post({url: Api.updateSoundPillar, params});
