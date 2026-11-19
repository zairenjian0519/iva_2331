import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
//列表数据
export const columns: BasicColumn[] = [
  {
    title: '名称',
    align: 'left',
    dataIndex: 'name',
  },
  // {
  //   title: '编码',
  //   align: 'center',
  //   dataIndex: 'code',
  // },
];
//查询数据
export const searchFormSchema: FormSchema[] = [];
//表单数据
export const areaFormSchema: FormSchema[] = [
  {
    label: '名称',
    field: 'name',
    component: 'Input',
    required: true,
  },
  {
    label: '编码',
    field: 'code',
    component: 'Input',
    // rules: [{ required: true, pattern: /^[0-9a-zA-Z]+$/, trigger: 'blur', message: '只允许英文和数字，不允许中文以及特殊符号！' }],
    show: false,
  },
  {
    label: '类别',
    field: 'pid',
    component: 'Input',
    show: false,
    // component: 'JTreeSelect',
    // componentProps: {
    //   dict: 'ky_category,name,id',
    //   pidField: 'pid',
    //   pidValue: '0',
    //   condition: '{"category": 1}',
    //   hasChildField: 'has_child',
    // },
  },
  // TODO 主键隐藏字段，目前写死为ID
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
];

export const alarmFormSchema: FormSchema[] = [
  {
    label: '名称',
    field: 'name',
    component: 'Input',
    required: true,
  },
  {
    label: '编码',
    field: 'code',
    component: 'Input',
    rules: [{ required: true, pattern: /^[0-9]+$/, trigger: 'blur', message: '只允许输入数字' }],
  },

  {
    label: '阈值',
    field: 'threshold',
    component: 'InputNumber',
    required: true,
    componentProps: {
      min: 0,
      max: 100,
      placeholder: '请输入阈值，最小值 0， 最大值 100',
      style: {
        width: '100%',
      },
    },
  },
  // {
  //   label: '',
  //   field: 'threshold',
  //   component: 'Input',
  //   required: true,
  //   colProps: {
  //     span: 4,
  //   },
  // },
  {
    label: '目标数量阈值',
    field: 'targetThreshold',
    component: 'InputNumber',
    required: true,
    componentProps: {
      min: 0,
      max: 20,
      placeholder: '请输入目标数量阈值，最小值 0， 最大值 20',
      style: {
        width: '100%',
      },
    },
  },
  // {
  //   label: '',
  //   field: 'targetThreshold',
  //   component: 'Input',
  //   required: true,
  //   colProps: {
  //     span: 4,
  //   },
  // },
  {
    label: '跟踪框交并比',
    field: 'iouThreshold',
    component: "InputNumber",
    componentProps: {
      min: 50,
      max: 100,
      placeholder: '跟踪事件结果时，前后相邻两个结果之间的重合度，最小值 50， 最大值 100',
      addonAfter: '%',
      style: {
        width: '100%',
      }
    },
  },
  {
    label: '时间持续间隔',
    field: 'timeSpace',
    component: "InputNumber",
    componentProps: {
      min: 1,
      addonAfter: '秒',
      placeholder: '连续N秒内检测到相同事件结果，首次产生告警',
      style: {
        width: '100%',
      }
    },
  },
  {
    label: '连续命中周期',
    field: 'hitCount',
    component: "InputNumber",
    componentProps: {
      min: 1,
      addonAfter: '秒',
      placeholder: '当跟踪结果切换后保持N秒，产生告警',
      style: {
        width: '100%',
      }
    },
  },
  {
    label: '误报过滤周期',
    field: 'sensitivity',
    component: "InputNumber",
    componentProps: {
      min: 1,
      addonAfter: '秒',
      placeholder: '有可能是误报事件结果，N秒后产生告警',
      style: {
        width: '100%',
      }
    },
  },
  {
    label: '类别',
    field: 'pid',
    component: 'Input',
    show: false,
    // componentProps: {
    //   dict: 'ky_category,name,id',
    //   pidField: 'pid',
    //   pidValue: '0',
    //   condition: '{"category": 0}',
    //   hasChildField: 'has_child',
    // },
  },
  // TODO 主键隐藏字段，目前写死为ID
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
];
