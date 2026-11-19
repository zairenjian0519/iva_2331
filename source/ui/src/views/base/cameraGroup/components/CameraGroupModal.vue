<template>
  <div ref="groupModal">
    <a-modal
      :title="titleModal"
      width="558px"
      centered
      v-model:visible="dialogVisible"
      :ok-text="okTextModal"
      :cancel-text="cancelTextModal"
      @ok="handleOk"
      @cancel="handleCancel"
      :get-container="getContainer"
    >
      <slot></slot>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import { Modal } from 'ant-design-vue';
  const AModal = Modal;

  const props = defineProps<{
    title: string;
    visible: boolean | undefined;
    okText: string;
    cancelText: string;
  }>();
  const emits = defineEmits(['val', 'handleOk', 'handleCancel', 'title', 'okText', 'cancelText']);
  const groupModal = ref<HTMLElement | string | false>();
  const dialogVisible = computed({
    get() {
      return props.visible;
    },
    set(val) {
      emits('val', val);
    },
  });

  const titleModal = computed({
    get() {
      return props.title;
    },
    set(val) {
      emits('title', val);
    },
  });

  const okTextModal = computed({
    get() {
      return props.okText;
    },
    set(val) {
      emits('okText', val);
    },
  });
  const cancelTextModal = computed({
    get() {
      return props.cancelText;
    },
    set(val) {
      emits('cancelText', val);
    },
  });

  // 制定modal挂载的节点
  const getContainer = () => {
    return groupModal.value;
  };

  //新建
  const handleOk = () => {
    emits('handleOk');
  };

  //取消
  const handleCancel = () => {
    emits('handleCancel');
  };
</script>
<style lang="less" scoped>
  :deep .ant-modal {
    .group-name,
    .camera-select,
    .title {
      color: var(--headerline-text-color);
      font-size: 13px;
    }
    .input-box {
      width: 300px;
      height: 28px;
      border: 1px solid var(--weak-split-line-color);
      border-radius: 4px;
      margin: 0 0 0 32px;
    }
    .group-name {
      margin: 28px 0 8px 32px;
    }

    .camera-select {
      margin: 16px 0 8px 32px;
    }

    .default-select {
      margin: 0 0 72px 32px;
    }
    .tips-icon {
      background: url('/@/assets/svg/icon/info_icon_tips.svg');
      display: inline-block;
      width: 14px;
      height: 14px;
      margin: 2px 4px 0 0;
    }
    .polling-gap {
      position: relative;
      &-item {
        margin: 28px 0 8px 32px;
      }
      &-input {
        height: 28px;
        border: 1px solid var(--weak-split-line-color);
        border-radius: 4px;
        margin: 6px 28px 0 0;
        width: 90%;
      }
    }
    // .divider {
    //   position: absolute;
    //   width: 1px;
    //   height: 12px;
    //   background-color: var(--input-text-color);
    //   right: 48px;
    //   top: 14px;
    // }
    .unit {
      position: absolute;
      right: 8px;
      top: 8px;
    }
    .ant-modal-footer {
      display: flex !important;
      justify-content: flex-end !important;
      align-items: center;
    }
    .ant-modal-footer .ant-btn {
      width: 64px;
      height: 24px;
      background-color: var(--fill-color);
      border-radius: 4px;
      color: var(--secondary-text-color);
      cursor: pointer;
      padding-top: 2px;
    }
    .ant-modal-footer .ant-btn:hover {
      background-color: var(--fill-two-color);
      color: var(--primary-Color);
    }
    .ant-modal-footer .ant-btn:active {
      background-color: var(--input-text-color);
      border-color: #7089ed;
      color: var(--primary-Color-actived);
    }
    .ant-modal-footer .ant-btn-primary {
      margin-left: 8px;
      width: 64px;
      height: 24px;
      background-color: var(--primary-Color);
      border-radius: 4px;
      color: rgba(255, 255, 255, 0.95);
      border: none;
      cursor: pointer;
    }

    .ant-modal-footer .ant-btn-primary:hover {
      background-color: var(--primary-Color-hover);
      color: rgba(255, 255, 255, 0.95);
    }
    .ant-modal-footer .ant-btn-primary:active {
      background-color: var(--primary-Color-actived);
    }
  }
</style>
