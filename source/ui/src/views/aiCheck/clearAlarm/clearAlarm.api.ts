import {defHttp} from '/@/utils/http/axios';

enum Api {
  save='/system/sysParameter/add',
  edit='/system/sysParameter/edit',
  getByCode='/system/sysParameter/getByCode',
  clearAlarmPlan='/alarmDetail/alarmDetail/clearAlarmPlan',
}

/**
 * 保存或者更新
 * @param params
 * @param isUpdate 是否是更新数据
 */
export const saveOrUpdate = (params, isUpdate) => {
  let url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({url: url, params});
}

/**
 * 通过code获取参数
 * @param params
 */
export const getByCode = (params) => {
  return defHttp.get({url: Api.getByCode, params});
}

/**
 * 按照规则清楚告警
 */
export const clearAlarmPlan = () => {
  return defHttp.get({url: Api.clearAlarmPlan});
}
