import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { render } from '/@/utils/common/renderUtils';
//列表数据
export const columns: BasicColumn[] = [
  {
    title: '算法名称',
    align: 'center',
    dataIndex: 'modelName_dictText',
  },
  {
    title: '任务备注',
    align: 'center',
    dataIndex: 'taskRemark',
  },
  {
    title: '所属相机',
    align: 'center',
    dataIndex: 'belongTo_dictText',
  },
  {
    title: '检测区域',
    align: 'center',
    dataIndex: 'checkArea',
  },
  {
    title: '生效日期',
    align: 'center',
    dataIndex: 'checkDate',
  },
  {
    title: '检测时间',
    align: 'center',
    dataIndex: 'checkTime',
  },
  {
    title: '检测周期',
    align: 'center',
    dataIndex: 'checkCycle',
  },
  {
    title: '阈值',
    align: 'center',
    dataIndex: 'threshold',
  },
  {
    title: '命中次数',
    align: 'center',
    dataIndex: 'hitCount',
  },
  {
    title: '事件推送间隔',
    align: 'center',
    dataIndex: 'alarmCycle',
  },
  {
    title: '事件等级',
    align: 'center',
    dataIndex: 'alarmLevel_dictText',
  },
  {
    title: '事件类型',
    align: 'center',
    dataIndex: 'alarmType_dictText',
  },
  {
    title: '运行状态',
    align: 'center',
    dataIndex: 'runStatus',
    customRender: ({ text }) => {
      return render.renderSwitch(text, [
        { text: '是', value: 'Y' },
        { text: '否', value: 'N' },
      ]);
    },
  },
  {
    title: '误报灵敏度',
    align: 'center',
    dataIndex: 'sensitivity',
  },
  {
    title: '时间持续间隔',
    align: 'center',
    dataIndex: 'timeSpace',
  },
];
//查询数据
export const searchFormSchema: FormSchema[] = [];
//表单数据
export const formSchema: FormSchema[] = [
  {
    label: '算法名称',
    field: 'modelName',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'algorithm_factory,algorithm_name,algorithm_name',
      showChooseOption: false,
    },
  },
  {
    label: '任务备注',
    field: 'taskRemark',
    component: 'Input',
  },
  {
    label: '所属相机',
    field: 'belongTo',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'camera_manager,camera_name,camera_name',
      showChooseOption: false,
    },
  },
  {
    label: '检测区域',
    field: 'checkArea',
    component: 'Input',
  },
  {
    label: '生效日期',
    field: 'checkDate',
    component: 'Input',
  },
  {
    label: '检测时间',
    field: 'checkTime',
    component: 'Input',
  },
  {
    label: '检测周期',
    field: 'checkCycle',
    component: 'InputNumber',
  },
  {
    label: '阈值',
    field: 'threshold',
    component: 'InputNumber',
  },
  {
    label: '命中次数',
    field: 'hitCount',
    component: 'InputNumber',
  },
  {
    label: '事件推送间隔',
    field: 'alarmCycle',
    component: 'InputNumber',
  },
  {
    label: '事件等级',
    field: 'alarmLevel',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'alarm_level',
    },
  },
  {
    label: '事件类型',
    field: 'alarmType',
    component: 'JSelectMultiple',
    componentProps: {
      dictCode: 'ky_category,name,id,category="0"',
    },
  },
  {
    label: '运行状态',
    field: 'runStatus',
    component: 'JSwitch',
    componentProps: {},
  },
  {
    label: '误报灵敏度',
    field: 'sensitivity',
    component: 'InputNumber',
  },
  {
    label: '时间持续间隔',
    field: 'timeSpace',
    component: 'InputNumber',
  },
  // TODO 主键隐藏字段，目前写死为ID
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
];

export type CheckPoint = [number, number];

export type CheckArea = CheckPoint[];

export type CanvasPoint = { x: number; y: number };

export type CanvasArea = CanvasPoint[];

export enum ROIAreaType {
  Full = 0,
  Rect,
  Polygon,
  InvalidArea,
}

export interface licenseObj {
  allNum: number;
  canUseNum: number;
  usedNum: number;
}
