<template>
  <div style="text-align: -webkit-center">
    <div class="div-search">
      <img style="margin-left: 10px" :src="searchIcon" alt="" />
      <a-input v-model:value="searchValue" :bordered="false" placeholder="请输入相机名称进行查询" />
    </div>
    <a-tree :expanded-keys="expandedKeys" :auto-expand-parent="autoExpandParent" :treeData="treeData" @select="handleSelect" @expand="onExpand" show-icon>
      <template #icon="{ dataRef }">
        <img :src="enterpriseIcon" alt="" v-if="dataRef.slots.icon === 'location_parent_icon_info'" class="parent_icon_info" />
        <!-- 车间 -->
        <img :src="locationIcon" alt="" v-else-if="dataRef.slots.icon === 'location_icon_info'" class="location_icon_info" />
        <!-- 设备 -->
        <img :src="deviceIcon" alt="" v-else-if="dataRef.slots.icon === 'device_icon_info'" class="device_icon_info" />
        <!-- 相机 -->
        <img :src="cameraIcon" alt="" v-else-if="dataRef.slots.icon === 'camera_icon_info' && dataRef.runStatus === '1'" class="camera_icon_info" />
        <img :src="cameraFaultIcon" alt="" v-else-if="dataRef.slots.icon === 'camera_icon_info' && dataRef.runStatus !== '1'" class="camera_icon_info" />
      </template>
      <template #title="{ title, runStatus, type, cameraCount, offLineCount }">
        <span v-if="title && searchValue && title.indexOf(searchValue) > -1" :class="type === 'camera' ? (runStatus !== '1' ? 'camera-offline' : '') : ''">
          <span>{{ title.substr(0, title.indexOf(searchValue)) }}</span>
          <span style="color: #f50">{{ searchValue }}</span>
          <span>{{ title.substr(title.indexOf(searchValue) + searchValue.length) }}</span>
        </span>
        <span v-else :class="type === 'camera' ? (runStatus !== '1' ? 'camera-offline' : '') : ''">{{ title }} </span>
        <label v-if="type !== 'camera'">({{ cameraCount ? cameraCount : 0 }})</label>
        <a-tag v-if="type !== 'camera' && offLineCount && offLineCount > 0" class="tag-offline" color="error">
          <label style="margin-top: -2px; margin-left: -23px; position: absolute">离线 {{ offLineCount ? offLineCount : 0 }}</label>
        </a-tag>
      </template>
    </a-tree>
    <a-button style="margin-bottom: 6px" @click="handleCameraAdd" v-auth="'alg:camera'">
      <img alt="" :src="addCameraIcon" style="float: left" />
      <label>新建相机</label>
    </a-button>
    <a-button style="float: right" type="link" @click="goCameraList">相机管理</a-button>
  </div>
  <camera-modal ref="cameraModelRef" />
</template>

<script lang="ts" setup>
  import { TreeItem } from '/@/components/Tree';
  import { ref, defineEmits, onMounted, defineExpose, watch } from 'vue';
  import { cameraTree } from '../AlgorithmManager.api';
  import { useGo } from '/@/hooks/web/usePage';
  import CameraModal from '../../cameraManager/components/CameraModal.vue';
  import cameraIcon from '/@/assets/svg/icon/filter_icon_camera.svg';
  import cameraFaultIcon from '/@/assets/svg/icon/info_icon_camera_fault.svg';
  import enterpriseIcon from '/@/assets/svg/icon/info_icon_enterprise.svg';
  import searchIcon from '/@/assets/svg/icon/info_icon_search.svg';
  import addCameraIcon from '/@/assets/svg/icon/editor_btn_adds_default.svg';
  import locationIcon from '/@/assets/svg/icon/info_icon_location.svg';
  import deviceIcon from '/@/assets/svg/icon/info_icon_device.svg';
  import { filter } from '/@/components/BaseSearch/BaseSearch.data';

  const treeData = ref<TreeItem[]>([]);
  const emit = defineEmits(['select']);
  const cameraData = ref();
  const cameraModelRef = ref();
  const go = useGo();
  const searchValue = ref<string>('');
  const autoExpandParent = ref<boolean>(false);
  const expandedKeys = ref<(string | number)[]>([]);
  const isLocationSelect = ref<boolean>(false);
  const isShow = ref<boolean>(false);
  const onExpand = (keys: string[]) => {
    expandedKeys.value = keys;
    autoExpandParent.value = false;
  };

  watch(searchValue, (value) => {
    if (value === '') {
      treeData.value = isLocationSelect.value ? cameraData.value.locationTree : cameraData.value.deviceTree;
      return;
    }
    treeData.value = filter(isLocationSelect.value ? cameraData.value.locationTree : cameraData.value.deviceTree, searchValue.value);
    searchValue.value = value;
    treeData.value.forEach((item) => {
      expandedKeys.value.push(item.key);
      if (item.children) {
        item.children.forEach((ele) => {
          expandedKeys.value.push(ele.key);
        });
      }
    });
  });

  async function reload() {
    let v = await cameraTree();
    cameraData.value = v;
    treeData.value = v.deviceTree;
  }

  function loClick() {
    treeData.value = cameraData.value.locationTree;
    isLocationSelect.value = true;
  }

  function devClick() {
    treeData.value = cameraData.value.deviceTree;
    isLocationSelect.value = false;
  }
  //相机配置弹窗
  const handleOk = function handleSelect(keys, item) {
    isShow.value = false;
    if (item.node?.type === 'camera') {
      emit('select', item.node?.title, item.node.key);
    }
  };

  function handleSelect(keys, item) {
    if (item.node?.type === 'camera') {
      emit('select', item.node?.title, item.node.key);
    }
  }

  const hanleCancel = () => {
    isShow.value = false;
  };

  function handleCameraAdd() {
    cameraModelRef.value.initCameraModel({ isUpdate: false });
  }

  function handleSuccess() {
    reload();
  }

  function goCameraList() {
    go('/base/camera');
  }

  onMounted(() => {
    reload();
  });

  defineExpose({
    loClick,
    devClick,
  });
</script>

<style lang="less" scoped>
  button {
    float: left;
    margin-top: 10px;
  }

  .div-search {
    border-radius: 30px;
    display: flex;
    align-items: center;
    border: 1px solid;
    margin-bottom: 8px;
    margin-top: 4px;
    width: 100%;
    height: 24px;
    border: 1px solid;
    border-color: var(--weak-split-line-color);
    border-radius: 12px;
    .ant-input-borderless {
      border: none;
    }
  }

  .parent_icon_info,
  .location_icon_info,
  .device_icon_info,
  .camera_icon_info {
    width: 16px;
    height: 16px;
    margin-top: 4px;
  }

  .camera-offline {
    color: #c5c6cc;
    font-size: 13px;
  }

  .tag-offline {
    width: 56px;
    height: 18px;
    border-radius: 2px;
    font-size: 9px;
    position: absolute;
    margin-top: 3px;
    margin-left: 10px;
    background-color: #f4e0e0;
  }
</style>
