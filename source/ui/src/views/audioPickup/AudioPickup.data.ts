import {FormSchema} from "/@/components/Form";

export const columns = [
  {
    title: '音频采集点名称',
    dataIndex: 'deviceName',
    width: 150,
    align: 'center',
  },
  {
    title: '分片时长（秒）',
    dataIndex: 'recordSegmentSeconds',
    width: 150,
    align: 'center',
  },
  {
    title: 'rtsp地址',
    dataIndex: 'rtspUrl',
    align: 'center',
  },
  {
    title: '采集状态',
    dataIndex: 'collectStatus',
    width: 100,
    align: 'center',
    slots: { customRender: 'collectStatus' },
  },
  {
    title: '设备状态',
    dataIndex: 'status',
    width: 100,
    align: 'center',
    slots: { customRender: 'status' },
  },
  {
    title: '备注',
    dataIndex: 'remark',
    width: 250,
    align: 'center',
  }
]

export const schemas: FormSchema[] = [
  {
   field: 'id',
   label: 'id',
   component: 'Input',
   show: false,
  },{
    field: 'location',
    label: '位置',
    component: 'TreeSelect',
    componentProps: {
      treeData: [],
    },
    required: true,
  },{
    field: 'deviceName',
    label: '采集点名称',
    component: 'Input',
    required: true,
  }, {
    field: 'deviceCode',
    label: '编码',
    component: 'Input',
    required: true,
  }, {
    field: 'rtspUrl',
    label: 'rtsp地址',
    component: 'Input',
    required: true,
  },
  {
    field: 'recordSegmentSeconds',
    label: '分片时长（秒）',
    componentProps: {
      style: {
        width: '100%',
      },
    },
    component: 'InputNumber',
    required: true,
  },
  {
    field: 'remark',
    label: '备注',
    component: 'Input',
  }
]
