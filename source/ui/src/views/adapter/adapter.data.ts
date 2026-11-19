import {BasicColumn} from "/@/components/Table";

export const columnsFile: BasicColumn[] = [
  {
    title: '文件名',
    dataIndex: 'fileOriginalName',
    key: 'fileOriginalName',
    align: 'center',
  }, {
    title: '类型',
    dataIndex: 'fileType',
    key: 'fileType',
    align: 'center',
    slots: {customRender: 'fileType'},
  }, {
    title: '保存位置',
    dataIndex: 'filePath',
    key: 'filePath',
    align: 'center',
    slots: {customRender: 'filePath'},
  }, {
    title: '工作路径',
    dataIndex: 'workPath',
    key: 'workPath',
    align: 'center',
    slots: {customRender: 'workPath'},
  }, {
    title: '日期',
    dataIndex: 'createTime',
    key: 'createTime',
    align: 'center',
  }, {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    align: 'center',
    slots: {customRender: 'status'},
  }
]

export const columnsClass: BasicColumn[] = [
  {
    title: 'class类名',
    dataIndex: 'className',
    key: 'className',
    align: 'center',
  }, {
    title: 'class描述',
    dataIndex: 'remark',
    key: 'remark',
    align: 'center',
  }, {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    align: 'center',
    slots: {customRender: 'status'},
  }, {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    align: 'center',
  }
]

export const columnsMethod: BasicColumn[] = [
  {
    title: '方法名',
    dataIndex: 'className',
    key: 'className',
    align: 'center',
  },{
    title: '算法名称',
    dataIndex: 'algorithmName',
    key: 'algorithmName',
    align: 'center',
  }, {
    title: '算法Id',
    dataIndex: 'algorithmId',
    key: 'algorithmId',
    align: 'center',
  }, {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    align: 'center',
    slots: {customRender: 'status'},
  }]
