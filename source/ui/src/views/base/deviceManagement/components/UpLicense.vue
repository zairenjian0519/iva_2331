<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="License授权" :width="600" ok-text="授权" @ok="handleSubmit">
    <a-row style="margin-top: 20px">
      <a-col :offset="1" :span="23">
        <label>选择license文件</label>
      </a-col>
    </a-row>
    <a-row style="margin-top: 6px">
      <a-col :offset="1" :span="23">
        <a-upload :action="uploadUrl" :headers="getheader()" :max-count="1" @change="uploadChange" :show-upload-list="false">
          <a-input style="width: 500px" v-model:value="selectFileName">
            <template #suffix>
              <img :src="infoIconWarning" alt="" />
            </template>
          </a-input>
        </a-upload>
      </a-col>
    </a-row>
  </BasicModal>
</template>

<script lang="ts" setup>
  import BasicModal from '/@/components/Modal/src/BasicModal.vue';
  import { useModalInner } from '/@/components/Modal';
  import { Col, Row } from 'ant-design-vue';
  import { ref } from 'vue';
  import { uploadLicense, uploadUrl } from '../DeviceManagement.api';
  import infoIconWarning from '/@/assets/svg/icon/info_icon_warning.svg';
  import { getToken } from '/@/utils/auth';
  import { useMessage } from '/@/hooks/web/useMessage';
  const { createMessage, createConfirm } = useMessage();

  const ACol = Col;
  const ARow = Row;
  const selectFileName = ref<string>('');
  const devItem = ref();

  const emits = defineEmits(['handleLicenseOk']);
  function getheader() {
    return { 'X-Access-Token': getToken() };
  }

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    selectFileName.value = '';
    devItem.value = data.item;
    setModalProps({ confirmLoading: false, showCancelBtn: true, showOkBtn: true });
  });

  function handleSubmit() {
    if (selectFileName.value == '') {
      createMessage.error('请选择文件');
      return;
    }
    uploadLicense(
      {
        id: devItem.value.id,
        path: selectFileName.value,
      },
      updateCallbackSuccess,
      updateCallbackError
    );
    emits('handleLicenseOk');
  }

  function updateCallbackSuccess() {
    closeModal();
  }
  function updateCallbackError() {}

  function uploadChange(info) {
    if (info.file.status === 'done') {
      if (info.file.response.success) {
        createMessage.success(`${info.file.name} 上传成功!`);
        selectFileName.value = info.file.name;
      } else {
        createMessage.error(`${info.file.response.message}`);
      }
    } else if (info.file.status === 'error') {
      createMessage.error(`${info.file.response.message}`);
    }
  }
</script>

<style scoped></style>
