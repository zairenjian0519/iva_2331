<template>
  <div ref="importModalContainer">
    <a-modal title="导入人脸"
             v-model:visible="visible"
             :get-container="getContainer"
             :closable="false"
             :show-cancel="false"
             @cancel="handleCancel"
             @ok="importFace">
      <div class="container">
        <img :src="faceInfo.picPath" alt="">
        <div style="height: 10px"></div>
        <a-form ref="importForm"
                :label-col="{ span: 7 }"
                :wrapper-col="{ span: 12 }"
                :model="formState"
                :rules = "rules">
          <a-form-item label="人脸组" name="faceGroup">
            <a-select v-model:value="formState.faceGroup" placeholder="请选择人脸组">
              <a-select-option v-for="item in faceGroupList" :key="item.id" :value="item.id">{{ item.groupName }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="姓名" name="faceName">
            <a-input v-model:value="formState.faceName" placeholder="请输入姓名"></a-input>
          </a-form-item>
          <a-form-item label="证件号" name="faceIdCard">
            <a-input v-model:value="formState.faceIdCard" placeholder="请输入证件号"></a-input>
          </a-form-item>
          <a-form-item label="手机号" name="facePhone">
            <a-input v-model:value="formState.facePhone" placeholder="请输入手机号"></a-input>
          </a-form-item>
          <a-form-item label="备注" name="faceRemark">
            <a-input v-model:value="formState.faceRemark" placeholder="备注信息"></a-input>
          </a-form-item>
        </a-form>
      </div>
    </a-modal>
  </div>

</template>

<script setup lang="ts">

import {reactive, ref} from "vue";
import {getList} from "/@/views/face/storage/group.api";
import {importSingleFace} from "/@/views/face/storage/detail.api";

const visible = ref(false)
const importModalContainer = ref()
const importForm = ref()
const faceInfo = ref({
  id: '',
  picPath: ''
})

const formState = reactive({
  faceGroup: undefined,
  faceName: '',
  facePhone: '',
  faceIdCard: '',
  faceRemark: ''
})

const rules = {
  faceGroup: [
    { required: true, message: '请选择人脸组', trigger: 'change' }
  ],
  faceName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 4, message: '长度在 2 到 4 个字符', trigger: 'blur' }
  ],
}

const getContainer = () => {
  return importModalContainer.value
}

const faceGroupList = ref([])
const open = (faceParams: any) => {
  faceInfo.value = faceParams
  getList().then((data) => {
    faceGroupList.value = data
  })
  visible.value = true
}

const importFace = () => {
  importForm.value.validate().then(() => {
    const params = {
      alarmDetailId: faceInfo.value.id,
      faceGroup: formState.faceGroup,
      faceName: formState.faceName,
      faceIdCard: formState.faceIdCard,
      facePhone: formState.facePhone,
      remark: formState.faceRemark
    }
    importSingleFace(params)
    visible.value = false
  }).catch((err) => {
    console.log('校验失败', err)
  })
}

const handleCancel = () => {
  importForm.value.resetFields()
  visible.value = false
}

defineExpose({
  open
})

</script>

<style scoped lang="less">
.container {
  width: 100%;
  margin-top: 10px;

  :deep(.ant-modal-close) {
    margin-right: 10px;
  }

  img {
    min-width: 100px;
    width: 100px;
    max-width: 100px;
    height: 100px;
    margin: 0 auto;
  }


}
</style>
