<template>
  <div class="secondary-button">
    <slot></slot>
    <span class="label">{{ label }}</span>
  </div>
</template>
<script lang="ts" setup>
  defineProps<{
    label: String;
  }>();
</script>
<style lang="less" scoped>
  .secondary-button {
    padding-left: 5px;
    padding-right: 15px;
    height: 24px;
    background-color: var(--fill-color);
    border: 1px solid;
    border-color: var(--fill-color);
    display: flex;
    justify-content: center;
    align-items: center;
    font-family: Source Han Sans CN;
    border-radius: 4px;
    font-size: 13px;
    cursor: pointer;
    .label {
      margin-left: 6px;
    }
    &:hover {
      .label {
        color: var(--primary-Color-hover);
      }
    }
    &:active {
      .label {
        color: var(--primary-Color-actived);
      }
    }
  }
</style>
