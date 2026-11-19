<template>
  <RangePicker v-model:value="value1" show-time />
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import type { Dayjs } from 'dayjs';
  import { RangePicker } from 'ant-design-vue';
  // 不适应版本，不显示时间
  type RangeValue = [Dayjs, Dayjs];
  const value1 = ref<RangeValue>();
</script>
<style lang="less" scoped></style>
