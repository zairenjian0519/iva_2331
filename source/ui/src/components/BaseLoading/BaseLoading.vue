<template>
  <div class="loading">
    <img :src="loadingIcon" class="icon" />
  </div>
</template>
<script lang="ts" setup>
  import loadingIcon from '/@/assets/svg/icon/icon_loading.svg';
</script>
<style lang="less" scoped>
  .loading {
    background: #000000;
    width: 336px;
    height: 189px;
    display: flex;
    align-items: center;
    justify-content: center;
    .icon {
      width: 24px;
      height: 24px;
    }
  }
</style>
