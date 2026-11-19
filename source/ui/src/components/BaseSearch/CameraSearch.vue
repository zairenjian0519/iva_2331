<template>
  <div class="search">
    <div class="select-box">
      <span class="camera-select">相机厂商</span>
      <a-select v-model:value="mainState.selectValue" :options="mainState.options" @change="handleChange">
        <template #suffixIcon><img :src="downIcon" alt="" style="margin-top: 5px" /></template>
      </a-select>
    </div>
    <div class="select-box">
      <span class="camera-select">相机名称</span>
      <a-input class="inputClass" v-model:value="mainState.cameraValue" style="width: 180px; border-radius: 4px; height: 28px" />
    </div>
    <div class="select-box"
      ><span class="alarm-type">事件类型</span>
      <div class="select">
        <TreeSelect
          :tree-data="mainState.treeData"
          ref="treeSelectRef"
          @change="handleChangeTree"
          tree-checkable
          v-model:value="mainState.categoryArray"
          key="key"
          tree-default-expand-all
          :multiple="true"
          :show-checked-strategy="ShowParent"
        />
        <img :src="downIcon" class="down-icon" alt="" />
      </div>
      <div class="search-btn">
        <BaseButton type="back" @click="handleSearch" second-button="查询" />
      </div>
      <div class="reset-btn">
        <BaseButton @click="handleReset" type="back" second-button="重置" />
      </div>
      <!-- <Button class="search-btn" @click="handleSearch">查询</Button>
      <Button class="reset-btn" @click="handleReset">重置</Button> -->
    </div></div
  >
</template>
<script lang="ts" setup>
  import { reactive, onMounted, defineEmits, ref, defineExpose } from 'vue';
  import { TreeSelect, Button } from 'ant-design-vue';
  import downIcon from '/@/assets/svg/icon/icon_arrowdown_default.svg';
  import type { TreeSelectProps } from 'ant-design-vue';
  import { alarmTreeData } from './BaseSearch.api';
  import { initDictOptions } from '/@/utils/dict';
  import BaseButton from '../BaseButton/BaseButton.vue';

  import { LegacyCheckedNode } from 'ant-design-vue/lib/vc-tree-select/TreeSelect';

  interface selectValue {
    value: string;
    label: string;
  }
  defineProps({
    backStyle: Boolean,
  });
  const emits = defineEmits(['getParentNode', 'handleSearch', 'handleReset']);
  const treeSelectRef = ref();
  const mainState = reactive({
    cameraValue: '',
    isFlag: false,
    isExpand: false,
    selectValue: '',
    options: [] as selectValue[],
    categoryArray: [] as string[],
    treeData: [] as TreeSelectProps[],
  });

  // 右边树
  const ShowParent = TreeSelect.SHOW_PARENT;

  //初始化
  onMounted(async () => {
    const dictData = await initDictOptions('factory_name');
    mainState.options = dictData as selectValue[];
    const alarmTreeRes = await alarmTreeData({});
    mainState.treeData = alarmTreeRes;
  });

  //点击确认将节点放到输入框，checked或click不给输入框
  const handleChange = (value: string) => {
    mainState.selectValue = value;
  };

  //事件树选择
  const handleChangeTree = (value, labelList, extra: { checked: boolean; allCheckedNodes: LegacyCheckedNode[] }) => {
    mainState.categoryArray = [];
    const { allCheckedNodes } = extra;
    allCheckedNodes?.forEach((item) => {
      const pop = item.node.props;
      if (item.children && item.children.length > 0) {
        //全选
        item.children.forEach((child) => {
          const childPop = child.node.props;
          if (childPop.value !== pop.value) {
            mainState.categoryArray.push(childPop.value);
          }
        });
      } else {
        mainState.categoryArray.push(pop.value);
      }
    });
  };

  // 查询
  const handleSearch = async () => {
    const selectObj = { factory: mainState.selectValue, categorys: mainState.categoryArray, cameraName: mainState.cameraValue };
    emits('handleSearch', selectObj);
  };
  //重置
  const handleReset = () => {
    clearSearch();
    handleSearch();
  };

  const clearSearch = () => {
    mainState.selectValue = '';
    mainState.cameraValue = '';
    mainState.categoryArray = [];
  };

  defineExpose({
    clearSearch,
  });
</script>
<style lang="less" scoped>
  .search {
    display: flex;
    width: fit-content;
    align-items: center;
    // flex-wrap: wrap;
    .parent_icon_info,
    .location_icon_info,
    .device_icon_info .device_icon_info {
      width: 16px;
      height: 16px;
    }
    /deep/ .ant-tabs-content {
      display: flex;

      height: 311px;
      overflow: auto;
    }

    /deep/ .inputClass {
      position: relative;
      min-width: 88px;
      width: 180px;
      border-color: var(--border-color);
      &:hover {
        border-color: #adb4cc;
      }
      &:active {
        border-color: #738ce6;
      }
    }
    /deep/ .ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn {
      color: #7089ed;
      &:hover {
        color: #7089ed;
      }
    }
    /deep/ .ant-tree-checkbox-checked .ant-tree-checkbox-inner {
      background-color: #7089ed;
      border-color: #7089ed;
    }
    /deep/ .ant-tree-checkbox-indeterminate .ant-tree-checkbox-inner::after {
      background-color: #7089ed;
    }
    .select-box {
      display: flex;
      align-items: center;
    }
    .camera-select {
      width: 72px;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      padding: 16px 4px 16px 16px;
      background-color: var(--background-Color);
    }
    /deep/ .ant-tabs-left > .ant-tabs-nav .ant-tabs-tab,
    .ant-tabs-right > .ant-tabs-nav .ant-tabs-tab,
    .ant-tabs-left > div > .ant-tabs-nav .ant-tabs-tab,
    .ant-tabs-right > div > .ant-tabs-nav .ant-tabs-tab {
      height: 24px;
      text-align: center;
    }
    /deep/ .ant-tabs > .ant-tabs-nav .ant-tabs-nav-list,
    .ant-tabs > div > .ant-tabs-nav .ant-tabs-nav-list {
      margin-top: 16px;
    }
    /deep/.ant-tabs-left > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane,
    .ant-tabs-left > div > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane {
      margin-top: 16px;
    }
    /deep/ .ant-tabs-ink-bar {
      background-color: #7089ed;
    }
    /deep/.ant-tabs-tab:hover {
      color: #7089ed;
    }

    .ant-divider-horizontal {
      display: flex;
      clear: both;
      width: 100%;
      min-width: 100%;
      margin-top: 1px;
    }
    .callapse-icon {
      background: url('/@/assets/svg/icon/icon_collapse_default.svg');
      display: inline-block;
      width: 13px;
      height: 13px;
      cursor: pointer;
      margin-right: 8px;
      margin-top: 4px;
      &:hover {
        background: url('/@/assets/svg/icon/icon_collapse_hover.svg');
      }
    }
    .expand-icon {
      background: url('/@/assets/svg/icon/icon_expand_default.svg');
      display: inline-block;
      width: 13px;
      height: 13px;
      cursor: pointer;
      margin-right: 8px;
      margin-top: 4px;
      &:hover {
        background: url('/@/assets/svg/icon/icon_expand_hover.svg');
      }
    }
    .control {
      color: var(--cancel-text-color);
    }
    /deep/ .ant-tree-treenode.ant-tree-treenode-switcher-close:hover {
      opacity: 60%;
      width: 328px;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      background-color: var(--fill-four-color);
    }
    /deep/ .ant-tree-treenode.ant-tree-treenode-switcher-open:hover {
      opacity: 60%;
      width: 328px;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      background-color: var(--fill-four-color);
    }
    /deep/ .ant-tree-checkbox-indeterminate .ant-tree-checkbox-inner {
      background-color: #fff;
      border-color: var(--border-color);

      background-color: #7089ed;
      border-color: #7089ed;
      &::after {
        width: 6px;
        height: 1.5px;
        background-color: #ffffff;
        border-radius: 0.4px;
      }
    }

    /deep/ .ant-tree-switcher .ant-tree-switcher-icon,
    .ant-tree-switcher .ant-select-tree-switcher-icon {
      // display: none;
      font-size: 10px;
      vertical-align: baseline;
    }
    /deep/ .ant-select-tree-list-holder {
      padding: 12px;
    }
    /deep/ .ant-tree-treenode.ant-tree-treenode-switcher-open.ant-tree-treenode-selected {
      opacity: 40%;
      width: 328px;
      background-color: #738ce6;
      border-radius: 4px;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
    }
    /deep/ .ant-tree .ant-tree-node-content-wrapper.ant-tree-node-selected {
      background-color: transparent;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
    }
    /deep/.ant-divider.ant-divider-horizontal.divider {
      width: 480px;
      height: 1px;
      background-color: rgba(211, 216, 230, 0.6);
    }

    .camera-icon {
      width: 16px;
      height: 16px;
      margin: 8px 8px;
    }
    .alarm-type {
      width: 52px;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      margin-left: 40px;
      margin-right: 12px;
      margin-top: 4px;
      // margin: 0px 12px 24px 40px;
    }
    /deep/ .ant-select:not(.ant-select-customize-input) .ant-select-selector {
      position: relative;
      background-color: var(--background-Color) !important;
      border: 1px solid var(--border-color);
      border-radius: 2px;
      transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
      width: 320px;
      min-width: 88px;
      height: 28px;
      border: 1px solid;
      border-color: var(--weak-split-line-color);
      border-radius: 4px;
    }
    .select {
      position: relative;
      /deep/ .ant-select-selector {
        &:hover {
          border-color: #adb4cc;
        }
      }
      /deep/.ant-tree-select-dropdown {
        padding: 8px 4px;
        height: 305px;
        overflow: auto;
      }
      /deep/span.ant-select-tree-switcher.ant-select-tree-switcher_open {
        display: none;
      }
      /deep/.ant-select-tree-checkbox-checked .ant-select-tree-checkbox-inner {
        background-color: #738ce6;
        border-color: #738ce6;
      }
      /deep/ .ant-select-tree-checkbox-indeterminate .ant-select-tree-checkbox-inner {
        background-color: #fff;
        border-color: var(--border-color);

        background-color: #7089ed;
        border-color: #7089ed;
        &::after {
          width: 6px;
          height: 1.5px;
          background-color: #ffffff;
          border-radius: 0.4px;
        }
      }
      // ant treeSelect样式不合理
      /deep/.ant-select-tree-node-content-wrapper .ant-select-tree-node-content-wrapper-open {
        background: #ffff;
      }
      /deep/ .ant-select-tree-checkbox-input:focus + .ant-select-tree-checkbox-inner {
        border-color: #7089ed;
      }
      /deep/.ant-select-tree-treenode.ant-select-tree-treenode-switcher-open.ant-select-tree-treenode-checkbox-checked {
        // background-color: #738ce6;
        border-radius: 4px;
        color: var(--secondary-text-color);
        opacity: 40%;
      }
      /deep/ .ant-select-tree-treenode .ant-select-tree-treenode-switcher-open:hover {
        opacity: 60%;
        font-family: Source Han Sans CN;
        color: var(--secondary-text-color);
        font-size: 13px;
        background-color: var(--fill-four-color);
      }
    }
    .down-icon {
      position: absolute;
      left: 95%;
      top: 14px;
      color: #969cb3;
    }
    .search-btn {
      margin: 0 12px 0 56px;
    }
    .reset-btn {
    }

    .back {
      font-family: Source Han Sans CN;
      color: #adb4cc;
      font-size: 13px;
      margin-left: 112px;
      cursor: pointer;
      &:hover {
        color: #757fa3;
      }
    }
    .back-alarm {
      font-family: Source Han Sans CN;
      color: #adb4cc;
      font-size: 13px;
      margin-left: 16px;
    }
  }
</style>
