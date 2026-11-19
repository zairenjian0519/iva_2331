<template>
  <div class="search">
    <SelectInput :value="selectValue" @handle-enter="handleEnter" @handle-focus="handleFocus" @handle-sure="handleSure(groupId)" :placeholder="isPlaceholder ? `(非必填)` : ''">
      <template #icon><img :src="cameraIcon" class="camera-icon" /></template>
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
              checkable
              :auto-expand-parent="autoExpandParent"
              v-model:checkedKeys="checkedKeysValue"
              v-model:selectedKeys="selectedKeysValue"
              showIcon
              key="key"
              :expandedKeys="expandedkeys"
              @expand="onExpand"
              @select="handleSelect"
            >
              <!-- <template #title="{ title, key, slot }">
                  <span>{{ title }}</span>
                </template> -->
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
        <Divider class="divider-line" />
      </template>
    </SelectInput>
  </div>
</template>
<script lang="ts" setup>
  import { ref, reactive, onMounted, watch } from 'vue';
  import { Divider, Input, TreeProps } from 'ant-design-vue';
  import SelectInput from '/@/components/SelectInput/SelectInput.vue';

  import cameraIcon from '/@/assets/svg/icon/filter_icon_camera.svg';
  import enterpriseIcon from '/@/assets/svg/icon/info_icon_enterprise.svg';
  import locationIcon from '/@/assets/svg/icon/info_icon_location.svg';
  import deviceIcon from '/@/assets/svg/icon/info_icon_device.svg';

  import type { TabsProps } from 'ant-design-vue';
  import { cameraTreeData } from '/@/components/BaseSearch/BaseSearch.api';

  import { CameraTreeData, selectParentNode, handleRawData, filter } from '/@/components/BaseSearch/BaseSearch.data';

  const AInput = Input;

  interface TreeNodeData {
    title: string;
    type: string;
    parentNode: string;
    haveAlarm: boolean;
    parentKey: string;
    children?: TreeNodeData[];
  }

  interface selectValue {
    title: string;
    id: string;
  }
  const props = defineProps({
    backStyle: Boolean,
    isSearchBtn: Boolean,
    isPlaceholder: Boolean,
    groupId: String,
    selectValue: Array,
    checkedKeys: Array,
    selectedKeys: Array,
  });

  const emits = defineEmits(['getSelectCamera', 'getSelectedGroup', 'handleSelect', 'val', 'selectVal', 'handleCheck', 'handleExpand']);
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
    checkedCamera: [] as TreeNodeData[],
    res: [] as TreeNodeData[],
    activeKey: 0 as number,
    selectedKeys: [] as string[],
  });
  let treeValue = {} as CameraTreeData;
  // tabs的方向
  const mode = ref<TabsProps['tabPosition']>('left');
  const activeKey = ref(0);

  // 树
  const expandedkeys = ref<string[]>([]);

  // 左边树
  const treeData = ref<TreeNodeData[]>([]);
  const selectValues: selectValue[] = [];
  const selectParent = ref<selectParentNode[]>([]);

  const autoExpandParent = ref(true);
  const searchValue = ref<string>('');
  const checkedKeysValue = ref(props.checkedKeys);
  const selectedKeysValue = ref(props.selectedKeys);
  onMounted(async () => {
    const cameraTreeRes = await cameraTreeData({});

    treeValue = handleRawData(cameraTreeRes);

    treeData.value = cameraTreeRes.locationTree;
  });

  //切换树
  const handleChange = (activeKey: number) => {
    mainState.activeKey = activeKey;
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

  watch(
    () => props.checkedKeys,
    (val) => {
      checkedKeysValue.value = val;
    },
    {
      immediate: true,
    }
  );
  watch(
    () => props.selectedKeys,
    (val) => {
      selectedKeysValue.value = val;
    },
    {
      immediate: true,
    }
  );

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

  function copy(data: TreeNodeData): TreeNodeData {
    const clone = { ...data };
    delete clone.children;
    return clone;
  }
  // 树的选择，点击复选框触发todo
  const handleCheck = (keys: string[], e: { checked: boolean; checkedNodes: TreeNodeData[]; node; event: string; halfCheckedKeys }) => {
    emits('handleCheck', keys);
    const { checkedNodes } = e;
    mainState.res.length = 0;
    let clone: TreeNodeData;
    for (const data of treeData.value) {
      clone = copy(data);

      mainState.res.push(clone);
    }
    mainState.res.forEach((item) => {
      if (!item.parentKey) {
        let flag = false;
        checkedNodes.forEach((i) => {
          if (i.parentKey === item.key) {
            flag = true;
          }
        });
        if (flag) {
          item.children = checkedNodes.filter((i) => i.parentKey);
        }
      }
    });

    mainState.checkedCamera = checkedNodes;

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
    emits('handleExpand', keys);
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
    emits('handleSelect', keys);
  };

  //点击确认将节点放到输入框，checked或click不给输入框
  const handleSure = (groupId) => {
    emits('getSelectCamera', mainState.tempValue, groupId);
    emits('getSelectedGroup', mainState.res, mainState.activeKey, groupId, props.selectValue);
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
</script>
<style lang="less" scoped>
  .search {
    display: flex;
    align-items: center;
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

    :deep .ant-divider .ant-divider-horizontal .divider-line {
      width: 480px;
      height: 1px;
      background-color: rgba(211, 216, 230, 0.6);
    }

    .camera-icon {
      width: 16px;
      height: 16px;
      margin: 8px 8px;
    }
  }
</style>
