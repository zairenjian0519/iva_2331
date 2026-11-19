<template>
  <div class="select-input" ref="select">
    <div class="default-select">
      <slot name="icon"></slot>
      <!-- 类似下拉菜单的输入框可以自己输入 -->
      <div class="input-wrap">
        <!-- 用户输入的,根据内容搜索/用户自己选择 -->
        <div class="wrapper">
          <div tabindex="0" contenteditable="true" @focus="handleFocus" @blur="handleBlur" class="input-select" :data-text="defaultValue">
            <span v-for="(item, index) of value" :key="index" class="select-item">
              <span class="content-item" :title="`${item}`">{{ item.title + '、' }}</span>
            </span>
            <span class="suffix" v-if="value?.length > 0">等{{ value?.length }}个相机</span>
          </div>
        </div>
        <div class="dropdown"></div>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { reactive, ref, defineProps } from 'vue';
  defineProps({
    defaultValue: String,
    isRead: Boolean,
    value: Array,
  });
  const emits = defineEmits(['handleAddCamera', 'handleSure', 'handleFocus', 'handleEnter']);
  const mainState = reactive({
    isShow: false,
    isFlag: false,
    inputValue: '',
  });

  const select = ref<HTMLElement | null>(null);
  // 失去焦点
  const handleBlur = () => {
    mainState.isShow = mainState.isFlag;
  };
  const listenClick = (event) => {
    const e = event || window.event;
    if (select.value && !select.value.contains(e.target)) {
      mainState.isShow = false;
      document.removeEventListener('click', listenClick);
    }
  };

  // 获取焦点
  const handleFocus = () => {
    mainState.isShow = true;
    emits('handleFocus');
    document.addEventListener('click', listenClick);
  };
</script>
<style lang="less" scoped>
  .select-input {
    position: relative;
    min-width: 88px;
    width: 180px;
    .default-select {
      width: 180px;
      height: 28px;
      border: 1px solid;
      border-color: #d3d9e8;
      display: flex;
      align-items: center;
      border-radius: 4px;
      &:hover {
        border-color: #adb4cc;
      }
      &:active {
        border-color: #738ce6;
      }
    }
    .input-wrap {
      .clear-icon {
        width: 12px;
        height: 12px;
      }

      .dropdown {
        width: 0px;
        height: 0px;
        border: 4px solid transparent;
        border-top: 4px solid #969cb3;
        position: absolute;
        top: 10px;
        left: 90%;
      }
    }
    .select-item {
      height: 18px;

      &:hover {
        cursor: pointer;
      }
    }
    .wrapper {
      width: 230px;
    }
    .suffix {
      position: absolute;
      right: 35px;
    }
    .input-select {
      width: 180px;
      height: 18px;
      margin: 2px 8px 7px 4px;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      &:empty:before {
        content: attr(data-text);
        color: rgba(204, 210, 223, 0.902);
      }
    }

    .select {
      width: 480px;
      height: 360px;
      background-color: #ffffff;
      box-shadow: 0px 0px 6px rgba(51, 63, 102, 0.1);
      position: absolute;
      z-index: 9999;
      background: #ffffff;

      margin-top: 5px;
    }
    .footer {
      display: flex;
      align-items: center;
      height: 15px;
      justify-content: space-between;
      .add-camera {
        color: var(--cancel-text-color);
        margin-left: 12px;
      }
      .sure-btn {
        margin-right: 12px;
      }
    }
  }
</style>
