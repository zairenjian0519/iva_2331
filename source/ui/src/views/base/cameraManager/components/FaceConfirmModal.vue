<template>
  <div>
    <!-- 自定义确认 Modal -->
    <a-modal
      v-model:visible="confirmVisible"
      :title="'重要提示'"
      :okText="'确定'"
      :cancelText="'取消'"
      :confirmLoading="confirmLoading"
      :closable = false
      @ok="handleConfirm"
      @cancel="handleCancel"
    >
      <p class="content">
        {{
          faceEnabled
            ? '开启人脸识别算法后，其他算法将自动删除？'
            : '确定关闭人脸识别算法？关闭后将删除该算法。'
        }}
      </p>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {faceAlgSwitch} from "/@/views/base/cameraManager/CameraManager.api";

const emits = defineEmits(['refreshTable'])

const faceEnabled = ref(false)
const confirmVisible = ref(false)
const confirmLoading = ref(false)

const openConfirm = (enable: boolean) => {
  faceEnabled.value = enable
  confirmVisible.value = true
}

const handleConfirm = async () => {
  confirmLoading.value = true
  try {
    await faceAlgSwitch({enable: faceEnabled.value}) // 你自己的后端方法
    refreshTable()
  }finally {
    confirmLoading.value = false
    confirmVisible.value = false
  }
}

const handleCancel = () => {
  confirmVisible.value = false
}

const refreshTable = () => {
  emits('refreshTable')
}

defineExpose({
  openConfirm,
})
</script>

<style lang="less" scoped>
.content {
  margin-top: 20px;
  width: 100%;
  margin-left: 20px;
  text-align: left;
}
</style>
