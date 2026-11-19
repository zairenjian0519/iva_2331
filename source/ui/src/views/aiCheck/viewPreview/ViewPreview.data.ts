export type mainTabs = {
  label: string;
  index: number;
  isAlarm: boolean;
};
// type alarmType = 'common' | 'remind' | 'serious' | 'warning';
export type alarmDetail = {
  alarmAreaDict: string;
  alarmLevel: string;
  alarmSubtype: string;
  alarmTime: string;
  alarmTypeDict: string;
  cameraName: string;
  createTime: string;
  detectObjs: string;
  id: string;
  picPath: string;
  remarks: string;
  stream: string;
  thumbnailPath: string;
};
type treeCategory = 'all' | 'device' | 'area';
export type treeTabs = {
  label: string;
  index: number;
  type: treeCategory;
};

export type viewDetailInfo = {
  cameraAlias: string;
  cameraName: string;
  cameraOwnership: string;
  cameraStatus: string;
  cameraType: string;
  createBy: string;
  createTime: string;
  factory: string;
  id: string;
  ip: string;
  location: string;
  password: string;
  pic: string;
  port: number;
  remark: string;
  runStatus: string;
  streamAddress: string;
  streamFormat: string;
  streamType: string;
  username: string;
  workType: string;
  seriousCameraCount: number;
  allCameraCount: number;
};
export interface enlargeMsg {
  // 图片url
  pic: string;
  // 名称
  cameraName: string;
  // 事件总数
  allCameraCount: number;
  // 严重事件数
  seriousCameraCount: number;
  // 当前设备
  cameraOwnership: string;
  // 抓拍地点
  location: string;
  // 当前算法
  alarmType: string;
  // 算法对应的名称
  alarmTypeDict: string;
  // 地点对应的名称
  locationDict: string;
  // 所属设备对应的名称
  deviceDict: string;
  streamAddress: string;
  id: string;
  factory: string;
  streamType: string;
}
export interface vertifyDetail {
  alarmArea: string;
  alarmLevel: string;
  alarmSubtype: string;
  alarmTime: string;
  alarmType: string;
  auditPeople: string;
  auditReason: string;
  auditResult: string;
  auditStatus: string;
  auditTime: string;
  cameraName: string;
  createTime: string;
  detectObjs: string;
  id: string;
  picPath: string;
  remarks: null;
  stream: string;
  thumbnailPath: string;
}

export interface parentMsg {
  id: string;
  parent: string;
  isAlarm: boolean;
  type: string;
  isRootNode: boolean;
}

export interface IPageListData {
  byLocationList: ICameraLocationData[];
  byOwnershipList: ICameraOwnershipData[];
}

export interface ICameraLocationData {
  cameraLocationDict: string;
  location: string;
  id: string;
  cameraLocationList: ICameraData[];
}

export interface ICameraOwnershipData {
  cameraOwnershipDict: string;
  id: string;
  cameraOwnershipList: ICameraData[];
}

export interface ICameraData {
  allCameraCount: number; // 12
  cameraAlias: string; // "相机"
  cameraName: string; // "相机001"
  cameraOwnership: string; // "AIAAA"
  cameraStatus: string; // "1"
  cameraType: string; // "type1"
  createBy: string; // "admin"
  createTime: string; // "2022-08-05 15:51:39"
  factory: string; // ""
  id: string; // "1555461517623857153"
  ip: string; // "192.168.1.83"
  location: string; // "固体车间"
  password: string; // "123456"
  pic: string; // "temp/CameraA001_1661235002582.png"
  port: number; // 554
  remark: string; // ""
  runStatus: string; // "1"
  seriousCameraCount: number; // 6
  streamAddress: string; // "rtsp://admin:123456789a@192.168.1.83:554"
  streamFormat: string; // ""
  streamType: string; // "1"
  username: string; // "admin"
  workType: string; // "0"
  channel: string;
  deviceIp: string; //设备ip
}
export interface alarmMsg {
  allCount: number;
  cameraId: string;
  seriousCount: number;
}

export interface params {
  cameraNames: string[];
  ids: string[];
  label: string[];
}
