<template>
  <BasicDrawer v-bind="$attrs" @register="register">
    <div>
      <p v-if="logItems.length === 0">暂无日志，等待算法工作后后可查看实时日志</p>
      <p class="logItem" v-for="(log, index) in logItems" :key="index">{{ log }}</p>
    </div>
  </BasicDrawer>
</template>
<script setup lang="ts">
import {BasicDrawer, useDrawerInner} from '/@/components/Drawer';
import {onUnmounted, ref, unref} from "vue";
import {initAiEngine} from "/@/views/adapter/adapter.api";
import {connectWebSocket, offWebSocket, onWebSocket} from "/@/hooks/web/useWebSocketnew";
import {useUserStore} from "/@/store/modules/user";

const logItems = ref([]);

const [register, {setDrawerProps}] = useDrawerInner(async () => {
  setDrawerProps({
    title: '运行日志',
    width: '50%',
    bodyStyle: {
      background: '#000',
      color: '#fff'
    }
  })

  setDrawerProps({
    loading: true
  })

  // 初始化算法引擎
  await initAiEngine()

  // 建立websocket连接
  initWebSocket()
  onWebSocket(onWebSocketMessage)

  setDrawerProps({
    loading: false
  })
});

const userStore = useUserStore();

function initWebSocket() {
  let userId = unref(userStore.getUserInfo).id;
  const index = window.location.host.indexOf(':');
  const hostIp = window.location.host.slice(0, index);
  const algAdapterWebSocket = `ws://${hostIp}:8088/kyscene` + '/algorithm/adapter/' + userId
  connectWebSocket(algAdapterWebSocket);
}

const onWebSocketMessage = (data: any) => {
  console.log('接收到 websocket data：', data)
  logItems.value.push(data.msg)
  if (logItems.value.length > 100) {
    logItems.value.shift()
  }
}

onUnmounted(() => {
  offWebSocket(onWebSocketMessage)
})

</script>

<style scoped lang="less">
.logItem {
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-all;
}
</style>
