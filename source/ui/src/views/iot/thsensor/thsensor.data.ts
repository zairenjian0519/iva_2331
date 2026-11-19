import {FormSchema} from "/@/components/Form";
import {BasicColumn} from "/@/components/Table";

export const columns: BasicColumn[] = [
  {
    title: '设备名称',
    dataIndex: 'deviceName',
    key: 'deviceName',
    align: 'center',
  }, {
    title: '设备编码',
    dataIndex: 'deviceCode',
    key: 'deviceCode',
    align: 'center',
  }, {
    title: '轮询指令',
    dataIndex: 'pollingCommand',
    key: 'pollingCommand',
    align: 'center',
  }, {
    title: '从机地址',
    dataIndex: 'slaveAddress',
    key: 'slaveAddress',
    align: 'center',
  }
]

export const formSchemas: FormSchema[] = [
  {
    field: 'commPortName',
    label: '选择串口',
    component: 'Select',
    required: true,
    componentProps: {
      options: [],
    },
    colProps: {
      span: 8,
    },
  },
  {
    field: 'baudRate',
    label: '波特率',
    component: 'InputNumber',
    componentProps: {
      min: 0,
      max: 1000000,
      step: 1,
      style: {
        width: '100%',
      },
    },
    required: true,
    defaultValue: 9600,
    helpMessage: '波特率范围0-1000000，默认9600',
    colProps: {
      span: 8,
    },
  }, {
    field: 'dataBits',
    label: '数据位',
    component: 'InputNumber',
    componentProps: {
      min: 0,
      max: 1000000,
      step: 1,
      style: {
        width: '100%',
      },
    },
    required: true,
    defaultValue: 8,
    helpMessage: '数据位范围0-1000000，默认8',
    colProps: {
      span: 8,
    },
  }, {
    field: 'stopBits',
    label: '停止位',
    component: 'InputNumber',
    componentProps: {
      min: 0,
      max: 1000000,
      step: 1,
      style: {
        width: '100%',
      },
    },
    required: true,
    defaultValue: 1,
    helpMessage: '停止位范围0-1000000，默认1',
    colProps: {
      span: 8,
    },
  }, {
    field: 'parity',
    label: '校验位',
    component: 'Select',
    componentProps: {
      options: [
        {
          label: '无校验',
          value: 0,
        },
        {
          label: '奇校验',
          value: 1,
        },
        {
          label: '偶校验',
          value: 2,
        },
        {
          label: 'Mark校验',
          value: 3,
        },
        {
          label: 'Space校验',
          value: 4,
        },
      ]
    },
    required: true,
    defaultValue: 0,
    colProps: {
      span: 8,
    },
    helpMessage: '默认无校验',
  },{
    field: 'status',
    label: '状态',
    component: 'Input',
    slot: 'status',
    colProps: {
      span: 8
    }
  }
]
