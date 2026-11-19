<template>
  <a-modal title="批量导入人脸"
           v-model:visible="visible"
           :closable="false"
           :show-cancel="false"
           @cancel="handleCancel"
           @ok="submit">
      <div class="content">
        <span>选择人脸库：</span>
        <a-select placeholder="请选择人脸库" style="width: 60%" v-model:value="faceGroup">
          <a-select-option v-for="item in faceGroupList" :key="item.id" :value="item.id">
            {{ item.groupName }}
          </a-select-option>
        </a-select>
      </div>
  </a-modal>
</template>

<script setup lang="ts">

import {ref} from "vue";
import {getList} from "/@/views/face/storage/group.api";
import {batchImportFace} from "/@/views/face/storage/detail.api";

const visible = ref(false)
const faceGroup = ref('')
const faceGroupList = ref([])

const handleCancel = () => {
  visible.value = false
}

let selectArr = []
const submit = () => {
  batchImportFace({ faceGroup: faceGroup.value, ids: selectArr.join(',')})
  visible.value = false
}


const showModal = (ids: string[]) => {
  getList().then((data) => {
    faceGroupList.value = data
  })
  selectArr = ids
  visible.value = true
}

defineExpose({
 showModal
})


</script>

<style scoped lang="less">
  .content {
    width: 100%;
    margin: 15px auto;
    display: inline-flex;
    justify-content: center;
    align-items: center
  }
</style>
