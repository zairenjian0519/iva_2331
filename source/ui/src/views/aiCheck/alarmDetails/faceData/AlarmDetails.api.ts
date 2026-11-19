import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/alarmDetail/alarmDetail/getFaceDetails',
  save = '/alarmDetails/alarmDetails/add',
  edit = '/alarmDetails/alarmDetails/edit',
  deleteOne = '/alarmDetail/alarmDetail/delete',
  deleteBatch = '/alarmDetail/alarmDetail/deleteBatch',
  importExcel = '/alarmDetails/alarmDetails/importExcel',
  exportXls = '/alarmDetail/alarmDetail/exportXls',
  //审核结果
  alarmAudit = '/alarmAudit/alarmAudit/audit',
  //统计数据
  alarmCount = '/alarmDetail/alarmDetail/queryAlarmCount',
}

/**
 * 导出api
 * @param params
 */
export const getExportUrl = (params) => defHttp.get({ url: Api.exportXls, params });

/**
 * 导入api
 */
export const getImportUrl = Api.importExcel;

/**
 * 列表接口
 * @param params
 */
export const list = (params) => defHttp.get({ url: Api.list, params });
export const getAlarmCount = (params) => defHttp.get({ url: Api.alarmCount, params });

/**
 * 删除单个
 * @param params
 * @param handleSuccess
 */
export const deleteOne = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteOne, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};
/**
 * 批量删除
 * @param params
 * @param handleSuccess
 */
export const batchDelete = (params, handleSuccess) => {
  Modal.confirm({
    title: '确认删除',
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      return defHttp.delete({ url: Api.deleteBatch, data: params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};

//审核结果
export const auditsResult = (params, handleSuccess) => {
  return defHttp.post({ url: Api.alarmAudit, params }).then(() => {
    handleSuccess();
  });
};
