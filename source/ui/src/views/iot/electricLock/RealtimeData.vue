<template>
  <div>
    <BasicModal v-bind="$attrs" destroyOnClose @register="registerInnerModal" @visibleChange="visibleChange">
      <div class="lock-info">
        <p><strong>锁名称：</strong>{{ model.deviceName }}</p>
        <p><strong>锁编号：</strong>{{ model.deviceCode }}</p>
        <p><strong>备注：</strong>{{ model.remark }}</p>
        <p><strong>实时数据：</strong>{{ realtimeData }}</p>
      </div>
    </BasicModal>
  </div>
</template>

<script setup lang="ts">
import {BasicModal, useModalInner} from "/@/components/Modal";
import {ref} from "vue";
import {getRealData} from "/@/views/iot/electricLock/lock.api";

const model = ref({});

const realtimeData = ref(0);

const count = ref(3);

let timer: any = null;

const [registerInnerModal, {setModalProps}] =useModalInner((data) => {
  setModalProps({
    title:  data.title + ' - 自动刷新 ( 0 )',
    width: 500,
    footer: null,
    closable: true,
    destroyOnClose: true,
  });
  if (data) {
    model.value = { ...data.record };
  }
  timer =setInterval(async () => {
    // 倒计时 3 秒
    setModalProps({
      title:  data.title + ' - 自动刷新 ( ' + count.value + ' )',
    });
    if (count.value == 0) {
      try {
        realtimeData.value = await getRealData({ id: data.record.id })
        count.value = 3;
      }catch (e) {
        clearInterval(timer)
      }
    } else count.value--
  }, 1000);
})

function visibleChange(visible:boolean) {
  if (!visible) {
    clearInterval(timer)
    timer = null;
    model.value = {};
    realtimeData.value = 0;
    count.value = 3;
  }
}

</script>

<style scoped lang="less">
.lock-info {
  padding-left: 20px;
}
</style>
