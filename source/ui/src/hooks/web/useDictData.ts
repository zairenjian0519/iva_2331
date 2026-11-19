import {initDictOptions} from "/@/utils/dict";
import {omit} from "lodash-es";

export function getDictData(): Promise<any>{
  return initDictData()
}

async function initDictData(): Promise<any> {
  //根据字典Code, 初始化字典数组
  const dictData = await initDictOptions('ky_category,name,code,category="0" and pid!="0"');
  return dictData.reduce((prev: any[], next: any) => {
    if (next) {
      const value = next['value'];
      prev.push({
        label: next['text'] || next['label'],
        value: value,
        ...omit(next, ['text', 'value']),
      });
    }
    return prev;
  }, []);
}


