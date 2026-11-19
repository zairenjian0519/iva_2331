<template>
  <BasicModal
    v-bind="$attrs"
    destroyOnClose
    @register="registerInnerModal"
    @ok="handleSubmit">

    <BasicForm @register="registerForm">

    </BasicForm>

  </BasicModal>
</template>

<script setup lang="ts">
import {BasicModal, useModalInner} from '/@/components/Modal';
import {BasicForm, useForm} from "/@/components/Form";
import {schemas} from './lock.data';
import {addDevice, updateDevice} from "/@/views/iot/electricLock/lock.api";
import {useMessage} from "/@/hooks/web/useMessage";
import {ref} from "vue";
import {getKyCategoryTreeData} from "/@/views/base/kycategory/KyCategory.api";

const {createMessage} = useMessage();

const emit = defineEmits(['register', 'success']);
const updateFlag = ref(false);

const [registerInnerModal, {setModalProps, closeModal}] = useModalInner((data) => {
  setModalProps({
    width: 600,
    title: data.title,
  });
  resetFields();
  const {isUpdate} = data
  if (isUpdate) {
    updateFlag.value = true;
    setFieldsValue(data.record);
  }
  getKyCategoryTreeData({category: 1}).then((response:any)=>{
    if (!response) return
    updateSchema({
      field: 'location',
      componentProps: {
        treeData: response,
        multiple: false,
        treeCheckable: false,
        treeDefaultExpandAll: true,
        showCheckedStrategy: 'SHOW_PARENT',
      }
    })
  })
});

const [registerForm, {resetFields, setFieldsValue, validate, updateSchema}] = useForm({
  labelWidth: 120,
  schemas: schemas,
  showActionButtonGroup: false,
})

const handleSubmit = async () => {
  const values = await validate();
  console.log('values', values);
  setModalProps({confirmLoading: true});
  try {
    if (updateFlag.value) {
      await updateDevice(values)
    } else {
      await addDevice(values)
    }
    createMessage.success('保存成功')
    emit('success')
  } finally {
    setModalProps({confirmLoading: false});
    closeModal();
  }
};

</script>

<style scoped lang="less">

</style>
