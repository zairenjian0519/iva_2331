<template>
  <BasicModal @register="registerModal">
    <BasicForm @register="registerForm" @submit="handleSubmit" @reset="handleReset">
    </BasicForm>
  </BasicModal>
</template>

<script setup lang="ts">
import {BasicModal, useModalInner} from "/@/components/Modal"
import {BasicForm, FormSchema, useForm} from "/@/components/Form";
import {addDevice, updateDevice} from "/@/views/iot/thsensor/thsensor.api";
import {useMessage} from "/@/hooks/web/useMessage";
import {getKyCategoryTreeData} from "/@/views/base/kycategory/KyCategory.api";

const {createMessage} =useMessage();

const emit = defineEmits(['register', 'success']);

const schemas: FormSchema[] = [
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
    field: 'deviceName',
    component: 'Input',
    label: '设备名称',
    required: true,
  },
  {
    field: 'deviceCode',
    component: 'Input',
    label: '设备编号',
    dynamicRules: () => {
      return [
        {
          required: true,
          validator: (_, value) => {
            return new Promise((resolve, reject) => {
              if (value) {
                // 设备编号只能以字母开头，只能包括字母、数字、下划线，长度不超过50个字符
                if (/^[a-zA-Z][a-zA-Z0-9_]*$/.test(value) && value.length <= 50) {
                  resolve();
                } else {
                  reject("设备编号只能以字母开头，只能包括字母、数字、下划线，长度不超过50个字符");
                }
              } else {
                reject("请输入设备编号");
              }
            });
          },
        },
      ];
    },
  },{
    field: 'pollingCommand',
    component: 'Input',
    label: '轮询指令',
    required: true,
    helpMessage: '请输入16进制指令，如：0x01',
  }, {
    field: 'slaveAddress',
    component: 'InputNumber',
    label: '从机地址',
    dynamicDisabled: true,
    componentProps: ({  formModel })=> {
      const hexStr = formModel.pollingCommand;
      if (hexStr) {
        const bytes = hexStr.split(' ').map(byte => parseInt(byte, 16));
        formModel.slaveAddress = bytes[0];
      }
      return {
        style: {
          width: '100%',
        },
      };
    }
  }
]
const [registerForm, {setFieldsValue, validate, clearValidate, setProps, updateSchema}] = useForm({
  schemas: schemas,
  labelCol: {
    span: 6,
  },
  wrapperCol: {
    span: 17,
  },
  actionColOptions: {
    span: 24,
  },
  submitButtonOptions: {
    text: '保存',
    preIcon: 'ant-design:save-outlined',
  },
})

const handleSubmit = async () => {
  const values = await validate()
  // console.log(values)
  await setProps({
    submitButtonOptions: {
      loading: true,
      preIcon: '',
    }
  })
  try {
    if (values.id) {
      await updateDevice(values)
    }else {
      await addDevice(values)
    }
    emit('success')
    createMessage.success(values.id? '修改成功' : '保存成功')
    closeModal()
  } finally {
    await setProps({
      submitButtonOptions: {
        loading: false,
        preIcon: 'ant-design:save-outlined',
      }
    })
  }
}

const handleReset =  () => {
  setFieldsValue({
   deviceName: '',
   deviceCode: '',
   pollingCommand: ''
 })
  clearValidate()
}

const [registerModal, {setModalProps, closeModal}] = useModalInner(data => {
  setModalProps({
    title: data.title,
    confirmLoading: false,
    width: 500,
    destroyOnClose: true,
    showOkBtn: false,
    showCancelBtn: false,
    footer: null,
  });
  // resetFields()
  if (data.isUpdate) {
    setFieldsValue(data.record)
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
})
</script>

<style scoped lang="less">

</style>
