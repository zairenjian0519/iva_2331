<template>
  <div style="text-align: -webkit-center">
    <a-tree v-if="treeData.length > 0" :defaultExpandAll="autoExpandParent" :treeData="treeData" @select="handleSelect" show-icon v-model:selectedKeys="selectedValue">
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
      <template #title="{ title, runStatus, type, cameraCount }">
        <span v-if="title && searchValue && title.indexOf(searchValue) > -1" :class="type === 'camera' ? (runStatus !== '1' ? 'camera-offline' : '') : ''">
          <span>{{ title.substr(0, title.indexOf(searchValue)) }}</span>
          <span style="color: #f50">{{ searchValue }}</span>
          <span>{{ title.substr(title.indexOf(searchValue) + searchValue.length) }}</span>
        </span>
        <span v-else :class="type === 'camera' ? (runStatus !== '1' ? 'camera-offline' : '') : ''">{{ title }} </span>
        <label v-if="type !== 'camera'">({{ cameraCount ? cameraCount : 0 }})</label>
      </template>
    </a-tree>
  </div>
</template>

<script lang="ts" setup>
  import { ref, defineEmits, watch } from 'vue';

  import cameraIcon from '/@/assets/svg/icon/filter_icon_camera.svg';
  import cameraFaultIcon from '/@/assets/svg/icon/info_icon_camera_fault.svg';
  import enterpriseIcon from '/@/assets/svg/icon/info_icon_enterprise.svg';
  import locationIcon from '/@/assets/svg/icon/info_icon_location.svg';
  import deviceIcon from '/@/assets/svg/icon/info_icon_device.svg';
  import { TreeNodeData } from '../CameraGroup.data';

  const props = defineProps<{
    treeData: TreeNodeData[];
    autoExpandParent: boolean;
    selectedKey: string[];
  }>();

  const emit = defineEmits(['select', 'key']);

  const searchValue = ref<string>('');

  const selectedValue = ref<string[]>();

  watch(
    () => props.selectedKey,
    () => {
      selectedValue.value = props.selectedKey;
    },
    { immediate: true, deep: true }
  );

  function handleSelect(keys, item) {
    // if (item.node?.type === 'camera') {
    emit('select', item);
    // }
  }
</script>

<style scoped>
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
