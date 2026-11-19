import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/alarmDetail/alarmDetail/getFaceDetails',
  save = '/alarmDetails/alarmDetails/add',
  edit = '/alarmDetails/alarmDetails/edit',

  importExcel = '/alarmDetails/alarmDetails/importExcel',
  exportXls = '/alarmDetail/alarmDetail/exportXls',
  //审核结果
  alarmAudit = '/alarmAudit/alarmAudit/audit',
  //统计数据
  alarmCount = '/alarmDetail/alarmDetail/queryAlarmCount',

  /**
   * ==== 下面三个api用到了===========
   */
  getPageData = '/alarmFace/page',
  deleteOne = '/alarmFace/delete',
  deleteBatch = '/alarmFace/deleteBatch',
}


export const getPageData = (params: any) => defHttp.get({ url: Api.getPageData, params });

/**
 * 删除单个
 * @param params
 * @param handleSuccess
 */
export const deleteOne = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteOne, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  })
}

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
      })
    },
  })
}

/**
 * 列表接口
 * @param params
 */
export const list = (params) => defHttp.get({ url: Api.list, params });


/**
 * ================== 下面的api没有用到=====================
 */

/**
 * 导出api
 * @param params
 */
export const getExportUrl = (params) => defHttp.get({ url: Api.exportXls, params });

/**
 * 导入api
 */
export const getImportUrl = Api.importExcel;


export const getAlarmCount = (params) => defHttp.get({ url: Api.alarmCount, params });


//审核结果
export const auditsResult = (params, handleSuccess) => {
  return defHttp.post({ url: Api.alarmAudit, params }).then(() => {
    handleSuccess();
  });
};
