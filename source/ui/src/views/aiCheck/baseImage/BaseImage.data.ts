import {BasicColumn} from "/@/components/Table";

export const columns: BasicColumn[] = [{
  title: '图片名称',
  dataIndex: 'fileName',
}, {
  title: '缩略图',
  dataIndex: '',
  slots: { customRender: 'img' },
}, {
  title: '图片大小',
  dataIndex: 'fileSize',
}, {
  title: '上传时间',
  dataIndex: 'createTime',
}]
