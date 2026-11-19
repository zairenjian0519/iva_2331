<template>
  <a-modal v-model:visible="visible"
           title="导入人脸"
           :get-container="getContainer"
           :confirm-loading="importBtnLoading"
           :ok-text="'导入'"
           @ok="handleOk">
    <div style="margin: 15px">
      <p>点击<a href="javascript:void(0)" @click="downloadTemplate">下载模板</a>，并填写人脸信息，上传即可导入人脸数据。</p>
      <div style="line-height: 25px">
        <div>注意事项：</div>
        <div>1. 一次只能上传一个文件，文件大小不能超过 <code class="errorColor">100M</code>。</div>
        <div>2. 人脸图片仅支持<code class="errorColor"> jpg </code>和<code class="errorColor"> png </code>格式。</div>
        <div>3. 只能导入小于<code class="errorColor"> 5000 </code>行的数据。</div>
      </div>
      <a-upload-dragger
        v-model:fileList="fileList"
        name="file"
        :multiple="false"
        :max-count="1"
        :before-upload="beforeUpload"
      >
        <p class="ant-upload-drag-icon">
          <inbox-outlined></inbox-outlined>
        </p>
        <p class="ant-upload-text">单击或拖动文件到此区域以上传</p>
      </a-upload-dragger>

      <div v-if="errList.length > 0" style="margin-top: 15px; line-height: 15px">
        <p v-for="(msg, index) in errList" :key="index" class="errorColor">{{ msg }}</p>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {InboxOutlined} from "@ant-design/icons-vue";
import {importExcel} from "/@/views/face/storage/detail.api";
import {useMessage} from "/@/hooks/web/useMessage";

const fileList = ref([])
const importBtnLoading = ref(false)
const visible = ref(false)
defineProps(['getContainer'])
const emit = defineEmits(['refresh', 'initFaceGroup'])

const beforeUpload = (file: any) => {
  fileList.value = [...fileList.value, file];
  return false;
}

const downloadTemplate = () => {
  const hostname = window.location.hostname;
  const port = window.location.port;
  const protocol = window.location.protocol;
  const currentHost = `${protocol}//${hostname}${port ? ':' + port : ''}`;
  const fileUrl = `${currentHost}/face_import_template.xlsx`;

  const a = document.createElement('a');
  a.href = fileUrl;
  a.download = '人脸数据导入模板.xlsx'; // 指定下载文件名
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

const errList = ref([])
const handleOk = async () => {
  if (fileList.value.length === 0) {
    useMessage().createMessage.error('请选择文件')
    return
  }
  // 只能上传 xls 或者 xlsx 格式的文件
  const file = fileList.value[0]
  if (!file.type.includes('excel') && !file.type.includes('spreadsheetml')) {
    useMessage().createMessage.error('只能上传 xls 或者 xlsx 格式的文件')
    return
  }
  // 判断上传文件大小不能超过 100m
  if (file.size > 100 * 1024 * 1024) {
    useMessage().createMessage.error('文件大小不能超过 100M')
    return
  }
  // 包装参数，上传文件
  const params: any = {
    file: fileList.value[0].originFileObj,
    data: {
      groupId: groupId
    }
  }
  try {
    importBtnLoading.value = true
    await importExcel(params, { success: uploadCallback, isReturnResponse: false})
    // 初始化人脸库
    emit('initFaceGroup')
  } finally {
    importBtnLoading.value = false
  }
}

const uploadCallback = (res) => {
  if (res && res.code === 200) {
    // 判断ret 是字符串还是数组
    if (typeof res.result === 'string') {
      emit('refresh')
      useMessage().createMessage.info(res.result)
      visible.value = false
    } else {
      errList.value = res.result || []
    }
  }
}

let groupId;
const open = (tempGroupId: string) => {
  groupId = tempGroupId;
  visible.value = true
}

defineExpose({
  open
})
</script>

<style scoped lang="less">
  .errorColor {
    color: var(--serious-text-color)
  }
</style>
