import {defHttp} from '/@/utils/http/axios';

enum Api {
  pageData = '/dataPushHistory/page',

}

export const getPageData = (params) => defHttp.post({url: Api.pageData, params});
