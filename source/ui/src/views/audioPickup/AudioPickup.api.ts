import {defHttp} from '/@/utils/http/axios';

enum Api {
  pageList = '/audioRecoder/list',
  save = '/audioRecoder/save',
  update = '/audioRecoder/edit',
  delete = '/audioRecoder/delete',
  start = '/audioRecoder/start',
  stop = '/audioRecoder/stop',
  fileList = '/audioRecoder/getAudioFileList',
  deleteFile = '/audioRecoder/deleteByPath',
  stopAll  = '/audioRecoder/stopAll',
}

export const listPage = (params) => defHttp.get({url: Api.pageList, params});

export const save = (params) => defHttp.post({url: Api.save, params});

export const update = (params) => defHttp.post({url: Api.update, params});

export const deleteData = (params) => defHttp.get({url: Api.delete, params});

export const start = (params) => defHttp.get({url: Api.start, params});

export const stop = (params) => defHttp.get({url: Api.stop, params});

export const getFileList = (params) => defHttp.get({url: Api.fileList, params});

export const deleteFile = (params) => defHttp.get({url: Api.deleteFile, params});

export const stopAll = () => defHttp.get({url: Api.stopAll});
