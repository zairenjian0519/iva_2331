<template>
  <div ref="realTimeModal">
    <Modal
      v-model:visible="dialogVisible"
      width="40%"
      :centered="true"
      dialogClass="dialog"
      :footer="null"
      :closable="false"
      :get-container="getContainer"
      :z-index="zIndex"
      :destroy-on-close="true">
      <template #title>
        实时报警信息：<span class="title-alarm-type">{{ alarmInfo['alarmEventType'] }}</span>
        <span style="position: absolute; right: 10px"> <ExclamationCircleFilled/> 按ESC开启或关闭弹窗</span>
      </template>
      <img :src="alarmInfo['alarmEventImg']"/>
      <div class="alarm-time">
        <span>相机名称：{{ alarmInfo['cameraName'] }}</span>
        <span style="margin-left: 20px">发生时间：{{ alarmInfo['alarmEventTime'] }}</span>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">

import {computed, ref} from "vue"
import noDataPng from '/@/assets/images/nodata.png';
import {formatToDateTime} from "/@/utils/dateUtil";
import {Modal} from 'ant-design-vue';
import {ExclamationCircleFilled} from '@ant-design/icons-vue';

const props = defineProps(["realTimeModalVisible", "alarmData"])
const emit = defineEmits(['toggleModalShow'])

const realTimeModal = ref<HTMLElement | null>(null);
// 制定modal挂载的节点
const getContainer = () => {
  return realTimeModal.value;
}

const zIndex = computed(() => Number.MAX_SAFE_INTEGER)

const dialogVisible = computed({
  get() {
    return props.realTimeModalVisible
  },
  set(val) {
    emit('toggleModalShow')
  }
})

const alarmDataInit = {
  cameraName: '未知',
  alarmEventType: '暂无事件',
  alarmEventImg: noDataPng,
  alarmEventTime: formatToDateTime(new Date())
}

const alarmInfo = computed(() => {
  const {alarmData} = props
  console.log('-===========', alarmData)
  if (!alarmData) return alarmDataInit
  return alarmData
})


</script>

<style scoped lang="less">
.title-alarm-type {
  color: red;
}

img {
  height: 420px;
  width: 100%;
}

.alarm-time {
  color: orange;
  width: 100%;
  margin-bottom: 0;
  position: absolute;
  bottom: 10px;
  text-align: right;
  padding-right: 15px;
  font-size: 15px;
  font-weight: bold;
}
</style>
