import { defHttp } from '/@/utils/http/axios';


enum Api {
  list = '/alarmStatistics/alarmStatistics/list',
  save = '/alarmStatistics/alarmStatistics/add',
  edit = '/alarmStatistics/alarmStatistics/edit',
  deleteOne = '/alarmStatistics/alarmStatistics/delete',
  deleteBatch = '/alarmStatistics/alarmStatistics/deleteBatch',
  importExcel = '/alarmStatistics/alarmStatistics/importExcel',
  exportXls = '/alarmStatistics/alarmStatistics/exportXls',
  statisticsList = '/alarmStatistics/alarmStatistics/statistics',
  //统计检索
  statisticsSearchApi = '/alarmStatistics/alarmStatistics/statistics',

  //五个部分的接口
  //part1-各类型总数
  statisticsMainCount = '/alarmStatistics/alarmStatistics/statisticsMainCount',
  //part1-各类型总数
  statisticsByCamera = '/alarmStatistics/alarmStatistics/statisticsByCamera',
  //part1-各类型总数
  statisticsByType = '/alarmStatistics/alarmStatistics/statisticsByType',
  //part1-各类型总数
  statisticsByDate = '/alarmStatistics/alarmStatistics/statisticsByDate',
  //part1-各类型总数
  statisticsByHour = '/alarmStatistics/alarmStatistics/statisticsByHour',
}

/**
 * 统计接口
 * @param params
 */
export const getStatistics = (params) => defHttp.get({ url: Api.statisticsList, params });
/**
 * 导出api
 * @param params
 */
export const getExportUrl = Api.exportXls;
/**
 * 导入api
 */
export const getImportUrl = Api.importExcel;
/**
 * 列表接口
 * @param params
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 统计检索
 * @param params
 */
export const statisticsSearch = (params) => defHttp.get({ url: Api.statisticsSearchApi, params });
//拆分五个部分的接口
export const getStatisticsMainCount = (params) => defHttp.get({ url: Api.statisticsMainCount, params });
export const getStatisticsByCamera = (params) => defHttp.get({ url: Api.statisticsByCamera, params });
export const getStatisticsByType = (params) => defHttp.get({ url: Api.statisticsByType, params });
export const getStatisticsByDate = (params) => defHttp.get({ url: Api.statisticsByDate, params });
export const getStatisticsByHour = (params) => defHttp.get({ url: Api.statisticsByHour, params });
