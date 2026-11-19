<template>
  <Switch v-model:checked="switchChecked" @change="handleChange" :disabled="switchDisabled" />
</template>
<script lang="ts" setup>
  import { Switch } from 'ant-design-vue';
  import { computed } from 'vue';
  const props = defineProps({
    checked: Boolean,
    disabled: Boolean,
  });
  const emit = defineEmits(['val', 'handleChange']);

  const switchChecked = computed({
    get() {
      return props.checked;
    },
    set(val) {
      emit('val', val);
    },
  });
  const switchDisabled = computed({
    get() {
      return props.disabled;
    },
    set(val) {
      emit('val', val);
    },
  });
  //   变化开关
  const handleChange = () => {
    emit('handleChange');
  };
</script>
<style lang="less" scoped>
  .ant-switch {
    margin: 0;
    padding: 0;
    // color: rgba(0, 0, 0, 0.85);
    font-size: 14px;
    font-variant: tabular-nums;
    line-height: 1.5715;
    list-style: none;
    font-feature-settings: tnum;
    position: relative;
    display: inline-block;
    box-sizing: border-box;
    min-width: 20px;
    height: 8px;
    line-height: 8px;
    vertical-align: middle;
    border: 0;
    border-radius: 100px;
    cursor: pointer;
    transition: all 0.2s;
    -webkit-user-select: none;
    -moz-user-select: none;
    user-select: none;
    box-shadow: 0px 0px 1px rgba(51, 63, 102, 0.1);
  }
  :deep .ant-switch-handle {
    position: absolute;
    top: -1px;
    left: 0px;
    width: 10px;
    height: 10px;
    transition: all 0.2s ease-in-out;
    border-color: #e8ecf3;
  }
  .ant-switch-checked {
    background-color: #7089ed;
  }
  :deep .ant-switch-inner {
    display: block;
    margin: 0 7px 0 0px;
    color: #fff;
    font-size: 10px;
    transition: margin 0.2s;
  }
  :deep .ant-switch-handle::before {
    position: absolute;
    top: 0px;
    right: 0;
    bottom: 0;
    left: 0;
    background-color: #fff;
    border-radius: 9px;
    box-shadow: 0 2px 4px 0 rgb(0 35 11 / 20%);
    transition: all 0.2s ease-in-out;
    content: '';
  }
  .ant-switch-handle {
    position: absolute;
    top: -1px;
    left: 0px;
    width: 10px;
    height: 10px;
    transition: all 0.2s ease-in-out;
  }
  :deep .ant-switch-checked .ant-switch-handle {
    // left: calc(100% - 10px - 2px);
  }

  .ant-switch::after {
    position: absolute;
    top: -1px;
    left: 0px;
    width: 10px;
    height: 10px;
    background-color: #fff;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.36s cubic-bezier(0.78, 0.14, 0.15, 0.86);
    content: ' ';
    border: 1px solid;
    border-color: #adb4cc;
  }
  :deep .ant-switch-handle::before {
    display: none;
  }
  .ant-switch-checked::after {
    left: 70%;
    margin-left: 5px;
    transform: translateX(-100%);
  }
</style>
