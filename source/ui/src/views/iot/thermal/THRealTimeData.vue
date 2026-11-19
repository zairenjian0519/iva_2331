<template>
  <BasicModal @register="registerModal" @visibleChange="visibleChange">
    <div class="ml-5">
      <p><strong>设备名称：</strong> {{ rawData.deviceName }}</p>
      <p><strong>设备编码：</strong> {{ rawData.deviceCode }}</p>
      <p><strong>IP地址：</strong> {{ rawData.ipAddress }}</p>
      <p><strong>最高温度读数：</strong> {{ rawData.temperatureMax }} ℃</p>
      <p><strong>最低湿度读数：</strong> {{ rawData.temperatureMin }} ℃</p>
      <p><strong>平均温度读数</strong> {{ rawData.temperatureAver }} ℃</p>
      <p><strong>中间值读数：</strong> {{ rawData.temperatureMid }} ℃</p>
      <p><strong>标准差读数：</strong> {{ rawData.temperatureStd }} ℃</p>
      <div style="text-align: center">
        <a-button type="primary" @click="handleRefresh" :loading="isSending">手动发送指令，3秒收自动刷新数据</a-button>
      </div>
    </div>
  </BasicModal>
</template>

<script setup lang="ts">

import {BasicModal, useModalInner} from "/@/components/Modal";
import {reactive, ref} from "vue";
import {getTHRealTimeData, getLastThermalRecord} from "./thsensor.api";
import {useMessage} from "/@/hooks/web/useMessage";
const { createMessage } =useMessage()

const rawData = reactive({
  id: '',
  deviceName: '',
  deviceCode: '',
  ipAddress: '',
  temperatureMax: 0,
  temperatureMin: 0,
  temperatureAver: 0,
  temperatureMid: 0,
  temperatureStd: 0,
})

const count = ref(3);
let timerInterval: any = null;
const [registerModal, {setModalProps}] = useModalInner((data:any) => {
  setModalProps({
    title: data.title,
    width: 500,
    confirmLoading: false,
    footer: null,
    destroyOnClose: true
  })
  rawData.id = data.record.id
  rawData.deviceName = data.record.deviceName
  rawData.deviceCode = data.record.deviceCode
  rawData.ipAddress = data.record.ipAddress
  rawData.temperatureMax = data.record.temperatureMax
  rawData.temperatureMin = data.record.temperatureMin
  rawData.temperatureAver = data.record.temperatureAver
  rawData.temperatureMid = data.record.temperatureMid
  rawData.temperatureStd = data.record.temperatureStd

  // 3秒刷新一次数据
  timerInterval =setInterval(async () => {
    // 倒计时 3 秒
    setModalProps({
      title:  data.title + ' - 自动刷新 ( ' + count.value + ' )',
    });
    if (count.value == 0) {
      try {
        const response = await getLastThermalRecord({ deviceId: data.record.id })
        if (response) {
          rawData.temperatureMax = response.temperatureMax
          rawData.temperatureMin = response.temperatureMin
          rawData.temperatureAver = response.temperatureAver
          rawData.temperatureMid = response.temperatureMid
          rawData.temperatureStd = response.temperatureStd
        }
        count.value = 3;
      }catch (e) {
        clearInterval(timerInterval)
      }
    } else count.value--
  }, 1000);
})

function visibleChange(visible:boolean) {
  if (!visible) {
    clearInterval(timerInterval)
    timerInterval = null;
    rawData.temperatureMax = 0
    rawData.temperatureMin = 0
    rawData.temperatureAver = 0
    rawData.temperatureMid = 0
    rawData.temperatureStd = 0
    count.value = 3;
  }
}

const isSending = ref(false);
// 手动发送指令, 1.5秒内只能点击一次
async function handleRefresh() {
  if (isSending.value) {
    createMessage.warning('请勿频繁发送指令')
    return
  }
  isSending.value = true
  try {
    await getTHRealTimeData({ deviceId: rawData.id })
    createMessage.success('指令发送成功')
  }finally {
    // 1 秒冷却时间
    setTimeout(() => {
      isSending.value = false
    }, 1500)
  }
}
</script>

<style scoped lang="less">

</style>
