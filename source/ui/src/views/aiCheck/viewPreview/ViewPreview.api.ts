import {defHttp} from '/@/utils/http/axios';
import {IPageListData} from './ViewPreview.data';

enum Api {
  // 相机列表
  list = '/viewPreview/viewPreview/list',
  // 事件列表
  alarmList = '/alarmDetail/alarmDetail/list',
  // 数量列表
  alarmCount = '/alarmDetail/alarmDetail/count',

  // 相机分页
  pageListByCamera = '/viewPreview/viewPreview/pageListByCamera',

  // 播放flv
  showView = '/viewPreview/viewPreview/getVideoStream',
  getDeviceInfo = '/viewPreview/viewPreview/getDeviceInfo',

  //单图放大返回
  // backList = '/viewPreview/viewPreview/offProcess',
  backList = '/viewPreview/viewPreview/closeVideoStream',

  //进入事件详情
  alarmDetail = '/alarmDetail/alarmDetail/detailsByCamera',

  //事件数量
  alarmInfo = '/viewPreview/viewPreview/getAlarmCountByCamera',

  //审核结果
  alarmAudit = '/alarmAudit/alarmAudit/audit',

  getAlarmCount = '/viewPreview/viewPreview/getAlarmCountByCamera',

  enableAlgStream = "/viewPreview/viewPreview/enableAlgStream",

  // 视频流代理
  startStreamProxy = "/stream/server/addStreamProxy"
}

/**
 * 列表接口
 * @param params
 */
export const list = (params) => defHttp.get({url: Api.list, params});
export const getAlarmCount = (params) => defHttp.get({url: Api.getAlarmCount, params});
export const pageListByCamera = (params) => defHttp.get<IPageListData>({
  url: Api.pageListByCamera,
  params
});
//事件列表接口
export const alarmList = (params) => defHttp.get({url: Api.alarmList, params});
//事件总数
export const alarmCount = (params) => defHttp.get({url: Api.alarmCount, params});
export const showView = (params) => defHttp.get({url: Api.showView, params});
export const getDeviceInfo = (params) => defHttp.get({url: Api.getDeviceInfo, params});
//单图放大返回
export const backCameraList = (params) => defHttp.get({url: Api.backList, params});

//事件详情
export const detailsByCamera = (params) => defHttp.get({url: Api.alarmDetail, params});

//审核结果
export const auditsResult = (params) => defHttp.post({url: Api.alarmAudit, params});

//相机事件数量接口
export const alarmInfo = (params) => defHttp.get({url: Api.alarmInfo, params});

// 是否开启算法视频流预览
export const enableAlgStream = () => defHttp.get({url: Api.enableAlgStream});

// 开启流代理的方式播放视频流
export const startStreamProxy = (params) => defHttp.post({url: Api.startStreamProxy, params});

