<template>
  <div class="search">
    <div class="select-box">
      <span class="camera-select">相机选择</span>
      <SelectInput :value="mainState.selectValue" @handle-enter="handleEnter" @handle-focus="handleFocus" @handle-sure="handleSure">
        <template #icon><img :src="cameraIcon" class="camera-icon" alt=""/></template>
        <template #content>
          <a-tabs v-model:activeKey="activeKey" :tab-position="mode" @change="handleChange">
            <a-tab-pane v-for="(item, index) of mainState.tabs" :key="index" :tab="`${item.label}`">
              <div class="input-search">
                <i class="search-icon" @keyup.enter="handleInputSearch"></i>
                <a-input v-model:value="searchValue" />
              </div>
              <div class="expand" @click="handleExpand">
                <span :class="[expandedkeys.length ? 'callapse-icon' : 'expand-icon']"></span>
                <span class="control">{{ expandedkeys.length ? '收起' : '展开' }}</span>
              </div>

              <a-tree
                :tree-data="treeData"
                @check="handleCheck"
                key="key"
                :auto-expand-parent="autoExpandParent"
                v-model:checkedKeys="checkedKeys"
                checkable
                showIcon
                :expandedKeys="expandedkeys"
                @expand="onExpand"
                @select="handleSelect"
              >

                <!-- 厂区 -->
                <template #icon="{ dataRef }">
                  <img :src="enterpriseIcon" alt="" v-if="dataRef.slots.icon === 'location_parent_icon_info'" class="parent_icon_info" />
                  <!-- 车间 -->
                  <img :src="locationIcon" alt="" v-else-if="dataRef.slots.icon === 'location_icon_info'" class="location_icon_info" />
                  <!-- 设备 -->
                  <img :src="deviceIcon" alt="" v-else-if="dataRef.slots.icon === 'device_icon_info'" class="device_icon_info" />
                  <!-- 相机 -->
                  <img :src="cameraIcon" alt="" v-else-if="dataRef.slots.icon === 'camera_icon_info'" class="camera_icon_info" />
                </template>
                <template #title="{ title }">
                  <span v-if="title.indexOf(searchValue) > -1">
                    <span>{{ title.substr(0, title.indexOf(searchValue)) }}</span>
                    <span style="color: #f50">{{ searchValue }}</span>
                    <span>{{ title.substr(title.indexOf(searchValue) + searchValue.length) }}</span>
                  </span>
                  <span v-else>{{ title }}</span>
                </template>
              </a-tree>
            </a-tab-pane>
          </a-tabs>
          <Divider class="divider" />
        </template>
      </SelectInput>
    </div>

    <div class="search">
      <BaseButton type="sure" :primary-buttton="isSearchBtn ? '检索' : '统计'" @click="handleSearch" />
    </div>
    <div class="reset">
      <BaseButton @click="handleReset" type="back" second-button="重置" />
    </div>
  </div>
</template>
<script lang="ts" setup>
import {defineEmits, onMounted, reactive, ref, watch} from 'vue';
import type {TabsProps} from 'ant-design-vue';
import {Divider, Input, TreeProps} from 'ant-design-vue';
import SelectInput from '/@/components/SelectInput/SelectInput.vue';
import BaseButton from '/@/components/BaseButton/BaseButton.vue';

import cameraIcon from '/@/assets/svg/icon/filter_icon_camera.svg';
import enterpriseIcon from '/@/assets/svg/icon/info_icon_enterprise.svg';
import locationIcon from '/@/assets/svg/icon/info_icon_location.svg';
import deviceIcon from '/@/assets/svg/icon/info_icon_device.svg';
import {cameraTreeData} from './BaseSearch.api';
import {CameraTreeData, filter, handleRawData, selectParentNode} from './BaseSearch.data';

const AInput = Input;

  interface TreeNodeData {
    title: string;
    key: string;
    type: string;
    parentNode: string;
    haveAlarm: boolean;
    parentKey: number;
    children?: TreeNodeData[];
  }

  interface selectValue {
    title: string;
    id: string;
  }
  defineProps({
    backStyle: Boolean,
    isSearchBtn: Boolean,
  });
  const emits = defineEmits(['getParentNode', 'handleSearch', 'handleReset']);
  const mainState = reactive({
    tabs: [
      { label: '按分组分类', index: 1, type: 'device', attr: 'locationTree' },
      { label: '按设备分类', index: 2, type: 'area', attr: 'deviceTree' },
    ],

    cameraValue: '',
    isFlag: false,
    isExpand: false,
    selectValue: [] as selectValue[],
    tempValue: [] as selectValue[],
  });
  let treeValue = {} as CameraTreeData;
  // tabs的方向
  const mode = ref<TabsProps['tabPosition']>('left');
  const activeKey = ref(0);

  // 树
  const expandedkeys = ref<string[]>([]);
  const checkedKeys = ref<string[]>([]);

  // 左边树
  const treeData = ref<TreeNodeData[]>([]);
  const selectValues: selectValue[] = [];
  const selectParent = ref<selectParentNode[]>([]);
  const allTreeData = ref<TreeNodeData[]>([]);
  const autoExpandParent = ref(true);
  const searchValue = ref<string>('');


  let selectTree: string[] = [];

  onMounted(async () => {
    const cameraTreeRes = await cameraTreeData({});
    treeValue = handleRawData(cameraTreeRes);
    treeData.value = cameraTreeRes.locationTree;
    allTreeData.value = cameraTreeRes.locationTree;
    findParentNode(cameraTreeRes.locationTree);

    //传出选择的父节点key
    emits('getParentNode', selectParent.value);
  });
  //找父节点
  const findParentNode = (data) => {
    data.forEach((item) => {
      if (item.children && item.cameraCount > 0) {
        if (item.children.length === item.cameraCount) {
          selectParent.value.push({ parent: item.title, isAlarm: item.haveAlarm, id: item.key, type: item.type, isRootNode: false });
        } else if (item.parentNode === null && item.children.length !== item.cameraCount) {
          selectParent.value.push({ parent: item.title, isAlarm: item.haveAlarm, id: item.key, type: item.type, isRootNode: true });
        }
        findParentNode(item.children);
      } else {
        return;
      }
    });
  };

  //切换树
  const handleChange = (activeKey: number) => {
    if (activeKey === 0) {
      treeData.value = treeValue.locationTree;
    } else {
      treeData.value = treeValue.deviceTree;
    }
  };

  //回车搜索树
  const handleInputSearch = () => {
    //输入框里面的值
    // searchValue.value
    //如果搜索的值在叶子节点
    //如果搜索的值有子节点，且无父节点--根节点
    //如果搜索的值有子节点，有父节点
    treeData.value = filter(treeValue[mainState.tabs[activeKey.value].attr], searchValue.value);
    treeData.value.forEach((item) => {
      expandedkeys.value.push(item.key);
    });
  };

  watch(searchValue, (value) => {
    treeData.value = filter(treeValue[mainState.tabs[activeKey.value].attr], searchValue.value);
    searchValue.value = value;
    treeData.value.forEach((item) => {
      expandedkeys.value.push(item.key);
      if (item.children) {
        item.children.forEach((ele) => {
          expandedkeys.value.push(ele.key);
        });
      }
    });
    if (!value) {
      expandedkeys.value.length = 0;
    }
  });

  // 树的选择，点击复选框触发todo
  const handleCheck = (keys: string[], e: { checked: boolean; checkedNodes: TreeNodeData[]; node; event: string; halfCheckedKeys }) => {
    const { checkedNodes } = e;
    selectValues.length = 0;
    const selectParentList: selectParentNode[] = [];
    checkedNodes?.forEach((item) => {
      if (item.type === 'camera') {
        //分组时加上分类
        selectValues.push({ title: item.title, id: item.key });
      }

      //如果是全选，只要叶子节点
      if (item.children) {
        if (item.parentNode === null && item.cameraCount !== 0) {
          selectParentList.push({ parent: item.title, isAlarm: item.haveAlarm, id: item.key, type: item.type, isRootNode: true });
        }
      }
    });
    selectParent.value = selectParentList;

    checkedKeys.value = keys;
    mainState.tempValue = selectValues;
  };

  const getExpKey = (data: TreeNodeData, list: string[], deep: number) => {
    if (++deep > 10) {
      throw new Error('层级嵌套超过10层');
    }
    if (!data.children) {
      return;
    }

    const rootChilden = data.children.filter((c) => c.children);

    if (!rootChilden.length) {
      return list.push(data.key);
    }
    list.push(data.key);
    for (const child of rootChilden) {
      getExpKey(child, list, deep);
    }
  };
  // 树的展开
  const handleExpand = () => {
    if (expandedkeys.value.length) {
      expandedkeys.value.length && (expandedkeys.value.length = 0);
      autoExpandParent.value = false;
    } else {
      const newArray: string[] = [];
      treeData.value.forEach((d) => getExpKey(d, newArray, 0));
      expandedkeys.value = newArray;
      autoExpandParent.value = false;
    }
  };

  // 展开/收起
  const onExpand: TreeProps['onExpand'] = (keys: string[]) => {
    expandedkeys.value = keys;
  };

  //点击树节点
  const handleSelect: TreeProps['onSelect'] = (keys: string[], { nativeEvent }) => {
    const toArray = (list) => Array.from(list || []);
    //获取被点击的树节点
    const node = nativeEvent?.path?.find((item: any) => {
      return toArray(item?.classList).findIndex((className) => className === 'ant-tree-treenode') !== -1;
    });
    //获取复选框
    const checkBox = toArray(node?.childNodes).find((item: HTMLElement) => {
      return toArray(item?.classList).findIndex((className) => className === 'ant-tree-checkbox') !== -1;
    }) as HTMLElement;
    //模拟点击
    checkBox?.click();
  };

  //点击确认将节点放到输入框，checked或click不给输入框
  const handleSure = () => {
    mainState.selectValue = mainState.tempValue;
  };

  //获取焦点重新调一次接口
  const handleFocus = async () => {
    const cameraTreeRes = await cameraTreeData({});
    treeValue = cameraTreeRes;
    treeData.value = cameraTreeRes.locationTree;
  };
  const handleEnter = async () => {
    const cameraTreeRes = await cameraTreeData({});
    treeValue = cameraTreeRes;
    treeData.value = cameraTreeRes.locationTree;
  };

  // 检索
  const handleSearch = () => {
    //传出选择的父节点key
    emits('getParentNode', selectParent.value);
    let selectIds: string[] = [];
    let selectNames: string[] = [];
    mainState.selectValue.forEach((item) => {
      selectIds.push(item.id);
      selectNames.push(item.title);
    });
    const selectObj = { ids: selectIds, label: selectTree, cameraNames: selectNames };
    emits('handleSearch', selectObj);
  }

  //重置
  const handleReset = async () => {
    const resetObj = { camera: mainState.selectValue };
    checkedKeys.value.length = 0;
    //重置后,重新点击一下区域选择
    activeKey.value = 0;
    searchValue.value = '';
    autoExpandParent.value = false;
    expandedkeys.value.length = 0;
    const cameraTreeRes = await cameraTreeData({});
    //清空之前的
    selectParent.value.length = 0;
    //重新赋值默认分组的父节点
    findParentNode(cameraTreeRes.locationTree);
    emits('getParentNode', selectParent.value);
    emits('handleReset', resetObj);
  };
</script>
<style lang="less" scoped>
  .search {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    .parent_icon_info,
    .location_icon_info,
    .device_icon_info .camera_icon_info {
      width: 16px;
      height: 16px;
    }
    :deep .ant-tabs-content {
      display: flex;

      height: 311px;
      overflow: auto;
    }

    :deep .ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn {
      color: #7089ed;
      &:hover {
        color: #7089ed;
      }
    }
    :deep .ant-tree-checkbox-checked .ant-tree-checkbox-inner {
      background-color: #7089ed;
      border-color: #7089ed;
    }
    :deep .ant-tree-checkbox-indeterminate .ant-tree-checkbox-inner::after {
      background-color: #7089ed;
    }
    .select-box {
      display: flex;
      align-items: center;
    }
    .camera-select {
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      margin: 16px 12px 16px 24px;
      flex: 0 0 52px;
    }
    :deep .ant-tabs-left > .ant-tabs-nav .ant-tabs-tab,
    .ant-tabs-right > .ant-tabs-nav .ant-tabs-tab,
    .ant-tabs-left > div > .ant-tabs-nav .ant-tabs-tab,
    .ant-tabs-right > div > .ant-tabs-nav .ant-tabs-tab {
      height: 24px;
      text-align: center;
    }
    :deep .ant-tabs > .ant-tabs-nav .ant-tabs-nav-list,
    .ant-tabs > div > .ant-tabs-nav .ant-tabs-nav-list {
      margin-top: 16px;
    }
    :deep .ant-tabs-left > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane,
    .ant-tabs-left > div > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane {
      margin-top: 16px;
    }
    :deep .ant-tabs-ink-bar {
      background-color: #7089ed;
    }
    :deep .ant-tabs-tab:hover {
      color: #7089ed;
    }

    .ant-divider-horizontal {
      display: flex;
      clear: both;
      width: 100%;
      min-width: 100%;
      margin-top: 1px;
    }
    :deep .ant-input {
      height: 24px;
      max-width: 220px;
      border-radius: 10px;

      &:focus {
        border-color: #7089ed;
        padding-left: 30px;
        box-shadow: none;
      }
      &:valid {
        padding-left: 30px;
      }
      &:hover {
        border-color: #7089ed;
        box-shadow: none;
      }
    }

    .input-search {
      // display: flex;
      position: relative;
    }
    .search-icon {
      width: 16px;
      height: 16px;
      display: inline-block;
      background: url('/@/assets/svg/icon/info_icon_search.svg');
      position: absolute;
      left: 10px;
      top: 6px;
      z-index: 10;
      cursor: pointer;
    }

    .expand {
      cursor: pointer;
      .callapse-icon {
        background: url('/@/assets/svg/icon/icon_collapse_default.svg');
        display: inline-block;
        width: 13px;
        height: 13px;
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
        margin-right: 8px;
        margin-top: 4px;
        &:hover {
          background: url('/@/assets/svg/icon/icon_expand_hover.svg');
        }
      }
      .control {
        color: var(--cancel-text-color);
      }
    }
    :deep .ant-tree-treenode.ant-tree-treenode-switcher-close:hover {
      opacity: 60%;
      width: 328px;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      background-color: var(--fill-four-color);
    }
    :deep .ant-tree-treenode.ant-tree-treenode-switcher-open:hover {
      opacity: 60%;
      width: 328px;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
      background-color: var(--fill-four-color);
    }
    :deep .ant-tree-checkbox-indeterminate .ant-tree-checkbox-inner {
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

    :deep .ant-tree-switcher .ant-tree-switcher-icon,
    .ant-tree-switcher .ant-select-tree-switcher-icon {
      // display: none;
      font-size: 10px;
      vertical-align: baseline;
    }
    :deep .ant-select-tree-list-holder {
      padding: 12px;
    }
    :deep .ant-tree-treenode.ant-tree-treenode-switcher-open.ant-tree-treenode-selected {
      opacity: 40%;
      width: 328px;
      // background-color: #738ce6;
      border-radius: 4px;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
    }
    :deep .ant-tree .ant-tree-node-content-wrapper.ant-tree-node-selected {
      background-color: transparent;
      font-family: Source Han Sans CN;
      color: var(--secondary-text-color);
      font-size: 13px;
    }

    :deep .ant-divider.ant-divider-horizontal.divider {
      width: 480px;
      height: 1px;
      background-color: rgba(211, 216, 230, 0.6);
    }

    .camera-icon {
      width: 16px;
      height: 16px;
      margin: 8px 8px;
    }

    :deep .ant-select:not(.ant-select-customize-input) .ant-select-selector {
      position: relative;
      background-color: var(--background-Color);
      border: 1px solid var(--border-color);
      border-radius: 2px;
      transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
      // width: 320px;
      min-width: 220px;
      height: 28px;
      border: 1px solid;
      border-color: var(--weak-split-line-color);
      border-radius: 4px;
      flex-shrink: 1;
    }
    .select {
      position: relative;
      :deep .ant-select-selector {
        &:hover {
          border-color: #adb4cc;

          box-shadow: none;
        }
      }

      :deep .ant-tree-select-dropdown {
        padding: 8px 4px;
        height: 305px;
        overflow: auto;
      }
      :deepspan.ant-select-tree-switcher.ant-select-tree-switcher_open {
        display: none;
      }
      :deep .ant-select-tree-checkbox-checked .ant-select-tree-checkbox-inner {
        background-color: #738ce6;
        border-color: #738ce6;
      }
      :deep .ant-select-tree-checkbox-indeterminate .ant-select-tree-checkbox-inner {
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
      :deep .ant-select-tree-node-content-wrapper .ant-select-tree-node-content-wrapper-open {
        background: #ffff;
      }
      :deep .ant-select-tree-checkbox-input:focus + .ant-select-tree-checkbox-inner {
        border-color: #7089ed;
      }
      :deep .ant-select-tree-treenode.ant-select-tree-treenode-switcher-open.ant-select-tree-treenode-checkbox-checked {
        // background-color: #738ce6;
        border-radius: 4px;
        color: var(--secondary-text-color);
        opacity: 40%;
      }
      :deep .ant-select-tree-treenode .ant-select-tree-treenode-switcher-open:hover {
        opacity: 60%;
        font-family: Source Han Sans CN;
        color: var(--secondary-text-color);
        font-size: 13px;
        background-color: var(--fill-four-color);
      }
      :deep .ant-select-tree-treenode.ant-select-tree-treenode-switcher-open:hover {
        opacity: 60%;
        font-family: Source Han Sans CN;
        color: var(--secondary-text-color);
        font-size: 13px;
        background-color: var(--fill-four-color);
      }
      :deep .ant-select-tree-checkbox-wrapper:hover .ant-select-tree-checkbox-inner,
      :deep .ant-select-tree-checkbox:hover .ant-select-tree-checkbox-inner,
      :deep .ant-select-tree-checkbox-input:focus + .ant-select-tree-checkbox-inner {
        border-color: #738ce6;
      }
    }
    .down-icon {
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
    .search {
      margin-right: 12px;
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
  :deep .ant-tree-checkbox-wrapper:hover .ant-tree-checkbox-inner,
  :deep .ant-tree-checkbox:hover .ant-tree-checkbox-inner,
  :deep .ant-tree-checkbox-input:focus + .ant-tree-checkbox-inner {
    border-color: #738ce6;
  }
</style>
