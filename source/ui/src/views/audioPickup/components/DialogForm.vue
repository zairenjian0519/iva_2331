<template>
  <BasicModal v-bind="$attrs"
              destroyOnClose
              @register="register"
              width="800px"
              @ok="handleOk">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script setup lang="ts">
import BasicModal from "/@/components/Modal/src/BasicModal.vue";
import {useModalInner} from "/@/components/Modal";
import {BasicForm, useForm} from "/@/components/Form";
import {schemas} from "../AudioPickup.data"
import {save, update} from "/@/views/audioPickup/AudioPickup.api";
import {getKyCategoryTreeData} from "/@/views/base/kycategory/KyCategory.api";

let isUpdate = false
let reload = null
let record = {}

const [register, { closeModal, setModalProps }] = useModalInner((data)=> {
  console.log('Data Received', data);
  setModalProps({
    title: data.title,
  });
  isUpdate = !!data.isUpdate;
  if (isUpdate){
    record = data.record;
    setFieldsValue(record);
  }
  reload = data.reload;

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

const [registerForm, { validateFields, resetFields, setFieldsValue, updateSchema }] = useForm({
  labelWidth: 120,
  schemas,
  showActionButtonGroup: false,
});

const handleOk = async () => {
  try {
    const values = await validateFields();
    console.log('values', values);
    console.log('isUpdate', isUpdate)
    isUpdate? await update({id: record.id, ...values}): await save(values)
    reload()
    await resetFields();
    closeModal();
  } catch (error) {
    console.log('error', error);
  }
};

</script>

<style scoped lang="less">

</style>
