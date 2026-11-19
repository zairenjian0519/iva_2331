import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: '参数名称',
    dataIndex: 'paramName',
  },
  {
    title: '参数编码',
    dataIndex: 'paramCode',
  },
  {
    title: '参数值',
    dataIndex: 'paramVal',
  },
  {
    title: '描述',
    dataIndex: 'description',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    label: '参数名称',
    field: 'paramName',
    component: 'Input',
  },
  {
    label: '参数编码',
    field: 'paramCode',
    component: 'Input',
  },
];

export const formSchema: FormSchema[] = [
  // TODO 主键隐藏字段，目前写死为ID
  { label: '', field: 'id', component: 'Input', show: false },
  {
    label: '参数名称',
    field: 'paramName',
    component: 'Input',
    required: true,
  },
  {
    label: '参数编码',
    field: 'paramCode',
    component: 'Input',
    required: true,
  },
  {
    label: '参数值',
    field: 'paramVal',
    component: 'Input',
    required: true,
  },
  {
    label: '描述',
    field: 'description',
    component: 'Input',
    required: true,
  },
];
