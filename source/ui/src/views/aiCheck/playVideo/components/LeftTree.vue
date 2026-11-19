<template>
  <div>
    <a-tree :tree-data="treeData" checkable key="key" :default-expand-all="isAutoExpand" @check="handleCheck" :disabled="isDisabled">
      <template #title="{ title, cameraCount, disabled }">
        <span>{{ title }}</span>
        <label v-if="type !== 'camera'">{{ cameraCount > 0 ? '(' + cameraCount + ')' : '' }}</label>
        <slot v-if="disabled && !cameraCount"></slot>
      </template>
    </a-tree>
  </div>
</template>
<script lang="ts" setup>
  import { defineEmits, computed } from 'vue';
  import { Tree } from 'ant-design-vue';
  import { TreeNodeData } from '../playVideoList.data';

  const props = defineProps<{
    treeData: TreeNodeData[];
    isAutoExpand?: boolean;
    type: string;
    isDisable: boolean;
  }>();
  const ATree = Tree;
  const emits = defineEmits(['handleCheck', 'val']);

  //点击复选框
  const handleCheck = (checkedKeys, e) => {
    emits('handleCheck', checkedKeys, e);
  };

  const isDisabled = computed({
    get() {
      return props.isDisable;
    },
    set(val) {
      emits('val', val);
    },
  });
</script>
<style lang="less" scoped>
  :deep .ant-tree-list {
    margin: 12px 0 0 20px;
    color: var(--headerline-text-color);
    font-size: 13px;
  }
  :deep .ant-tree-list-holder-inner {
    height: calc(100vh - 142px);
    overflow-y: scroll;
  }
</style>
