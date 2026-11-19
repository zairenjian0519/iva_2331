import { getAuthCache } from '/@/utils/auth';
import { DB_DICT_DATA_KEY } from '/@/enums/cacheEnum';
import { defHttp } from '/@/utils/http/axios';

/**
 * 从缓存中获取字典配置
 * @param code
 */
export const getDictItemsByCode = (code) => {
  if (getAuthCache(DB_DICT_DATA_KEY) && getAuthCache(DB_DICT_DATA_KEY)[code]) {
    return getAuthCache(DB_DICT_DATA_KEY)[code];
  }
};
/**
 * 获取字典数组
 * @param dictCode 字典Code
 * @return List<Map>
 */
export const initDictOptions = (code) => {
  let normalizedCode = code;
  try {
    normalizedCode = decodeURIComponent(code);
  } catch {
    // Keep the original code when it contains an invalid percent escape.
  }
  const isTableDict = normalizedCode.indexOf(',') > 0;

  // Table dictionaries are dynamic and must not reuse stale login cache values.
  if (!isTableDict && getDictItemsByCode(code)) {
    return new Promise((resolve, reject) => {
      resolve(getDictItemsByCode(code));
    });
  }
  //2.获取字典数组
  //update-begin-author:taoyan date:2022-6-21 for: 字典数据请求前将参数编码处理，但是不能直接编码，因为可能之前已经编码过了
  if (isTableDict) {
    // Encode the entire table dictionary path segment. This also normalizes
    // callers that previously applied encodeURI.
    code = encodeURIComponent(normalizedCode);
  }
  //update-end-author:taoyan date:2022-6-21 for: 字典数据请求前将参数编码处理，但是不能直接编码，因为可能之前已经编码过了
  return defHttp.get({ url: `/sys/dict/getDictItems/${code}` });
};
/**
 * 获取字典数组
 * @param code 字典Code
 * @param params 查询参数
 * @param options 查询配置
 * @return List<Map>
 */
export const ajaxGetDictItems = (code, params, options?) => defHttp.get({ url: `/sys/dict/getDictItems/${code}`, params }, options);
