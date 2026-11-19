export interface tableItem {
  alarmTime: string;
  alarmType: string;
  cameraName: string;
  createTime: string;
  id: string;
  alarmId: string;
  picPath: string;
  algorithmRemark: string;
  thumbnailPath: string;
  cameraId: string;
  alarmTypeDict: string;
  cameraNumber: string;
  targetCount: number;
  isRotate: number;
  vectorId: string;
  faceName: string;
  facePhone: string;
  faceIdCard: string;
  imageNewName: string;
  imagePath: string;
  remark: string;
  faceGroup: string;
  groupName: string;
  groupPath: string;
  faceUrl: string;
}

// 表格头部常量
export const columnData = [
  {
    title: '相机名称',
    dataIndex: 'cameraName',
    key: 'cameraName',
    align: 'center',
  },

  {
    title: '抓拍时间',
    dataIndex: 'alarmTime',
    key: 'alarmTime',
    align: 'center',
    sorter: true,
  },
  {
    title: '人员姓名',
    dataIndex: 'faceName',
    key: 'faceName',
    align: 'center',
  },

  {
    title: '检测模式',
    dataIndex: 'isRotate',
    key: 'isRotate',
    align: 'center',
    customRender: function ({ text }) {
      if (text == 0) {
        return '实时';
      } else if (text === 1) {
        return '轮询';
        //为了不修改历史告警 之前的都是实时
      } else if (text == null) {
        return '实时';
      } else {
        return text;
      }
    },
  },
  {
    title: '操作',
    dataIndex: 'operation',
    key: 'operation',
    align: 'center',
    className: 'operation',
    width: '13%',
  },
];

// card类型
export interface cardData {
  cameraName: string; //相机名称
  alarmTime: string; //事件时间
  auditResult: string; //审核结果
  alarmLevel: string; //事件等级
  alarmAreaDict: string; //抓拍地点
  alarmTypeDict: string; //检测类型
  auditTime: string; //审核时间
  id: string;
  thumbnailPath: string; //缩略图
  targetCount: number;
  isRotate: number; //检测类型
}

export interface searchParams {
  cameraNames: string;
  categorys: string;
  endTime: string;
  startTime: string;
  ids: string;
  isRotate: number | null;
}

export enum AuditStatus {
  NoAudit = '0',
  Audited = '1',
}

export enum AuditResult {
  FAIL = '0',
  PASS = '1',
}

export enum AlarmLevel {
  All = 'all',
  Remind = 'remind',
  Common = 'common',
  Warning = 'warning',
  Serious = 'serious',
  Normal = 'normal',
}

export const AlarmLevelIndex = {
  [AlarmLevel.All]: '',
  [AlarmLevel.Remind]: '1',
  [AlarmLevel.Common]: '2',
  [AlarmLevel.Warning]: '3',
  [AlarmLevel.Serious]: '4',
  [AlarmLevel.Normal]: '0',
};

export const AlarmLevelI2V = {
  [AlarmLevelIndex[AlarmLevel.Remind]]: AlarmLevel.Remind,
  [AlarmLevelIndex[AlarmLevel.Common]]: AlarmLevel.Common,
  [AlarmLevelIndex[AlarmLevel.Warning]]: AlarmLevel.Warning,
  [AlarmLevelIndex[AlarmLevel.Serious]]: AlarmLevel.Serious,
  [AlarmLevelIndex[AlarmLevel.Normal]]: AlarmLevel.Normal,
};

//检测模式
export interface detectionMode {
  value: number;
  label: string;
}
