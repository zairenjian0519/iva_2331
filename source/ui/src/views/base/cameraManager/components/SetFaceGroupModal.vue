<template>
  <a-modal
    v-bind:visible="visible"
    title="人脸库配置"
    :closable="false"  >

    <template #footer>
      <a-button type="default" @click="cancel">取消</a-button>
      <a-button type="danger" @click="deleteCameraFaceGroup">删除</a-button>
      <a-button type="primary" @click="saveOrUpdateFaceSetting">确定</a-button>
    </template>

    <div style="width: 100%;  margin: 10px 0 10px 100px">
      <div style="margin-bottom: 10px">相机名称：{{ rowData.cameraName }}</div>
      <div style="margin-bottom: 10px">
        人脸库名称：
        <a-select
          v-model:value="state.faceGroup"
          placeholder="请选择人脸库" style="width: 50%;">
          <a-select-option v-for="item in state.faceGroupData" :key="item.id" :value="item.id">
            {{ item.groupName }}
          </a-select-option>
        </a-select>
      </div>
      <div style="margin-bottom: 10px">人脸判断阈值：<a-input-number v-model:value="state.faceCheckThreshold" placeholder="请输入人脸判断阈值" style="width: 50%" /></div>
      <div>相似度阈值：<a-input-number v-model:value="state.faceSimilarityScore" placeholder="请输入人脸相似度阈值" style="width: 50%" /></div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {getList} from "/@/views/face/storage/group.api";
import {useMessage} from "/@/hooks/web/useMessage";
import {getById, saveFaceGroup} from "/@/views/base/cameraManager/CameraManager.api";

const visible = ref(false)
const state = reactive({
  faceGroupData: '',
  faceGroup: undefined,
  faceCheckThreshold: 80,
  faceSimilarityScore: 80
})

const rowData = ref({} as any)
const open = async (row: any) => {
  // 获取人脸组
  state.faceGroupData = await getList()
  const id = row.id
  const res = await getById({ id })
  rowData.value = res
  state.faceGroup = res.faceGroupId || undefined
  state.faceCheckThreshold = res.faceCheckThreshold
  state.faceSimilarityScore = res.faceSimilarityScore
  visible.value = true
}

const cancel = () => {
  state.faceGroupData = ''
  visible.value = false
}

const deleteCameraFaceGroup = async () => {
  const params: any = {
    cameraId: rowData.value.id,
    faceGroupId: "",
    faceCheckThreshold: 0,
    faceSimilarityScore: 0
  }
  await saveFaceGroup(params)
  state.faceGroup = undefined
  visible.value = false
}

const saveOrUpdateFaceSetting = async () => {
  if (!state.faceGroup) {
    useMessage().createMessage.error('请选择人脸库')
    return
  }
  const params: any = {
    cameraId: rowData.value.id,
    faceGroupId: state.faceGroup,
    faceCheckThreshold: state.faceCheckThreshold || 80,
    faceSimilarityScore: state.faceSimilarityScore || 80
  }

  await saveFaceGroup(params)
  visible.value = false
}

defineExpose({
  open
})

</script>

<style scoped lang="less">

</style>
