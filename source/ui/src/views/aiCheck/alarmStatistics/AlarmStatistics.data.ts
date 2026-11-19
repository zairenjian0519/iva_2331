// 事件统计（日期）
export interface dateCount {
  date: string;
  value: number;
  remindValue: number;
  seriousValue: number;
  commonValue: number;
  warningValue: number;
}

//事件统计（日期）-（上一周期）
export interface lastDateCount {
  remindValue: number;
  seriousValue: number;
  commonValue: number;
  warningValue: number;
  value: number;
}

//事件统计图（相机）
export interface cameraCount {
  name: string;
  value: number;
}

//24小时事件态势图
export interface hourCount {
  hour: string;
  value: number;
}

//24小时事件态势图（上一周期）
export interface lastHourCount {
  value: string;
}

//已选
export interface searchConditionParams {
  cameraNames: '';
  categorys: '';
}

export interface typeCountList {
  name: string;
  value: number;
}

//区域列表
export interface locationCount {
  name: string;
  value: number;
}
