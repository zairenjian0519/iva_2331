<template>
  <div style="margin: 10px;">
    <a-spin tip="读取网卡信息中..." :spinning="!isDataLoaded" :delay="300" size="large">

      <a-row :gutter="[10,10]" v-if="isDataLoaded">
        <a-col :span="8" v-for="(item, index) in ipList" :key="index">
          <CardConfig :formData="item"/>
        </a-col>
      </a-row>

      <a-card :body-style="cardBodyStyle" v-if="!isDataLoaded">
        <img src="../../assets/images/nodata.png" :width="300" alt="暂无数据"/>
        <div style="font-size: 16px; font-weight: bolder;">
          <p>1. 请检查网线是否插好</p>
          <p>2. 网口灯是否亮起</p>
          <p>3. 再次刷新网页尝试获取网口参数</p>
        </div>
      </a-card>

    </a-spin>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import {message} from 'ant-design-vue';
import CardConfig from './components/CardConfig.vue';
import {list} from './network.api'

const cardBodyStyle = {
  height: '92.6vh',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center'
}

const isDataLoaded = ref(false)

const ipList = ref([])

async function fetchData() {
  setTimeout(() => {
    if (!isDataLoaded.value) {
      message.error("未获取到网口数据")
    }
  }, 3000)
  try {
    const response = await list();
    console.log(response)
    if (response && response.length > 0) {
      isDataLoaded.value = true
      ipList.value = response.map(item => {
        return {
          networkCard: item.networkCard,
          name: item.name,
          ipMode: item.ipMethod,
          ip: item.ipAddress,
          subnet: item.subnetMask,
          gateway: item.gateway,
          dns1: item.dns[0] ? item.dns[0] : '',
          dns2: item.dns[1] ? item.dns[1] : ''
        }
      });
    }
  } catch (error) {
    console.error('API 请求失败:', error);
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style>
/* 添加你需要的样式 */
</style>
