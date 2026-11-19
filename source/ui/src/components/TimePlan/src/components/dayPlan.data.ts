export const WIDTH = 576;
export const TIMES = 24 * 60 - 1;
export const RATE = (TIMES + 1) / WIDTH;
export const defaultnBgn = 360;
export const defaultnEnd = 1080;
export const defaultLeft = 144;

export function str2Num(s: string) {
  const time = s?.split(':');
  if (!time?.length) {
    throw new Error(`Timeplan parse error: ${time}`);
  }
  return parseInt(time[0]) * 60 + parseInt(time[1]);
}

export function num2Str(n: number) {
  if (!n) {
    return '00:00';
  }
  if (n >= TIMES) {
    return '23:59';
  }
  return (((n / 60) >> 0) + '').padStart(2, '0') + ':' + ((n % 60) + '').padStart(2, '0');
}

export interface Section {
  nBgn: number; // 开始时间分钟形式
  bgnH?: number; // 开始时间H
  bgnM?: number; // 开始时间M
  nEnd: number; // 结束时间分钟形式
  endH?: number; // 结束时间H
  endM?: number; // 结束时间M
  sBgn: string; // 开始时间字符形式
  sEnd: string; // 结束时间字符形式
  width: number; // 宽度
  left: number; // 左偏移
  selected?: boolean; // 当前选中
}

export class PlanInfo {
  title = '';
  sections: Section[] = [];
  value = '';

  constructor(data: { title: string; value?: string }) {
    this.title = data.title;
    this.setValue(data.value);
  }

  onChange() {
    this.value = this.sections.map((s) => s.sBgn + '-' + s.sEnd).join(',');
  }

  setValue(value?: string) {
    this.sections.length = 0;
    if (value === '') {
      this.value = value;
      return;
    }

    // 新建时默认6点到下午18点
    if (!value) {
      this.sections.push({
        nBgn: defaultnBgn,
        nEnd: defaultnEnd,
        sBgn: num2Str(defaultnBgn),
        sEnd: num2Str(defaultnEnd),
        width: WIDTH / 2,
        left: defaultLeft,
      });
      this.value = '06:00-18:00';
      return;
    }
    this.value = value;

    // 解析00:00-06:00,21:00-23:59
    for (const v of value.split(',')) {
      const section = v.split('-');
      if (!section[0] || !section[1]) {
        continue;
      }
      const nBgn = str2Num(section[0]);
      const nEnd = str2Num(section[1]);
      this.sections.push({
        nBgn,
        nEnd,
        sBgn: section[0],
        sEnd: section[1],
        width: ((nEnd - nBgn) / RATE) >> 0,
        left: (nBgn / RATE) >> 0,
      });
    }
  }
}
