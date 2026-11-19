<template>
  <button class="primary-button" :disabled="disabled">
    <slot></slot>
    <span class="label">{{ label }}</span>
  </button>
</template>
<script lang="ts" setup>
  defineProps<{
    label: String;
    disabled: Boolean;
  }>();
</script>
<style lang="less" scoped>
  .primary-button {
    width: 104px;
    height: 24px;
    background-color: var(--primary-Color);
    border-radius: 4px;
    display: flex;
    justify-content: center;
    align-items: center;
    font-family: Source Han Sans CN;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.95);
    &:hover {
      background-color: var(--primary-Color-hover);
    }
    &:active {
      background-color: var(--primary-Color-actived);
    }
    .label {
      margin-left: 6px;
    }
  }
</style>
