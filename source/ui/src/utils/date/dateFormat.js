//处理时间格式
//获得年月日时分秒
//传入日期//例：2020-10-27T14:36:23
export const timeFormat = (time) => {
  let d = time ? new Date(time) : new Date();
  let year = d.getFullYear();
  let month = d.getMonth() + 1;
  let day = d.getDate();
  let hours = d.getHours();
  let min = d.getMinutes();
  let seconds = d.getSeconds();

  if (month < 10) month = '0' + month;
  if (day < 10) day = '0' + day;
  if (hours < 0) hours = '0' + hours;
  if (min < 10) min = '0' + min;
  if (seconds < 10) seconds = '0' + seconds;

  return year + '-' + month + '-' + day + ' ' + hours + ':' + min + ':' + seconds;
};
