import {defHttp} from '/@/utils/http/axios';

enum Api {
  save = '/dataPush/saveSetting',
  get = '/dataPush/getSetting',
  saveStreamSetting = '/dataPush/saveStreamSetting',
  getStreamSetting = '/dataPush/getStreamSetting',
}

export const save = (params) => {
  return defHttp.post({ url: Api.save, params });
};

export const get = () => defHttp.get({ url: Api.get});

export const saveStreamSetting = (params) => defHttp.post({ url: Api.saveStreamSetting, params});

export const getStreamSetting = () => defHttp.get({ url: Api.getStreamSetting});
