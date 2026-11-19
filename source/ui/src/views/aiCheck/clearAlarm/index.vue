<template>
  <a-card title="告警事件清理规则配置" style="margin: 10px"
          :body-style="{display: 'flex', justifyContent:'flex-start'}">
    <a-form ref="formRef" style="width: 100%" :model="form" :label-col="{ span: 2 }"
            :wrapper-col="{ span: 8 }" :rules="rules">
      <a-form-item prop="usableSpace" label="磁盘保留空间阈值">
        <div style="display: inline-flex; align-items: center; margin-left: 10px">
          <a-input-number v-model:value="form.usableSpace" :max=1024 :min="1"
                          style="width: 100%"/>
          <div style="margin-left: 10px;">GB</div>
        </div>

      </a-form-item>

      <a-form-item label="告警信息保留时间" prop="fileRetentionDays">
        <div style="margin-left: 10px">
          <a-radio-group v-model:value="form.fileRetentionDays">
            <a-radio :value="1">最近1天</a-radio>
            <a-radio :value="3">最近3天</a-radio>
            <a-radio :value="7">最近7天</a-radio>
            <a-radio :value="15">最近15天</a-radio>
          </a-radio-group>
        </div>

      </a-form-item>

      <a-form-item :wrapper-col="{ offset: 2 }">
        <div style="margin-left: 10px">
          <a-button type="primary" @click="save()" :loading = 'saveLoading'>保存立即生效</a-button>
          <a-button type="primary" style="margin-left: 15px" @click="startPlan()" :loading="startLoading">立即执行</a-button>
          <a-button type="error" style="margin-left: 15px" @click="reset()" :loading="resetLoading">恢复默认配置</a-button>
        </div>
      </a-form-item>

      <a-form-item :wrapper-col="{ span: 24 }">
        <p><strong>告警清理规则如下：</strong></p>
        <p>按照保留时间去清理，清理后还小于空闲空间阈值， 保留时间会缩短一半，比如保留时间是15天，
          如果清理后仍然小于空闲空间阈值，那第一次清理后保留时间变为7天，
          直到清理出小于等于空闲空间阈值。</p>
      </a-form-item>
    </a-form>
  </a-card>

</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from "vue";
import {useMessage} from "/@/hooks/web/useMessage";
import {clearAlarmPlan, getByCode, saveOrUpdate} from "/@/views/aiCheck/clearAlarm/clearAlarm.api";

const {createMessage, createConfirm} = useMessage()

const formRef = ref()

const form = reactive({
  id: '',
  usableSpace: 1,
  fileRetentionDays: 7,
})

const rules = {
  usableSpace: [
    {required: true, message: '请输入磁盘空闲空间阈值', trigger: 'blur'}
  ],
  fileRetentionDays: [
    {required: true, message: '请选择剩余空间阈值', trigger: 'change'}
  ]
}

const saveLoading = ref(false)
const resetLoading = ref(false)
const startLoading = ref(false)

const reset = () => {
  createConfirm({
    iconType: 'warning',
    content: "确定恢复默认配置？",
    onOk: async ()=> {
      form.usableSpace = 1
      form.fileRetentionDays = 7
      try {
        resetLoading.value = true
        await save()
      }finally {
        resetLoading.value = false
      }
    }
  });
}

const save = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return
  let isUpdate = false
  const params = {
    paramName: '告警清理规则',
    paramCode: 'AlarmClearRule',
    description: '告警清理规则',
    paramVal: JSON.stringify({
      usableSpace: form.usableSpace,
      fileRetentionDays: form.fileRetentionDays
    })
  }
  if (form.id) {
    isUpdate = true
    params['id'] = form.id
  }
  try {
    saveLoading.value = true
    await saveOrUpdate(params, isUpdate)
    await loadData()
  }finally {
    saveLoading.value = false
  }
}

const loadData = async () => {
  const result = await getByCode({paramCode: 'AlarmClearRule'})
  const dbData = JSON.parse(result.paramVal)
  form.usableSpace = dbData.usableSpace
  form.fileRetentionDays = dbData.fileRetentionDays
  form.id = result.id
}

const startPlan = async () => {
  createConfirm({
    iconType: 'warning',
    content: "确定立即执行清理计划？",
    onOk: async ()=> {
      try {
        startLoading.value = true
        await clearAlarmPlan()
      }finally {
        startLoading.value = false
      }
    }
  });
}

onMounted(() => {
  loadData();
})
</script>

<style scoped lang="less">

</style>
