<template>
  <div class="base-label">
    <div :class="label">
      <span class="label-text">{{ levelToText[label] }}</span>
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { propTypes } from '/@/utils/propTypes';

  defineProps({
    label: propTypes.string.isRequired,
  });

  const levelToText = {
    warning: '警告',
    common: '一般',
    serious: '严重',
    remind: '提醒',
    normal: '正常',
    pendingReview: '待审核',
    adopt: '已通过',
    cancel: '已作废',
  };
</script>
<style lang="less" scoped>
  .base-label {
    display: flex;
    justify-content: center;
    align-items: center;
    .remind {
      width: 56px;
      max-height: 24px;
      min-height: 22px;
      background: var(--remind-color);
      border-radius: 2px;
      text-align: center;
      .label-text {
        font-family: Source Han Sans CN;
        color: var(--remind-text-color);
        font-size: 12px;
      }
    }
    .common {
      width: 56px;
      max-height: 24px;
      min-height: 22px;
      background-color: var(--common-color);
      border-radius: 2px;
      text-align: center;
      .label-text {
        font-family: Source Han Sans CN;
        color: var(--common-text-color);
        font-size: 12px;
      }
    }

    .warning {
      width: 56px;
      max-height: 24px;
      min-height: 22px;
      background-color: var(--warning-color);
      border-radius: 2px;
      text-align: center;
      .label-text {
        font-family: Source Han Sans CN;
        color: var(--warning-text-color);
        font-size: 12px;
      }
    }
    .serious {
      width: 56px;
      max-height: 24px;
      min-height: 22px;
      background-color: var(--serious-color);
      border-radius: 2px;
      text-align: center;
      .label-text {
        font-family: Source Han Sans CN;
        color: var(--serious-text-color);
        font-size: 12px;
      }
    }
    .pendingReview {
      width: 56px;
      max-height: 24px;
      min-height: 22px;
      background-color: var(--serious-color);
      border-radius: 2px;
      text-align: center;
      .label-text {
        font-family: Source Han Sans CN;
        color: var(--serious-text-color);
        font-size: 12px;
      }
    }
    .adopt {
      width: 56px;
      max-height: 24px;
      min-height: 22px;
      background-color: var(--adopt-color);
      border-radius: 2px;
      text-align: center;
      .label-text {
        font-family: Source Han Sans CN;
        color: var(--adopt-text-color);
        font-size: 12px;
      }
    }
    .cancel {
      width: 56px;
      max-height: 24px;
      min-height: 22px;
      background-color: var(--cancel-color);
      border-radius: 2px;
      text-align: center;
      .label-text {
        font-family: Source Han Sans CN;
        color: var(--cancel-text-color);
        font-size: 12px;
      }
    }
    .normal {
      width: 56px;
      max-height: 24px;
      min-height: 22px;
      background-color: var(--adopt-color);
      border-radius: 2px;
      text-align: center;
      .label-text {
        font-family: Source Han Sans CN;
        color: var(--adopt-text-color);
        font-size: 12px;
      }
    }
  }
</style>
