export const WIDTH = 576;
export const TIMES = 24 * 60 - 1;
export const RATE = (TIMES + 1) / WIDTH;
//只有一个时间点，无结束时间
export const defaultnPoint = 360;
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

export interface points {
  tMType: number; // 时间分钟形式
  tHour?: number; // 时间H
  tMinute?: number; // 时间M
  strTime: string; // 开始时间字符形式
  width: number; // 宽度
  left: number; // 左偏移
  selected?: boolean; // 当前选中
}

export class PlanInfo {
  title = '';
  //时间点
  points: points[] = [];
  value = '';

  constructor(data: { title: string; value?: string }) {
    this.title = data.title;
    this.setValue(data.value);
  }

  onChange() {
    this.value = this.points.map((s) => s.strTime).join(',');
  }

  setValue(value?: string) {
    this.points.length = 0;
    if (value === '') {
      this.value = value;
      return;
    }

    // 新建时默认6点
    if (!value) {
      this.points.push({
        tMType: defaultnPoint,
        strTime: num2Str(defaultnPoint),
        width: WIDTH / 1440,
        left: defaultLeft,
      });
      this.value = '06:00';
      return;
    }
    this.value = value;
    for (const v of value.split(',')) {
      this.points.push({
        tMType: str2Num(v),
        strTime: v,
        width: WIDTH / 1440,
        left: (str2Num(v) / RATE) >> 0,
      });
    }
  }
}
