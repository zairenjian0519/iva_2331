<template>
  <BasicModal @register="registerModal"
              title="添加图片"
              @ok="handleOk">
      <a-upload v-model:file-list="formState.fileList"
                :max-count="1"
                list-type="picture"
                @remove="handleRemove"
                :before-upload="beforeUpload">
        <a-button type="primary">
          <upload-outlined></upload-outlined>
          点击上传
        </a-button>
      </a-upload>
  </BasicModal>
</template>

<script setup lang="ts">
import {UploadOutlined} from "@ant-design/icons-vue";
import BasicModal from "/@/components/Modal/src/BasicModal.vue";
import {useModalInner} from "/@/components/Modal";
import {saveData} from "/@/views/aiCheck/baseImage/BaseImage.api";
import {useMessage} from "/@/hooks/web/useMessage";
import {reactive} from "vue";
import {UploadProps} from "ant-design-vue";

const formState = reactive({
  fileList: [],
});

const emits = defineEmits(["refreshTable"])

const createMessage = useMessage().createMessage;

const [registerModal, {closeModal, setModalProps}] = useModalInner(() => {
})

const handleRemove: UploadProps['onRemove'] = file => {
  const index = formState.fileList.indexOf(file);
  const newFileList = formState.fileList.slice();
  newFileList.splice(index, 1);
  formState.fileList = newFileList;
};

const beforeUpload: UploadProps['beforeUpload'] = file => {
  formState.fileList = [...formState.fileList, file];
  return false;
};

const handleOk = async () => {
  const file = formState.fileList[0];
  if (!file) {
    createMessage.error("请选择图片");
    return
  }
  // 只能上传 jpg 或者 png 格式的图片
  if (!file.type.includes('image/jpeg') && !file.type.includes('image/png')) {
    createMessage.error("只能上传 jpg 或者 png 格式的图片");
    return
  }
  // 判断上传文件大小不能超过 500k
  if (file.size > 500 * 1024) {
    createMessage.error("文件大小不能超过 500K");
    return
  }

  const rawFile = getRawFile(file);
  if (!rawFile) {
    createMessage.error('无效图片');
    return;
  }
  setModalProps({
    loadingTip: "正在上传中...",
    loading: true,
    confirmLoading: true,
  });
  try {
    const formData = new window.FormData();
    formData.append('file', rawFile);
    await saveData(formData)
    emits('refreshTable')
  } finally {
    setModalProps({
      loading: false,
      confirmLoading: false,
    });
    closeModal();
  }
};

function getRawFile(fileItem: any): File | null {
  if (fileItem instanceof File) return fileItem;
  if (fileItem?.originFileObj instanceof File) return fileItem.originFileObj;
  if (fileItem?.file instanceof File) return fileItem.file;
  return null;
}
</script>

<style scoped lang="less">

</style>
