import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { useGlobSetting } from '/@/hooks/setting';
const globSetting = useGlobSetting();
let baseUploadUrl = globSetting.uploadUrl;
if (window.location.host !== 'localhost:3100') {
  const index = window.location.host.indexOf(':');
  const hostIp = window.location.host.slice(0, index);
  baseUploadUrl = 'http://' + hostIp + ':8088/kyscene';
}
const { createConfirm } = useMessage();

enum Api {
  list = '/cameraFactory/cameraFactory/list',
  save = '/cameraFactory/cameraFactory/add',
  edit = '/cameraFactory/cameraFactory/edit',
  deleteOne = '/cameraFactory/cameraFactory/delete',
  deleteBatch = '/cameraFactory/cameraFactory/deleteBatch',
  importExcel = '/cameraFactory/cameraFactory/importExcel',
  exportXls = '/cameraFactory/cameraFactory/exportXls',
}
/**
 * 导出api
 * @param params
 */
export const getExportUrl = Api.exportXls;
/**
 * 导入api
 */
export const getImportUrl = `${baseUploadUrl}` + Api.importExcel;
/**
 * 列表接口
 * @param params
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 删除单个
 */
export const deleteOne = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteOne, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};
/**
 * 批量删除
 * @param params
 */
export const batchDelete = (params, handleSuccess) => {
  createConfirm({
    iconType: 'warning',
    title: '确认删除',
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, data: params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};
/**
 * 保存或者更新
 * @param params
 */
export const saveOrUpdate = (params, isUpdate) => {
  const url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({ url: url, params });
};
