import {BasicColumn, FormSchema} from '/@/components/Table';

//列表数据

export const logColumns: BasicColumn[] = [
  {
    title: '升级时间', //下发时间作为升级时间
    align: 'center',
    sorter: true,
    dataIndex: 'downTime',
  },
  {
    title: '升级状态',
    align: 'center',
    dataIndex: 'resultStatus',
    slots: {customRender: 'resultStatus'},
  },
  {
    title: '操作人',
    align: 'center',
    dataIndex: 'upPeople',
  },
  // {
  //   title: '升级结果时间',
  //   align:"center",
  //   dataIndex: 'resultTime'
  // },
  // {
  //   title: '升级结果描述',
  //   align:"center",
  //   dataIndex: 'resultDes'
  // },
  // {
  //   title: '下发状态',
  //   align:"center",
  //   dataIndex: 'downStatus'
  // },
  // {
  //   title: '下发描述',
  //   align:"center",
  //   dataIndex: 'downDes'
  // },
  // {
  //   title: '备份时间',
  //   align:"center",
  //   dataIndex: 'backupTime'
  // },
  // {
  //   title: '备份状态',
  //   align:"center",
  //   dataIndex: 'backupStatus'
  // },
  // {
  //   title: '备份描述',
  //   align:"center",
  //   dataIndex: 'backupDes'
  // },
  // {
  //   title: '升级时间',
  //   align:"center",
  //   dataIndex: 'upTime'
  // },
  // {
  //   title: '升级状态',
  //   align:"center",
  //   dataIndex: 'upStatus'
  // },
  // {
  //   title: '升级描述',
  //   align:"center",
  //   dataIndex: 'upDes'
  // },
];

export const columns: BasicColumn[] = [
  {
    title: '设备名称',
    align: 'center',
    dataIndex: 'deviceName',
  },
  {
    title: '设备ip',
    align: 'center',
    dataIndex: 'deviceIp',
  },
  {
    title: '类别',
    align: 'center',
    dataIndex: 'category_dictText',
  },
  {
    title: '状态',
    align: 'center',
    dataIndex: 'status_dictText',
  },
  {
    title: '厂家名称',
    align: 'center',
    dataIndex: 'factoryName_dictText',
  },
  {
    title: '摄像头数量',
    align: 'center',
    dataIndex: 'cameraNum',
  },
  {
    title: '算法数量',
    align: 'center',
    dataIndex: 'algorithmNum',
  },
  {
    title: 'license状态',
    align: 'center',
    dataIndex: 'licenseStatus',
  },
  {
    title: '用户名',
    align: 'center',
    dataIndex: 'user',
  },
  {
    title: '密码',
    align: 'center',
    dataIndex: 'pwd',
  },
  {
    title: '数据存储时长',
    align: 'center',
    dataIndex: 'dataSave',
  },
];
//查询数据
export const searchFormSchema: FormSchema[] = [];
//表单数据
export const formSchema: FormSchema[] = [
  {
    label: '设备名称',
    field: 'deviceName',
    component: 'Input',
    rules: [
      {
        required: true,
        trigger: 'blur',
      },
    ],
  },
  {
    label: '设备ip',
    field: 'deviceIp',
    component: 'Input',
    componentProps: {
      placeholder: '请输入设备ip地址',
      //api: () => list(),
      //labelField: 'ipAddress',
      //valueField: 'ipAddress',

    },
    rules: [
      {
        required: true,
        pattern: /^(?:(?:1[0-9][0-9]\.)|(?:2[0-4][0-9]\.)|(?:25[0-5]\.)|(?:[1-9][0-9]\.)|(?:[0-9]\.)){3}(?:(?:1[0-9][0-9])|(?:2[0-4][0-9])|(?:25[0-5])|(?:[1-9][0-9])|(?:[0-9]))$/,
        message: "请输入正确的IP地址",
        trigger: 'blur',
      }
    ],
    defaultValue: '127.0.0.1',
    show: false
  },
  {
    label: '网卡ip',
    field: 'networkIp',
    component: 'ApiSelect',
    componentProps: {
      placeholder: '请选择网卡ip',
      // api: () => list(),
      // labelField: 'ipAddress',
      // valueField: 'ipAddress',

    },
    rules: [
      {
        required: true,
        pattern: /^(?:(?:1[0-9][0-9]\.)|(?:2[0-4][0-9]\.)|(?:25[0-5]\.)|(?:[1-9][0-9]\.)|(?:[0-9]\.)){3}(?:(?:1[0-9][0-9])|(?:2[0-4][0-9])|(?:25[0-5])|(?:[1-9][0-9])|(?:[0-9]))$/,
        message: "请输入正确的网卡ip",
        trigger: 'blur',
      }
    ],
    defaultValue: undefined,
    show: false
  },
  // {
  //   label: '类别',
  //   field: 'category',
  //   component: 'JDictSelectTag',
  //   rules: [
  //     {
  //       required: true,
  //       trigger: 'blur',
  //     },
  //   ],
  //   componentProps: {
  //     dictCode: 'device_category',
  //     showChooseOption: false,
  //   },
  // },
  // {
  //   label: '状态',
  //   field: 'status',
  //   component: 'JDictSelectTag',
  //   componentProps:{
  //       dictCode:"device_status"
  //    },
  // },
  // {
  //   label: '厂家名称',
  //   field: 'factoryName',
  //   component: 'JDictSelectTag',
  //   componentProps:{
  //       dictCode:"factory_name"
  //    },
  // },
  // {
  //   label: '摄像头数量',
  //   field: 'cameraNum',
  //   component: 'InputNumber',
  // },
  // {
  //   label: '算法数量',
  //   field: 'algorithmNum',
  //   component: 'InputNumber',
  // },
  // {
  //   label: 'license状态',
  //   field: 'licenseStatus',
  //   component: 'Input',
  // },
  // {
  //   label: '用户名',
  //   field: 'user',
  //   component: 'Input',
  //   rules: [
  //     {
  //       required: true,
  //       pattern: /^[a-zA-Z0-9_.~!@#$^&*()=+%\-]*$/,
  //       message: '包含不支持的字符',
  //       trigger: 'blur',
  //     },
  //   ],
  // },
  // {
  //   label: '密码',
  //   field: 'pwd',
  //   component: 'InputPassword',
  //   componentProps: {
  //     autocomplete: "off"
  //   },
  //   rules: [
  //     {
  //       required: true,
  //       pattern: /^[a-zA-Z0-9_.~!@#$^&*()=+%\-]*$/,
  //       message: '包含不支持的字符',
  //       trigger: 'blur',
  //     },
  //   ],
  // },
  {
    label: '',
    field: 'deviceCode',
    component: 'Input',
    helpMessage: '公司国际编码+行业编码，共13位.默认13个0.',
    defaultValue: '0000000000000',
    rules: [
      {
        pattern: /^\d{13}$/,
        message: '资源编码为公司国际编码+行业编码，共13位数字',
        trigger: 'blur',
      },
    ],
    show: false
  },
  {
    label: '设备编码',
    field: 'deviceNumber',
    component: 'Input',
    defaultValue: '',
    rules: [
      {
        pattern: /^[a-zA-Z0-9]*$/,
        message: '设备编码只含有字母、数字',
        trigger: 'blur',
      },
    ]
  },
  // {
  //   label: '数据存储时长',
  //   field: 'dataSave',
  //   component: 'Input',
  // },
  // TODO 主键隐藏字段，目前写死为ID
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
];

export interface resMessage {
  data: {
    code: number;
    msg: string;
  };
}
