//列表数据

export const columns = [
  {
    title: '相机名称',
    align: 'center',
    dataIndex: 'cameraName',
    key: 'cameraName',
  },
  {
    title: '分组',
    align: 'center',
    dataIndex: 'locationDict',
    key: 'locationDict',
    ellipsis: true,
    // 为了展示拼接的所有父子级 修改返回的字段location改为拼接好的新字段locationDict
    // customRender: ({ record }) => {
    //   return record.location_dictText;
    // },
  },
  {
    title: '相机状态',
    align: 'center',
    dataIndex: 'cameraStatus',
    customRender: ({ record }) => {
      return record.cameraStatus_dictText;
    },
    key: 'cameraStatus',
    width: '12%',
    filters: [
      {
        text: '离线',
        value: ['2', '3', '4', '5', '6', '7'],
      },
      {
        text: '在线',
        value: '1',
      },
    ],
    customFilterDropdown: true,
    filterMutiple: true,
    onFilter: (value: string, record: any) => {
      if (Array.isArray(value)) {
        return value.includes(record.cameraStatus);
      }
      return record.cameraStatus === value;
    },
  },
  // {
  //   title: '工作类型',
  //   align: 'center',
  //   dataIndex: 'workType',
  //   key: 'workType',
  //   width: '12%',
  //   customRender: () => {
  //     return '检测';
  //   },
  // },
  // {
  //   title: '相机型号',
  //   align: 'center',
  //   dataIndex: 'cameraType',
  //   key: 'cameraType',
  //   width: '10%',
  // },
  // {
  //   title: '运行状态',
  //   align: 'center',
  //   dataIndex: 'cameraStatus',
  //   customRender: ({ text }) => {
  //     return render.renderSwitch(text, [
  //       { text: '是', value: 'Y' },
  //       { text: '否', value: 'N' },
  //     ]);
  //   },
  //   key: 'cameraStatus',
  //   width: '10%',
  // },
  // {
  //   title: '当前算法',
  //   align: 'center',
  //   dataIndex: 'cameraAlias',
  //   key: 'cameraAlias',
  //   width: '10%',
  // },
  {
    title: '已配置算法',
    align: 'center',
    dataIndex: 'alarmType',
    key: 'alarmType',

    ellipsis: true,
  },
  {
    title: '相机厂商',
    align: 'center',
    dataIndex: 'factory',
    key: 'factory',

    customRender: ({ record }) => {
      return record.factory_dictText;
    },
  },
  {
    title: '网络IP',
    align: 'center',
    dataIndex: 'ip',
    key: 'ip',
  },
  // {
  //   title: '场景别名',
  //   align: 'center',
  //   dataIndex: 'cameraAlias',
  //   key: 'cameraAlias',
  //   width: '12%',
  // },
  // {
  //   title: '相机归属',
  //   align: 'center',
  //   dataIndex: 'cameraOwnership',
  //   key: 'cameraOwnership',
  //   width: '12%',
  // },
  // {
  //   title: '网络端口',
  //   align: 'center',
  //   dataIndex: 'port',
  //   key: 'port',
  //   width: '12%',
  // },
  // {
  //   title: '用户名',
  //   align: 'center',
  //   dataIndex: 'username',
  //   key: 'username',
  //   width: '12%',
  // },
  // {
  //   title: '密码',
  //   align: 'center',
  //   dataIndex: 'password',
  //   key: 'password',
  //   width: '12%',
  // },
  // {
  //   title: '码流类型',
  //   align: 'center',
  //   dataIndex: 'streamType_dictText',
  //   key: 'streamType_dictText',
  //   width: '12%',
  // },
  // {
  //   title: '码流格式',
  //   align: 'center',
  //   dataIndex: 'streamFormat_dictText',
  //   key: 'streamFormat_dictText',
  //   width: '12%',
  // },
  // {
  //   title: '流地址',
  //   align: 'center',
  //   dataIndex: 'streamAddress',
  //   key: 'streamAddress',
  //   width: '12%',
  // },
  // {
  //   title: '预览图',
  //   align: 'center',
  //   dataIndex: 'pic',
  //   customRender: render.renderImage,
  //   key: 'pic',
  //   width: '12%',
  // },
  {
    title: '操作',
    dataIndex: 'operation',
    key: 'operation',
    align: 'center',
    width: '30%',
    fixed: 'right',
  },
];

export interface tableItem {
  cameraName: string;
  location: string;
  cameraStatus: string;
  workType: string;
  alarmType: string;
  factory: string;
  ip: string;
}

export interface cameraData {
  cameraName: string;
  location: string;
  cameraOwnership: string;
  username: string;
  password: string;
  streamAddress: string;
  cameraType: string;
  streamType: string;
  streamFormat: string;
  ip: string;
  port: string;
  workType: string;
  factory: string;
  channel: string;
  isConfig: string;
  cameraType_dictText: string;
}

//license
export interface license {
  canUseNum: number;
  usedNum: number;
  allNum: number;
}
