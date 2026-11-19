import { IRawCameraTree } from './BaseSearch.data';
import { defHttp } from '/@/utils/http/axios';
enum Api {
  //事件类型树
  alarmTree = '/kycategory/kyCategory/categoryTree',
  //相机选择树
  cameraTree = '/cameraManager/cameraManager/cameraTree',
  //相机工厂
  factoryList = 'cameraFactory/cameraFactory/factoryList',
}
export const alarmTreeData = (params) => defHttp.get({ url: Api.alarmTree, params });
//事件列表接口
export const cameraTreeData = (params) => defHttp.get<IRawCameraTree>({ url: Api.cameraTree, params });
//获取相机工厂
export const factoryList = () => defHttp.get({ url: Api.factoryList });
