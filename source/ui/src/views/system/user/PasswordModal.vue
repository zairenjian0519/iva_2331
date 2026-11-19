<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="修改密码" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts" name="PassWordModal" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formPasswordSchema } from './user.data';
  import { changePassword } from './user.api';
  import { string } from 'vue-types';
  import { createNonceStr } from '/@/utils/common/compUtils';
  import { AesEncryption } from '/@/utils/cipher';

  const key: string = createNonceStr();
  const iv: string = createNonceStr();
  const encryption = new AesEncryption({ key: key, iv: iv });
  // 声明Emits
  const emit = defineEmits(['success', 'register']);
  //表单配置
  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: formPasswordSchema,
    showActionButtonGroup: false,
  });
  //表单赋值
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    //重置表单
    await resetFields();
    setModalProps({ confirmLoading: false });
    //表单赋值
    await setFieldsValue({ ...data });
  });
  //表单提交事件
  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      const { password } = values;
      values.password = encryption.encryptByAES(password);
      values.randomKey = key;
      values.randomCode = iv;
      //提交表单
      await changePassword(values);
      //关闭弹窗
      closeModal();
      //刷新列表
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
