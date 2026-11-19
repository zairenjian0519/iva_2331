<template>
  <a-col class="offline">
    <div class="out-box">
      <img :src="cameraIcon" alt="" class="offline-icon" />
      <p class="text" @click="toManagement(element)">相机离线，请检查并编辑相机>></p>
    </div>
    <div class="bottom"
      ><span class="name">{{ abnormalName }}</span></div
    >
  </a-col>
</template>
<script lang="ts" setup>
  import cameraIcon from '/@/assets/svg/icon/media_icon_cameraoffline.svg';
  import { Col } from 'ant-design-vue';
  const ACol = Col;
  const emits = defineEmits(['toCameraEdit']);
  //去相机管理页面
  const toManagement = (e) => {
    emits('toCameraEdit');
  };
  defineProps({
    abnormalName: String,
    element: Object,
  });
</script>
<style lang="less" scoped>
  .offline {
    background-color: #17171a;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    aspect-ratio: 16 / 9;
  }
  .out-box {
    position: relative;
  }
  .offline-icon {
    margin-left: 65px;
    margin-bottom: 4px;
  }
  .text {
    cursor: pointer;
    font-family: Source Han Sans CN;
    color: rgba(255, 255, 255, 0.6);
    font-size: 13px;
    &:hover {
      opacity: 95%;
      color: #fff;
    }
  }
  .bottom {
    opacity: 80%;
    cursor: pointer;
    width: 96%;
    height: 35px;
    background-color: #0b0b0d;
    border-radius: 4px;
    position: absolute;
    bottom: 4px;
    display: flex;
    align-items: center;
  }
  .name {
    font-family: Source Han Sans CN;
    font-weight: 500;
    color: #ffffff;
    font-size: 13px;
    margin-left: 8px;
  }
</style>
