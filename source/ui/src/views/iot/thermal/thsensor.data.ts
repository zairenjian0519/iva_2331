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
    title: '位置',
    dataIndex: 'location',
    key: 'location',
    align: 'center',
    slots: {customRender: 'location'},
  }, {
    title: '测点类型',
    dataIndex: 'meterType',
    key: 'meterType',
    align: 'center',
    slots: {customRender: 'meterType'},
  }, {
    title: '预置点编号',
    dataIndex: 'presetId',
    key: 'presetId',
    align: 'center',
  }, {
    title: '规则编号',
    dataIndex: 'ruleId',
    key: 'ruleId',
    align: 'center',
  }, {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    align: 'center',
  }
]

