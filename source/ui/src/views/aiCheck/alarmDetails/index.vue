<template>
  <div class="card-container">
    <a-tabs v-model:activeKey="activeKey" size="small" @change="tabChange">
      <a-tab-pane key="1" tab="行为数据">
        <alarm-details-list />
      </a-tab-pane>
      <a-tab-pane key="2" tab="人脸数据">
        <face-alarm-list />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import {ref} from "vue";

import AlarmDetailsList from "./behavioralData/AlarmDetailsList.vue";
import FaceAlarmList from "./faceData/index.vue";

const activeKey = ref('1');

const tabChange = (key: string) => {
  activeKey.value = key;
}
</script>

<style scoped lang="less">
.card-container {
  margin: 5px 7px 0 5px;
  padding: 0 15px;
  background: #ffffff;
}
</style>
