import {defHttp} from '/@/utils/http/axios';

enum Api {
  list = '/network/list',
  update = '/network/update',
  checkIP = '/network/checkIP',
  checkIpReachable = '/network/checkIpReachable'
}


/**
 * 列表接口
 * @param params
 */
export const list = () => defHttp.get({url: Api.list});

/**
 * 修改
 */
export const update = (params) => defHttp.post({url: Api.update, params});

/**
 * 检查IP是否被占用
 * @param params
 */
export const checkIP = (params) => defHttp.post({url: Api.checkIP + "/" + params.ipAddress + "/" + params.networkCard});

/**
 * 检查网络是否可达
 * @param params
 */
export const checkIpReachable = (params) => defHttp.post({url: Api.checkIpReachable + "/" + params });
