<template>
  <BasicModal @register="registerModal"
              @ok="handleOk">
    <BasicForm @register="registerForm"/>
  </BasicModal>
</template>

<script setup lang="ts">
import {BasicModal, useModalInner} from "/@/components/Modal";
import {BasicForm, FormSchema, useForm} from "/@/components/Form"
import {
  getAlgorithmList,
  getClassNameList,
  registerAlgorithm,
  updateMethodTable
} from "/@/views/adapter/adapter.api";

const emit = defineEmits(['refreshTable'])

let params = null
const [registerModal, {setModalProps, closeModal}] = useModalInner(async (data: any) => {
  setModalProps({
    loading: true,
    loadingTip: '正在初始化选择框数据...',
  });
  const classNameList = await getClassNameList({})
  if (classNameList && classNameList.length > 0) {
    classNameList.map((item: any) => {
      item.label = item.className
      item.value = item.id
    })
  }
  await updateSchema([{
      field: 'classId',
      componentProps: {
        options: classNameList,
      }
    }])
  setModalProps({
    loading: false,
  });
  params = data
  const {isUpdate} = data
  setModalProps({
    confirmLoading: false,
    width: '500px',
    title: isUpdate ? '编辑算法' : '新增算法'
  });
  if (isUpdate) {
    await setFieldsValue(data.model);
  } else {
    await resetFields();
  }
})

const schemas: FormSchema[] = [{
  field: 'classId',
  label: '选择类名',
  component: 'Select',
  rules: [{
    required: true,
    type: 'string',
    trigger: 'blur'
  }],
}, {
  field: 'kyCategoryId',
  label: '选择算法',
  component: 'ApiSelect',
  componentProps: {
    api: getAlgorithmList,
    // resultField: 'result',
    labelField: 'name',
    valueField: 'id'
  },
  rules: [{
    required: true,
    type: 'string',
    trigger: 'blur'
  }],
}, {
  field: 'status',
  label: '状态',
  component: 'RadioGroup',
  componentProps: {
    options: [{
      label: '启用',
      value: 1
    }, {
      label: '禁用',
      value: 0
    }]
  }
}, {
  field: 'remark',
  label: '备注',
  component: 'InputTextArea',
}]

const [registerForm, {setFieldsValue, validate, resetFields, updateSchema}] = useForm({
  labelWidth: 150,
  schemas,
  showActionButtonGroup: false,
})

const handleOk = async () => {
  const values = await validate();
  if (!values) return;
  setModalProps({confirmLoading: true, loadingTip: '正在提交中...'});
  const {isUpdate} = params;
  try {
    if (isUpdate) {
      // 修改
      values.id = params.model.id;
      await updateMethodTable(values);
      emit('refreshTable')
      closeModal();
    } else {
      // 新增
      await registerAlgorithm(values);
      emit('refreshTable')
      closeModal();
    }
  }finally {
    setModalProps({confirmLoading: false});
  }
}
</script>

<style scoped lang="less">

</style>
