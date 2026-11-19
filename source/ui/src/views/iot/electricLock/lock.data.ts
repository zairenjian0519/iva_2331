import {BasicColumn, FormSchema} from "/@/components/Table";

export const columns: BasicColumn[] = [
  {
    title: '锁名称',
    dataIndex: 'deviceName',
    key: 'deviceName',
    align: 'center',
  }, {
    title: '锁编号',
    dataIndex: 'deviceCode',
    key: 'deviceCode',
    align: 'center',
  }, {
    title: '备注',
    dataIndex: 'remark',
    key: 'remark',
    align: 'center',
  }
]

export const schemas: FormSchema[] = [
  {
    field: 'id',
    label: 'id',
    component: 'Input',
    show: false,
  }, {
    field: 'location',
    label: '位置',
    component: 'TreeSelect',
    componentProps: {
      treeData: [],
    },
    required: true,
  }, {
    field: 'pinNumber',
    label: 'pin编号',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'gpio_number',
    },
    required: true,
  },
  {
    field: 'deviceName',
    label: '锁名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'deviceCode',
    label: '锁编号',
    component: 'Input',
    required: true,
  },
  {
    field: 'remark',
    label: '备注',
    component: 'Input',

  }
]
