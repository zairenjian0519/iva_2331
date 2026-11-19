<template>
  <BasicModal v-bind="$attrs"
              destroyOnClose
              @ok="handleOk"
              @register="register">
    <BasicForm @register="registerForm" @submit="handleSubmit"/>
  </BasicModal>
</template>

<script setup lang="ts">
import {BasicModal, useModalInner} from "/@/components/Modal";
import {BasicForm, FormSchema, useForm} from "/@/components/Form";
import {updateClass, addClass} from "/@/views/adapter/adapter.api";

const emit = defineEmits(['refreshTable']);

let params = null
const [register, {setModalProps, closeModal}] = useModalInner((data: any) => {
   params = data
  const isUpdate = data.isUpdate
  setModalProps({
    width: '30%',
    confirmLoading: false,
    closable: true,
    maskClosable: false,
    title: isUpdate ? '修改信息' : '新增信息',
  });
  if (isUpdate) {
    setFieldsValue(data.model)
  }
})

const handleOk = async () => {
  await handleSubmit()
}

const schemas: FormSchema[] = [
  {
    field: 'className',
    label: '类名',
    component: 'Input',
    rules: [{
      required: true,
      type: 'string',
    }, {
      max: 50,
      message: '最大长度为50',
    }],
  }, {
    field: 'status',
    label: '状态',
    component: 'RadioGroup',
    componentProps: {
      options: [
        {
          label: '启用',
          value: 1
        }, {
          label: '禁用',
          value: 0
        }
      ]
    },
    required: true,

  }, {
    field: 'remark',
    label: 'class描述',
    component: 'InputTextArea',
    rules: [{
      required: false,
      type: 'string',
    }, {
      max: 100,
      message: '最大长度为100',
    }],

  }]

const [registerForm, {setFieldsValue, validate}] = useForm({
  schemas,
  labelWidth: 120,
  showActionButtonGroup: false,
});

const handleSubmit = async () => {
  const values = await validate();
  if (!values) return;
  console.log(values);
  setModalProps({confirmLoading: true, loadingTip: '正在提交中...'});
  const {isUpdate} = params;
  try {
    if (isUpdate) {
      // 修改
      values.id = params.model.id;
      await updateClass(values);
      closeModal();
      emit('refreshTable');
    } else {
      // 新增
      await addClass(values);
      closeModal();
      emit('refreshTable');
    }
  } finally {
    setModalProps({confirmLoading: false});
  }
}
</script>

<style scoped lang="less">

</style>
