import { defHttp } from '/@/utils/http/axios';
import { IRawCameraTree } from './playVideoList.data';

enum Api {
  // 播放flv
  showView = '/viewPreview/viewPreview/getVideoStream',

  //相机选择树
  cameraTree = '/cameraManager/cameraManager/cameraTree',
  //单图放大返回
  // backList = '/viewPreview/viewPreview/offProcess',
  backList = '/viewPreview/viewPreview/closeVideoStream',
}

export const showView = (params) => defHttp.get({ url: Api.showView, params });
//事件列表接口
export const cameraTreeData = (params) => defHttp.get<IRawCameraTree>({ url: Api.cameraTree, params });
//单图放大返回
export const backCameraList = (params) => defHttp.get({ url: Api.backList, params });
