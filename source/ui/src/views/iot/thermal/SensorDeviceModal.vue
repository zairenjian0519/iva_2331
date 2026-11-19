<template>
  <BasicModal @register="registerModal">
    <BasicForm @register="registerForm" @submit="handleSubmit" @reset="handleReset">
    </BasicForm>
  </BasicModal>
</template>

<script setup lang="ts">
import {BasicModal, useModalInner} from "/@/components/Modal"
import {BasicForm, FormSchema, useForm} from "/@/components/Form";
import {addDevice, updateDevice} from "./thsensor.api";
import {useMessage} from "/@/hooks/web/useMessage";

const {createMessage} =useMessage();

const emit = defineEmits(['register', 'success']);

// 校验ip地址
function isValidIPv4(ip) {
  if (!ip) return false;

  const octet = '(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])';
  const regex = new RegExp(`^${octet}\\.${octet}\\.${octet}\\.${octet}$`);

  return regex.test(ip);
}

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
                if (/^[a-zA-Z][a-zA-Z0-9_-]*$/.test(value) && value.length <= 50) {
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
  }, {
    field: 'ipAddress',
    component: 'Input',
    label: 'IP地址',
    dynamicRules: () => {
      return [
        {
          required: true,
          validator: (_, value) => {
            return new Promise((resolve, reject) => {
              if (value) {
                if (isValidIPv4(value)) {
                  resolve();
                } else {
                  reject("请填写正确的 IP 地址");
                }
              } else {
                reject("请填写设备 IP 地址");
              }
            });
          },
        },
      ];
    },
    helpMessage: '热成像相机中配置的 IP 地址',
  }, {
    field: 'port',
    component: 'InputNumber',
    label: '端口号',
    componentProps: {
      min: 1,
      max: 65536,
      step: 1,
      style: {
        width: '100%',
      },
    },
    required: true,
    defaultValue: 37777,
    helpMessage: '热成像相机中配置的端口号',
  }, {
    field: 'username',
    component: 'Input',
    label: '用户名',
    required: true,
    helpMessage: '热成像相机登录的用户名',
  }, {
    field: 'password',
    component: 'Input',
    label: '密码',
    required: true,
    componentProps: {
      type: 'password',
    },
    helpMessage: '热成像相机登录的密码',
  }, {
    field: 'thermalChannel',
    component: 'InputNumber',
    label: '热成像通道',
    componentProps: {
      min: 1,
      max: 999,
      step: 1,
      style: {
        width: '100%',
      },
    },
    required: true,
    defaultValue: 2,
    helpMessage: '热成像相机热力图通道',
  }, {
    field: 'meterType',
    component: 'Select',
    label: '测点类型',
    required: true,
    defaultValue: 1,
    helpMessage: '大华热成像相机中配置的测点类型',
  }, {
    field: 'presetId',
    component: 'InputNumber',
    label: '预置点编号',
    componentProps: {
      min: 1,
      max: 999,
      step: 1,
      style: {
        width: '100%',
      },
    },
    required: true,
    defaultValue: 1,
    helpMessage: '此编号为大华热成像相机中配置的测温点编号',
  }, {
    field: 'ruleId',
    component: 'InputNumber',
    label: '规则编号',
    componentProps: {
      min: 1,
      max: 999,
      step: 1,
      style: {
        width: '100%',
      },
    },
    required: true,
    defaultValue: 1,
    helpMessage: '此编号为大华热成像相机中配置的规则编号',
  },
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
    location: '',
    meterType: '',
    presetId: 1,
    ruleId: 1,
    ipAddress: '',
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
  if (data.kyCategoryTreeData) {
    updateSchema({
      field: 'location',
      componentProps: {
        treeData: data.kyCategoryTreeData,
        multiple: false,
        treeCheckable: false,
        treeDefaultExpandAll: true,
        showCheckedStrategy: 'SHOW_PARENT',
      }
    })
  }

  if (data.meterTypeList) {
    updateSchema({
      field: 'meterType',
      componentProps: {
        options: data.meterTypeList,
      }
    })
  }

  if (data.isUpdate) {
    setFieldsValue(data.record)
  }
})
</script>

<style scoped lang="less">

</style>
