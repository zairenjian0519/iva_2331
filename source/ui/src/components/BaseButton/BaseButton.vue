<template>
  <div class="base-button">
    <template v-if="type === 'back'">
      <Button class="back">{{ secondButton }}</Button>
    </template>
    <template v-else-if="type === 'sure'">
      <Button class="sure">{{ primaryButtton }}</Button>
    </template>
  </div>
</template>
<script lang="ts" setup>
  import { Button } from 'ant-design-vue';
  defineProps({
    type: String,
    isConfirm: Boolean,
    primaryButtton: String,
    secondButton: String,
  });
</script>
<style lang="less" scoped>
  .base-button {
    font-family: Source Han Sans CN;
    font-size: 13px;
    .back {
      width: 64px;
      min-width: 36px;
      height: 24px;
      background-color: var(--fill-color);
      border: 1px solid;
      border-color: var(--fill-color);
      border-radius: 4px;
      display: flex;
      align-items: center;
      color: var(--thead-text-color);
      cursor: pointer;
      &:hover {
        color: var(--primary-Color);
        background-color: var(--fill-two-color);
      }
      &:active {
        background-color: var(--fill-color);
        color: var(--primary-Color-actived);
      }
      &:disabled {
        background-color: var(--primary-Color-actived);
        color: var(--thead-text-color);
        opacity: 0.2;
      }
    }
    .sure {
      width: 64px;
      min-width: 36px;
      height: 24px;
      background-color: var(--primary-Color);
      display: flex;
      align-items: center;
      border-radius: 4px;
      color: rgba(255, 255, 255, 0.95);
      border: none;
      cursor: pointer;
      &:hover {
        background-color: var(--primary-Color-hover);
      }
      &:active {
        background-color: var(--primary-Color-actived);
      }
      &:disabled {
        background-color: var(--fill-color);
      }
    }
  }
</style>
