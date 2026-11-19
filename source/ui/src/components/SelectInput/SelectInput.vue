<template>
  <div class="select-input" ref="select">
    <div class="default-select" :key="key">
      <slot name="icon"></slot>
      <!-- 类似下拉菜单的输入框可以自己输入 -->
      <div class="input-wrap">
        <!-- 用户输入的,根据内容搜索/用户自己选择 -->
        <div class="wrapper">
          <div tabindex="0" contenteditable="false" @focus="handleFocus" @blur="handleBlur" class="input-select" :data-text="placeholder">
            <span v-for="(item, index) of value" :key="index" class="select-item">
              <span class="content-item" :title="`${item}`">{{ item.title + '、' }}</span>
            </span>
            <span class="suffix" v-if="value?.length > 0">等{{ value?.length }}个相机 </span>
          </div>
        </div>
        <i class="dropdown" @click="handleShowDrop"></i>
      </div>
    </div>
    <div class="select" v-if="mainState.isShow" @mouseenter="handleEnter">
      <slot name="content"></slot>
      <div class="footer">
        <a class="add-camera" @click="handleAddCamera">添加更多相机>></a>
        <BaseButton :type="'sure'" class="sure-btn" @click="handleClose" id="sure-btn" primary-buttton="确认" />
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { reactive, ref, defineProps } from 'vue';
  import { router } from '/@/router';
  import BaseButton from '../BaseButton/BaseButton.vue';
  defineProps({
    // defaultValue: String,
    isRead: Boolean,
    value: Array,
    placeholder: String,
    key: String,
  });
  const emits = defineEmits(['handleSure', 'handleFocus', 'handleEnter', 'val']);
  const mainState = reactive({
    isShow: false,
    isFlag: false,
    inputValue: '',
  });

  const select = ref<HTMLElement | null>(null);

  // const defaultVal = computed({
  //   get() {
  //     return props.placeholder;
  //   },
  //   set(val) {
  //     emits('val', val);
  //   },
  // });
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
  //靠近
  const handleEnter = () => {
    mainState.isFlag = true;
    // emits('handleEnter');
  };
  // 点击确认关闭
  const handleClose = () => {
    mainState.isShow = false;
    //点击确认将节点放到输入框，checked或click不给输入框
    emits('handleSure');
  };
  // 点击添加相机???弹窗//todo
  const handleAddCamera = () => {
    router.push('/base/camera');
  };

  //点击箭头出现下拉框
  const handleShowDrop = () => {
    mainState.isShow = true;
    document.addEventListener('click', listenClick);
  };
</script>
<style lang="less" scoped>
  .select-input {
    margin-right: 40px;
    position: relative;

    .default-select {
      width: 320px;
      height: 28px;
      border: 1px solid;
      border-color: var(--border-color);
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      border-radius: 4px;
      position: relative;

      &:hover {
        border-color: #adb4cc;
      }
      &:active {
        border-color: #738ce6;
      }
      // @media screen and (max-width: 1200px) {
      //   .default-select {
      //     width: 88px;
      //   }
      // }
    }
    .input-wrap {
      .clear-icon {
        width: 12px;
        height: 12px;
      }

      .dropdown {
        // width: 0px;
        // height: 0px;
        // border: 4px solid transparent;
        // border-top: 4px solid #969cb3;
        background: url('/@/assets/svg/icon/icon_arrowdown_default.svg');
        display: inline-block;
        width: 8px;
        height: 8px;
        position: absolute;
        top: 10px;
        right: 12px;
        cursor: pointer;
        &:hover {
          background: url('/@/assets/svg/icon/icon_arrowdown_hover.svg');
        }
        &:active {
          background: url('/@/assets/svg/icon/icon_arrowdown_actived.svg');
        }
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
      width: 130px;
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
      background-color: var(--background-Color);
      box-shadow: 0px 0px 6px rgba(51, 63, 102, 0.1);
      position: absolute;
      top: 28px;
      z-index: 9999;
      margin-top: 5px;
    }
    .footer {
      display: flex;
      align-items: center;
      height: 0;
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
