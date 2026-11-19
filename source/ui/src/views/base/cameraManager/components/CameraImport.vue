<template>
  <div ref="importModal">
    <Modal class="importStyle" title="导入" okText="导入"
           v-model:visible="modalVisible"
           style="width: 620px"
           :okButtonProps="okButtonProps"
           @ok="handleUploadFile"
           @cancel="handleCancel"
           :get-container="getContainer">
      <div style="margin-top: 30px; margin-left: 60px">
        <div>
          <span>请点击 </span> <span><a href="javascript:void(0)"
                                        @click="downloadTemplate">Excel 模板</a></span> <span>下载并编辑需要导入的相机信息</span>
        </div>
        <div style="margin-top: 16px; margin-right: 60px">
          <span style="color: #aaaaaa;">
          注：可导入相机上限根据设备能力情况而定，具体情况请咨询厂家。
          </span>
        </div>
        <div style="margin-top: 20px">
          <span> 请选择要上传的Excel文件 </span>
        </div>
      </div>

      <div style="height: 300px; margin-left: 55px">
        <a-upload :file-list="fileList" :max-count="1" accept=".xlsx,.xls"
                  :before-upload="beforeUploadFile" @remove="handleRemove">
          <div class="uploadStyle">
            <div style="margin-left: 40%; margin-top: 70px">
              <div>
                <span> {{ mainInfo.fileName }}</span>
              </div>
              <div :class="mainInfo.fileName !== '' ? 'buttonStyleName' : 'buttonStyle'">
                <a-button
                  style="border-radius: 7px; height: 26px; align-items: center; display: flex"> 重新选择
                </a-button>
              </div>
            </div>
          </div>
        </a-upload>
        <div v-if="mainInfo.success" style="display: flex; align-items: center; margin-top: 20px">
          <span class="redPointSuccess"></span>
          <span
            style="width: 30px; margin-left: 2px; font-family: Source Han Sans CN; color: #7bbdc7; font-size: 15px"> 完成 </span>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script lang="ts" setup>
import {Modal} from 'ant-design-vue';
import {getImportUrl, importXlsx} from '../CameraManager.api';
import {computed, defineExpose, reactive, ref} from 'vue';
import {useMessage} from '/@/hooks/web/useMessage';
import type {UploadProps} from 'ant-design-vue';

const importModal = ref<HTMLElement | null>(null);
const {createMessage} = useMessage();
const fileList = ref<UploadProps['fileList']>([]);
const emit = defineEmits(['handleImportCancel', 'finish']);
const props = defineProps<{
  visible: Boolean;
}>();

const okButtonProps = reactive({
  loading: false
})

const mainInfo = reactive({
  deviceId: '',
  success: false,
  fileName: '',
});

const modalVisible = computed(() => {
  return props.visible;
});

const handleRemove: UploadProps['onRemove'] = () => {
  fileList.value = [];
  mainInfo.success = false;
};

const beforeUploadFile: UploadProps['beforeUpload'] = (file) => {
  mainInfo.fileName = file.name;
  fileList.value = [file];
  mainInfo.success = false;
  return false;
};
// 下载导入模板
const downloadTemplate = () => {
  const hostname = window.location.hostname;
  const port = window.location.port;
  const protocol = window.location.protocol;
  const currentHost = `${protocol}//${hostname}${port ? ':' + port : ''}`;
  const fileUrl = `${currentHost}/camera_import_template.xlsx`;

  const a = document.createElement('a');
  a.href = fileUrl;
  a.download = '相机导入模板.xlsx'; // 指定下载文件名
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

// 导入 excel
function handleUploadFile() {
  if (fileList.value.length <= 0) {
    createMessage.error('请选择上传文件');
    return;
  }
  const file = fileList.value[0];
  const handleSuccess = (fileInfo) => {
    try {
      if (fileInfo.code === 201) {

      } else if (fileInfo.code === 500) {
        createMessage.error(fileInfo.message || `${file.name} 导入失败`);
      } else {
        createMessage.success(fileInfo.message || `${file.name} 文件上传成功`);
        okButtonProps.loading = false
        mainInfo.success = true;
        emit('finish')
        handleCancel()
      }
    } catch (error) {
      console.log('导入的数据异常', error);
    } finally {
      okButtonProps.loading = false
    }
  };
  const url = getImportUrl + '?id=' + mainInfo.deviceId;
  okButtonProps.loading = true
  importXlsx(url, file, handleSuccess)
}

const handleCancel = async () => {
  fileList.value = [];
  mainInfo.fileName = '';
  mainInfo.success = false;
  emit('handleImportCancel');
};

function setDeviceId(deviceId: string) {
  mainInfo.deviceId = deviceId;
}

// 制定modal挂载的节点
const getContainer = () => {
  return importModal.value;
};

defineExpose({
  setDeviceId,
});
</script>

<style lang="less" scoped>
.importStyle {
  font-size: 13px;

  .ant-modal-content {
    .ant-modal-close {
      margin-right: 20px;
    }
  }

  .uploadStyle {
    width: 480px;
    height: 150px;
    margin: 20px 0 20px 0;
    border: 1px dashed;
  }

  /deep/ .ant-modal-footer button {
    border-radius: 26px !important;
    height: 27px;
  }

  .buttonStyleName {
    margin-top: 10px;
  }

  .buttonStyle {
    margin-top: 110px;
  }

  .ant-upload-list {
    margin-left: 50px;
  }

  .redPointSuccess {
    color: var(--adopt-text-color);
    width: 16px;
    height: 16px;
    background-color: var(--adopt-text-color);
    border-radius: 8px;
  }
}

:deep .ant-modal-footer {
  .ant-btn {
    width: 64px;
    height: 24px;
    background-color: var(--fill-color);
    border-radius: 4px;
    color: var(--secondary-text-color);
    display: flex;
    align-items: center;
    cursor: pointer;

    &:hover {
      background-color: var(--fill-two-color);
      color: var(--primary-Color);
    }

    &:active {
      background-color: var(--input-text-color);
      border-color: #7089ed;
      color: var(--primary-Color-actived);
    }
  }
}

:deep .ant-modal-footer {
  display: flex;
  justify-content: flex-end;
}

:deep .ant-modal-footer {
  .ant-btn-primary {
    width: 64px;
    height: 24px;
    background-color: var(--primary-Color);
    display: flex;
    align-items: center;
    border-radius: 4px;
    justify-content: center;
    color: rgba(255, 255, 255, 0.95);
    border: none;
    cursor: pointer;

    &:hover {
      background-color: var(--primary-Color-hover);
      color: rgba(255, 255, 255, 0.95);
    }

    &:active {
      background-color: var(--primary-Color-actived);
    }

    &:disabled {
      background-color: var(--fill-color);
    }
  }
}

:deep .ant-modal-content {
  .ant-modal-close {
    margin-right: 12px;
  }
}
</style>
