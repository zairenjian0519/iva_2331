<template>
  <div class="cameraMain">
    <div class="tab-control">
      <div v-for="(item, index) of ['超脑', '分组']" :key="index" :class="{ activeItem: treeMain.currentIndex === index }" @click="clickItem(index)" class="tab-control-item">
        <span>{{ item }}</span>
      </div>
    </div>
    <div v-if="!treeMain.isDevice">
      <div style="margin-top: 15px; display: flex; align-items: center">
        <img :src="searchIcon" alt="" class="search-icon" />
        <a-input class="treeSeach" @keyup.enter="handleInputSearch" v-model:value="treeMain.locationValue" placeholder="分组" @change="locationChange" />
      </div>
    </div>
    <div :class="treeMain.isAllCamera ? 'allCameraStyleActive' : 'allCameraStyle'" @click="searchCamera()">
      <span>
        <img :src="cameraIcon" alt="" class="camera-icon" />
      </span>
      <span style="font-weight: bold; font-size: 13px">全部相机 ({{ treeMain.allCameraCount }})</span>
    </div>
    <template v-if="treeMain.allCameraCount === 0 && treeMain.isDevice">
      <div class="no-num">
        <i class="tips"></i>
        <span class="no-device">暂无设备</span>
        <a class="add-dev" v-auth="'dev:add'" @click="handleAddDev">新建超脑</a>
      </div>
    </template>
    <template v-else-if="treeMain.allCameraCount === 0 && !treeMain.isDevice">
      <div class="no-num">
        <i class="tips"></i>
        <span class="no-location">暂无分组</span>
        <a class="add-location" @click="handleAddArea">新建分组</a>
      </div>
    </template>
    <template v-else>
      <a-tree :tree-data="treeData" key="key" showIcon v-model:selected-keys="treeMain.selectKeys" @select="handleSelectTree">
        <!-- 厂区 -->
        <template #icon="{ dataRef }">
          <img :src="enterpriseIcon" alt="" v-if="dataRef?.slots?.icon === 'location_parent_icon_info'" class="parent_icon_info" />
          <!-- 车间 -->
          <img :src="locationIcon" alt="" v-else-if="dataRef?.slots?.icon === 'location_icon_info'" class="location_icon_info" />
          <!-- 设备 -->
          <img :src="deviceIcon" alt="" v-else-if="dataRef?.slots?.icon === 'device_icon_info'" class="device_icon_info" />
        </template>
        <template #title="item">
          <span style="font-size: 13px"> {{ item.title }} ({{ item.cameraNum === 0 ? '未授权' : item.cameraCount }})</span>
          <span v-if="treeMain.isDevice && item.offLineCount > 0">
            <a-tag class="offlineStyle">
              <label style="margin-top: -4px; margin-left: -7px; position: absolute">离线 {{ item.offLineCount }}</label>
            </a-tag>
          </span>
        </template>
      </a-tree>
    </template>

    <div v-if="treeMain.isDevice">
      <p class="divider"></p>
    </div>
    <div v-if="treeMain.isDevice" style="display: flex; align-items: center; margin-left: 25px">
      <img :src="chaoIconFrist" alt="" />
      <span style="font-size: 13px; margin-left: 8px" :onclick="handleDevice"> 超脑管理 </span>
      <span><img :src="chaoIcon" alt="" /> </span>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { TreeItem } from '/@/components/Tree';
  import { useModal } from '/@/components/Modal';

  import { ref, onMounted, defineExpose, reactive, defineEmits, onUnmounted } from 'vue';
  import { cameraTree } from '../CameraManager.api';
  import chaoIconFrist from '/@/assets/svg/icon/info_icon_help.svg';
  import chaoIcon from '/@/assets/svg/icon/date_btn_forward_default.svg';
  import cameraIcon from '/@/assets/svg/icon/tree_icon_camera.svg';
  import enterpriseIcon from '/@/assets/svg/icon/info_icon_enterprise.svg';
  import locationIcon from '/@/assets/svg/icon/info_icon_location.svg';
  import deviceIcon from '/@/assets/svg/icon/info_icon_device.svg';
  import searchIcon from '/@/assets/svg/icon/info_icon_search.svg';
  import { filter } from '/@/components/BaseSearch/BaseSearch.data';
  import { useGo } from '/@/hooks/web/usePage';
  let upInterval: NodeJS.Timer | null = null;

  const go = useGo();
  //注册model

  const emit = defineEmits(['select', 'reload', 'handleClickTab']);
  const treeData = ref<TreeItem[]>([]);
  const cameraData = ref();

  const treeMain = reactive({
    isDevice: true,
    locationValue: '',
    allCameraCount: 0,
    isAllCamera: true,
    selectKeys: [],
    currentIndex: 0 as number,
  });

  async function reload() {
    cameraData.value = await cameraTree();
    treeMain.allCameraCount = cameraData.value.allCameraCount;
    if (treeMain.currentIndex === 0) {
      deviceTree();
    } else {
      locationTree();
      locationChange(); //间隔刷新会刷新查询列表,需要重新按已输入的查询条件查
    }
  }

  function locationTree() {
    treeMain.isDevice = false;
    treeData.value = cameraData.value.locationTree;
  }

  function locationChange() {
    // treeData.value = filter(cameraData.value.locationTree, treeMain.locationValue);
    treeData.value = cameraData.value.locationTree.filter((item) => item.title.includes(treeMain.locationValue));
  }

  function deviceTree() {
    treeMain.locationValue = '';
    treeMain.isDevice = true;
    treeData.value = cameraData.value.deviceTree;
  }

  onMounted(() => {
    reload();
  });
  function handleAddDev() {
    go('/base/device');
  }
  function handleAddArea() {
    go('/base/areacate');
  }

  //切换超脑和区域时左侧树数据加载
  const clickItem = (index: number) => {
    if (treeMain.currentIndex !== index) {
      treeMain.currentIndex = index;
      if (index === 1) {
        locationTree();
      } else {
        deviceTree();
      }
      searchCamera();
    }
    emit('handleClickTab', index);
  };

  //点击全部相机查询
  const searchCamera = () => {
    treeMain.isAllCamera = true;
    treeMain.selectKeys = [];
    emit('select', { title: '全部相机' });
  };

  const handleSelectTree = (keys, item) => {
    treeMain.selectKeys = keys;
    treeMain.isAllCamera = false;
    if (treeMain.isDevice) {
      emit('select', { cameraOwnership: item.node.key, location: null, title: item.node.title });
    } else {
      emit('select', { location: item.node.key, cameraOwnership: null, title: item.node.title });
    }
  };

  const updateTree = async () => {
    cameraData.value = await cameraTree();
    console.log(cameraData.value, '2222');
    treeMain.allCameraCount = cameraData.value.allCameraCount;
    if (treeMain.isDevice) {
      deviceTree();
    } else {
      locationTree();
    }
  };

  defineExpose({
    locationTree,
    deviceTree,
    updateTree,
    reload,
  });

  //回车搜索树
  const handleInputSearch = () => {
    treeData.value = cameraData.value.locationTree.filter((item) => item.title.includes(treeMain.locationValue));
    // filter(cameraData.value.locationTree, treeMain.locationValue);
  };

  const handleDevice = () => {
    go('/base/device');
  };
</script>

<style lang="less" scoped>
  .cameraMain {
    /deep/ .device_icon_info {
      margin-top: 4px;
    }
    .divider {
      border-bottom: 1px solid var(--input-text-color);
      opacity: 60%;
      margin: 4px 16px 16px 16px;
      width: 240px;
    }
    .allCameraStyle {
      display: flex;
      align-items: center;
      margin-left: 5px;
      margin-top: 10px;
      width: 240px;
    }
    /deep/ .ant-tree {
      width: 240px;
    }
    .allCameraStyleActive {
      display: flex;
      align-items: center;
      margin-left: 5px;
      margin-top: 10px;
      background-color: rgba(112, 137, 224, 0.15);
      width: 240px;
    }
    .no-num {
      display: flex;
      align-items: center;
    }
    .tips {
      display: inline-block;
      width: 16px;
      height: 16px;
      background: url('/@/assets/svg/icon/info_icon_tips.svg');
      margin-left: 24px;
      text-align: center;
      margin-right: 4px;
    }
    .no-device,
    .no-location {
      font-family: Source Han Sans CN;
      color: var(--title-tips-text-color);
      font-size: 13px;
      cursor: pointer;
    }
    .add-dev,
    .add-location {
      margin-left: 8px;
      font-family: Source Han Sans CN;
      color: var(--primary-Color);
      font-size: 13px;
    }
    .addButton {
      display: flex;
      align-items: center;
      border-radius: 4px;
      width: 96%;
      height: 27px;
    }
    .treeSeach {
      margin-left: 10px;
      width: 92%;
      border-radius: 6px;
      height: 27px;
      font-size: 13px;
      text-indent: 12px;
    }
    .search-icon {
      position: absolute;
      margin-left: 12px;
      z-index: 2;
    }
    .tree-node {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 13px;
      padding-right: 8px;
    }
    .offlineStyle {
      background-color: #f4e0e0;
      border-radius: 2px;
      width: 50px;
      height: 14px;
      position: absolute;
      margin-top: 6px;
      margin-left: 10px;
      font-size: 9px;
      font-family: Helvetica;
      color: var(--serious-text-color);
    }

    .tab-control {
      display: flex;
      text-align: center;
      height: 27px;
      line-height: 24px;
      background-color: var(--background-Color);
      border: 2px solid;
      border-color: var(--netural-color);
      border-radius: 4px;
      width: 240px;
      margin-left: 10px;
      &-item {
        width: 240px;
        cursor: pointer;
        background: var(--netural-color);
        span {
          font-family: Source Han Sans CN;
          color: var(--cancel-text-color);
          font-size: 13px;
        }
      }
      .activeItem {
        background-color: var(--background-Color);
        width: 240px;
        span {
          font-family: Source Han Sans CN;
          //color: #8a8d99;
          color: var(--secondary-text-color);
          font-size: 13px;
        }
      }
    }
  }
</style>
