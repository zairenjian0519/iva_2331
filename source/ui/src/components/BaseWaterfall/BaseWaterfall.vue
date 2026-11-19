<template>
  <div ref="container" class="container">
    <slot></slot>
  </div>
</template>
<script lang="ts" setup>
  import { useThrottleFn } from '@vueuse/core';
  import { onMounted, ref, onUnmounted } from 'vue';
  const container = ref<HTMLElement>(null);
  const emits = defineEmits(['handleLoadingMore']);
  const scroll = () => {
    if (container.value?.clientHeight + container.value?.scrollTop > container.value?.scrollHeight) {
      // 进行下拉加载更多
      emits('handleLoadingMore');
    }
  };
  const scrollHandler = () => {
    useThrottleFn(scroll, 100);
  };
  onMounted(() => {
    window.addEventListener('scroll', scrollHandler);
  });
  onUnmounted(() => {
    window.removeEventListener('scroll', scrollHandler);
  });
</script>
<style lang="less" scoped></style>
