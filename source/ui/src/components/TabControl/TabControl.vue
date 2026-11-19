<template>
  <!-- tab切换 -->
  <div class="tab-control">
    <div v-for="(item, index) of title" :key="index" :class="{ active: mainState.currentIndex === index }" @click="itemClick(index)" class="tab-control-item">
      <span>{{ item }}</span>
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { reactive } from 'vue';
  defineProps({
    // 需要传的数组
    title: Array,
  });
  const emits = defineEmits(['clickItem']);
  const mainState = reactive({
    currentIndex: 0,
  });
  const itemClick = (index) => {
    // console.log(index, 'index');
    mainState.currentIndex = index;
    emits('clickItem', index);
  };
</script>
<style lang="less" scoped>
  .tab-control {
    display: flex;
    text-align: center;
    height: 24px;
    line-height: 24px;
    background-color: var(--background-Color);
    border: 2px solid;
    border-color: var(--netural-color);
    border-radius: 4px;

    margin: 0 24px 0 24px;

    &-item {
      width: 50%;
      cursor: pointer;

      span {
        font-family: Source Han Sans CN;
        color: var(--secondary-text-color);
        font-size: 12px;
      }
    }
    .active {
      background: var(--netural-color);
      width: 50%;
      span {
        font-family: Source Han Sans CN;
        color: var(--cancel-text-color);
        font-size: 12px;
      }
    }
  }
</style>
