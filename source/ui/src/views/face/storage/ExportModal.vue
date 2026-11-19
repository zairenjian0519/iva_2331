<template>
  <a-modal title="导出数据"
           v-model:visible="visible"
           :get-container="getContainer"
           ok-text="导出"
           cancel-text="取消"
           :ok-button-props="{loading: exportBtnLoading}"
           @ok="exportData">
    <div class="exportBody">
      <p>注意：默认导出数据为 5000 条数据， 可以自行选择导出条数。</p>
      <div style="margin-bottom: 15px">
        起始位置：
        <a-input-number v-model:value="state.startNum" :min="0" :max="4999" style="width: 150px"/>
      </div>
      <a-radio-group v-model:value="state.exportNum">
        <a-radio value="1000">1000 条</a-radio>
        <a-radio value="2000">2000 条</a-radio>
        <a-radio value="3000">3000 条</a-radio>
        <a-radio value="4000">4000 条</a-radio>
        <a-radio value="5000">5000 条</a-radio>
      </a-radio-group>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {useMethods} from "/@/hooks/system/useMethods";


const visible = ref(false);
defineProps(['getContainer'])

const state = reactive({
  startNum: 0,
  exportNum: '5000'
})

let groupId;
const open = (tempGroupId: string) => {
  groupId = tempGroupId;
  state.startNum = 0;
  state.exportNum = '5000';
  visible.value = true;
}

const exportBtnLoading = ref(false);
const exportData = async () => {
    try {
      exportBtnLoading.value = true;
      await useMethods().handleExportXlsx("人脸数据", '/faceDetail/export',{
        groupId,
        startNum: state.startNum,
        exportNum: state.exportNum
      })
    }finally {
      exportBtnLoading.value = false;
    }
}

defineExpose({
  open
})
</script>

<style scoped lang="less">
.exportBody {
  padding: 20px;
}
</style>
