<template>
  <div>
    <a-tooltip color="#ffffff" overlayClassName="overlay-class">
      <template #title>{{ tooltipTitle }}</template>
      <slot></slot>
    </a-tooltip>
  </div>
</template>
<script lang="ts" setup>
  import { Tooltip } from 'ant-design-vue';
  defineProps<{
    tooltipTitle: String;
  }>();
  const ATooltip = Tooltip;
</script>
<style>
  .overlay-class .ant-tooltip-inner {
    color: rgba(46, 47, 51, 1) !important;
  }
</style>
