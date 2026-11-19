<template>
  <div class="device-management">
    <div class="device-container">
      <a-card :bordered="false" style="height: 100%">
        <div v-if="currentDevice" class="device-profile">
          <!-- Device Header: Name, Status, and Operations -->
          <div class="device-profile-header">
            <div class="device-identity">
              <span class="device-name-text">{{ currentDevice.deviceName }}</span>
            </div>
            <div class="device-actions">
              <a-button type="primary" @click="saveConfig(currentDevice)"
                        v-show="currentDevice.status === '1'" v-auth="'dev:conf'">配置下发
              </a-button>
              <a-button type="primary" @click="devReboot(currentDevice)"
                        v-show="currentDevice.status === '1'" v-auth="'dev:reboot'">重启算法
              </a-button>
              <a-button type="primary" @click="upDeviceVer(currentDevice)"
                        v-show="currentDevice.status === '1'" v-auth="'dev:edit'">
                软件升级
              </a-button>
              <a-button type="primary" @click="handleEdit(currentDevice)" v-auth="'dev:edit'">设置
              </a-button>
            </div>
          </div>

          <a-divider/>

          <!-- Device Body: Image, Details, and RunReport -->
          <a-row :gutter="[32, 24]">
            <a-col :xs="24" :sm="24" :md="currentDevice.showRun ? 12: 24"
                   :lg="currentDevice.showRun ? 12 : 24" class="device-content-col">
              <a-row :gutter="[24, 24]" style="align-items: flex-start;">
                <a-col :xs="24" :sm="24" :md="6" :lg="5" class="device-image-wrapper">
                  <div
                    class="css-drawn-custom-aibox-container"
                    :class="{ 'status-online': currentDevice.status === '1', 'status-offline': currentDevice.status !== '1' }"
                  >
                    <div class="aibox-main-body-custom">
                      <div class="aibox-side-panel-custom">
                        <div class="aibox-screw-custom screw-tr"></div>
                        <div class="aibox-screw-custom screw-br"></div>
                        <div class="aibox-port-custom debug-port"></div>
                        <div class="aibox-port-custom ethernet-port gbe0">
                          <div class="aibox-led-custom led-left"></div>
                          <div class="aibox-led-custom led-right"></div>
                        </div>
                        <div class="aibox-port-custom ethernet-port gbe1">
                          <div class="aibox-led-custom led-left"></div>
                          <div class="aibox-led-custom led-right animation-delay"></div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <span
                    :class="['device-status-tag', currentDevice.status === '1' ? 'online' : 'offline']"
                    style="margin-top: 12px;">
                    <img :src="currentDevice.status === '1' ? onLine : offLine" alt="status"
                         class="status-icon"/>
                    设备{{ currentDevice.status_dictText }}
                  </span>
                </a-col>
                <a-col :xs="24" :sm="24" :md="18" :lg="19" class="device-details-wrapper">
                  <div class="detail-grid">
                    <div class="detail-item">
                      <span class="detail-label">产品型号</span>
                      <span class="detail-value">{{ currentDevice.category_dictText }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">设备SN码</span>
                      <span class="detail-value" v-if="isShowId">{{ devSn }}</span>
                      <span class="detail-value" v-else>N/A</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">算法包版本</span>
                      <span class="detail-value">{{ currentDevice.AIVer }}</span>
                    </div>
                    <div class="detail-item detail-item-full-row detail-storage">
                      <span class="detail-label section-title">存储空间</span>
                      <div class="storage-content">
                        <a-progress
                          :percent="currentDevice.disk_percent ? currentDevice.disk_percent : 0"
                          stroke-color="#7bbdc7"
                          :status="currentDevice.disk_percent ? (currentDevice.disk_percent > 85 ? 'exception' : 'active') : 'active'"
                          :show-info="false"
                          class="storage-progress"
                        />
                        <div class="storage-text">
                          <span>占量 ({{
                              currentDevice.disk_used ? (currentDevice.disk_used / 1024).toFixed(0) : '-'
                            }}G, {{
                              currentDevice.disk_percent ? currentDevice.disk_percent : 0
                            }}%)</span>
                          <span class="total-space">总量 {{
                              currentDevice.disk_total ? (currentDevice.disk_total / 1024).toFixed(0) : '--'
                            }}G</span>
                        </div>
                      </div>
                    </div>
                    <div class="detail-item detail-item-full-row detail-license">
                      <span class="detail-label section-title">授权信息</span>
                      <div
                        v-if="currentDevice.licenseStatus !== null && currentDevice.licenseStatus !== '0' && currentDevice.licenseStatus !== ''"
                        class="license-info-valid">
                        <div class="license-item">
                          <img alt="camera icon" :src="treeCameraIcon" class="license-icon"/>
                          <span class="license-type-label">路数授权:</span> <span
                          class="license-type-value">{{
                            currentDevice.useCameraNum
                          }}/{{ currentDevice.cameraNum }}</span>
                        </div>
                        <div class="license-item">
                          <img alt="algorithm icon" :src="treeAlgIcon" class="license-icon"/>
                          <span class="license-type-label">算法个数授权:</span> <span
                          class="license-type-value">{{
                            currentDevice.useAlgorithmNum
                          }}/{{ currentDevice.algorithmNum }}</span>
                        </div>
                        <div class="license-item">
                          <img alt="algorithm camera icon" :src="treeAlgIcon" class="license-icon"/>
                          <span class="license-type-label">可配算法相机授权:</span> <span
                          class="license-type-value">{{
                            currentDevice.useAlgorithmCameraNum
                          }}/{{ currentDevice.algorithmCameraNum }}</span>
                        </div>
                      </div>
                      <div v-else class="license-info-invalid">
                        <a-icon type="info-circle" style="margin-right: 4px;"/>
                        未授权请联系管理员
                      </div>
                    </div>
                  </div>
                </a-col>
              </a-row>
            </a-col>

            <a-col :xs="24" :sm="24" :md="12" :lg="12" v-if="currentDevice.showRun"
                   class="run-report-col">
              <RunReport ref="runReportHtml" :dev-run-info="currentDevice.devRun"/>
            </a-col>
          </a-row>
        </div>
        <div v-else class="no-device-placeholder">
          <a-empty description="暂无设备信息"/>
        </div>

        <!-- Modals remain unchanged -->
        <DeviceManagementModal @register="registerModal" @success="handleSuccess"/>
        <UpDevice @register="upDeviceModal" @success="handleSuccess"/>
        <UpLicense @register="upLicenseModal" @success="handleSuccess"
                   @handle-license-ok="handleLicenseOk"/>
      </a-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref, watchEffect} from 'vue';
import {Col, Divider, Empty, Progress, Row} from 'ant-design-vue';
import {InfoCircleOutlined} from '@ant-design/icons-vue';
import {getDeviceSn, getDevInfo, list, rebootDev, saveByDev} from './DeviceManagement.api';
import RunReport from './components/RunReport.vue';
import {useModal} from '/@/components/Modal';
import DeviceManagementModal from './components/DeviceManagementModal.vue';
import UpDevice from './components/UpDevice.vue';
import {useGlobSetting} from '/@/hooks/setting';
import {useWebSocket} from '@vueuse/core';
import {useUserStore} from '/@/store/modules/user';
import {useMessage} from '/@/hooks/web/useMessage';
import treeCameraIcon from '/@/assets/svg/icon/tree_icon_camera.svg';
import treeAlgIcon from '/@/assets/svg/icon/tree_icon_algorithm.svg';
import offLine from '/@/assets/svg/icon/icon_offline.svg';
import onLine from '/@/assets/svg/icon/icon_online.svg';
import UpLicense from '/@/views/base/deviceManagement/components/UpLicense.vue';
import {saveGroupAll} from '/@/views/base/cameraGroup/CameraGroup.api';
import {getAllVersion} from "/@/views/base/deviceManagement/UpdateSoft.api";


const ADivider = Divider;
const AEmpty = Empty;
const AIcon = InfoCircleOutlined;

//register model
const [registerModal, {openModal}] = useModal();
const [upDeviceModal, {openModal: openUpDevice}] = useModal();

const ACol = Col;
const ARow = Row;
const AProgress = Progress;
const glob = useGlobSetting();
const userId = useUserStore().getUserInfo?.id;
const hostUrl = window.location.host;
const index = window.location.host.indexOf(':');
const hostIp = window.location.host.slice(0, index);
let url = glob.domainUrl.replace('https://', 'wss://').replace('http://', 'ws://') + '/devWebsocket/' + userId;
if (hostUrl != 'localhost:3100') {
  url = url.replace(/\/\/([^\s/:]+)(:\d+)?/, '//' + hostIp + ':8088');
}
const {data} = useWebSocket(url);
const {createMessage, createConfirm} = useMessage();

const devIp = ref<string>('');

const isShowUpdateAlg = ref<boolean>(false);

const selectFileName = ref<string>('');
const versionSelected = ref<string>('');

const runReportHtml = ref();
const isShowId = ref<boolean>(false);
let map = new Map();

const currentDevice = computed(() => (devList.dev && devList.dev.length > 0 ? devList.dev[0] : null));

watchEffect(() => {
  if (data.value) {
    try {
      onWebsocketMessage(JSON.parse(data.value));
    } catch (error) {
      console.error("WebSocket message parse error:", error);
    }
  }
});

const devList = reactive<{ dev: any[] }>({
  dev: [
    // Default structure for a device, can be empty array initially if fetched
    // {
    //   id: '', algorithmNum: 0, useAlgorithmNum: 0, cameraNum: 0, useCameraNum: 0,
    //   algorithmCameraNum: 0, useAlgorithmCameraNum: 0, category: 'KYAIBox', category_dictText: 'KYAIBox',
    //   deviceIp: null, deviceName: 'KYAIBox', factoryName: 'ky', factoryName_dictText: 'KYLAND',
    //   licenseStatus: '0', status: '0', status_dictText: '离线', disk_total: 0, disk_used: 0,
    //   disk_percent: 0, AIVer: '1.0.0', showRun: false, devRun: {}, machineId: '',
    // },
  ],
});
const mainState = reactive({
  hostIp: '' as string,
  hostUrl: window.location.host,
  devListTemp: [],
});

function handleEdit(record: Recordable) {
  // 填充获取到的设备SN码到deviceNumber
  record.deviceNumber = devSn.value;
  openModal(true, {
    record,
    isUpdate: true,
    showFooter: true,
  });
}

function handleLicenseOk() {
  reload();
}

function handleSuccess() {
  reload();
}

onMounted(async () => {
  const index = mainState.hostUrl.indexOf(':');
  mainState.hostIp = mainState.hostUrl.slice(0, index);

  const devs = await list('');
  devList.dev = devs.records || [];

  mainState.devListTemp = devs.records;
  if (devList.dev.length > 0) {
    devList.dev.forEach((item) => {
      item.showRun = false;
    });
  }

  if (currentDevice.value) {
    devIp.value = currentDevice.value.deviceIp;
  }

  await getDeviceInfo();
  await devMachine({});
  const softVersion =  await getAllVersion()
  console.log('softVersion', softVersion)
  // currentDevice.value.AIVer = softVersion.algorithm_pkg? softVersion.algorithm_pkg : '';
});

async function reload() {
  const devs = await list('');
  devList.dev = devs.records || [];
  if (devList.dev.length > 0) {
    devList.dev.forEach((item) => {
      const existingDevice = mainState.devListTemp.find(d => d.id === item.id);
      item.showRun = existingDevice ? existingDevice.showRun : false;
    });
  }
  mainState.devListTemp = JSON.parse(JSON.stringify(devs.records || []));
  await getDeviceInfo();
  if (currentDevice.value) {
    await devMachine(currentDevice.value);
    devIp.value = currentDevice.value.deviceIp;
  }
}

async function onWebsocketMessage(data) {
  for (let dev of devList.dev) {
    if (dev.deviceIp == data.devIp) {
      map.set(data.devIp, new Date().getTime());
      dev.disk_total = data.diskTotal;
      dev.disk_used = data.diskUse;
      dev.disk_percent = data.diskRate ? Number.parseInt(data.diskRate) : 0;
      dev.AIVer = data.aiVer;
      dev.status = '1';
      dev.status_dictText = '在线';
      dev.devRun = {
        cpuRate: Number.parseInt(data.cpuRate),
        gpuRate: Number.parseInt(data.gpuRate),
        memRate: Number.parseInt(data.memRate),
        diskUse: data.diskUse,
        diskRate: Number.parseInt(data.diskRate),
      };
      break;
    }
  }
}

async function getDeviceInfo() {
  const devInfo = await getDevInfo();
  if (devInfo && devInfo.devList) {
    for (let data of devInfo.devList) {
      for (let dev of devList.dev) {
        if (dev.deviceIp == data.devIp) {
          dev.disk_total = data.diskTotal;
          dev.disk_used = data.diskUse;
          dev.disk_percent = data.diskRate ? Number.parseInt(data.diskRate) : 0;
          dev.status = data.status;
          // dev.status = '1';
          dev.useCameraNum = data.useCameraNum;
          dev.useAlgorithmCameraNum = data.useAlgorithmCameraNum;
          dev.useAlgorithmNum = data.useAlgorithmNum;
          dev.status_dictText = data.status == '1' ? '在线' : '离线';
          dev.AIVer = data.aiVer;
        }
      }
    }
  }
  devList.dev.sort((a, _) => {
    return a.status === '1' ? -1 : 1;
  });
}

// function showRunReport(item) {
//   if (!item) return;
//   item.showRun = !item.showRun;
//   if (item.showRun && item.devRun && Object.keys(item.devRun).length === 0) {
//   } else if (!item.showRun) {
//   }
// }

async function saveConfig(item) {
  if (!item) return;
  createConfirm({
    iconType: 'info',
    title: '配置下发',
    content: '配置下发将重新同步相机和算法配置?',
    onOk: async () => {
      try {
        await saveGroupAll({});
        await saveByDev({devId: item.id});
        createMessage.success('配置已成功下发');
      } catch (e) {
        createMessage.error('配置下发失败');
      }
    }
  })
}

async function devReboot(item) {
  const ipToReboot = item?.deviceIp || devIp.value;
  if (!ipToReboot) {
    createMessage.error("无法确定设备IP进行重启");
    return;
  }
  devIp.value = ipToReboot;

  createConfirm({
    iconType: 'warning',
    title: '重启算法服务',
    content: '确定要重启当前设备算法服务吗？',
    onOk: async () => {
      try {
        await rebootDev({ip: ipToReboot}, retCancel);
        createMessage.success('重启命令已发送');
      } catch (e) {
        // createMessage.error('重启命令发送失败');
        console.log(e);
      }
    },
  });
}

const devSn = ref("")

async function devMachine(item) {
  const response = await getDeviceSn();
  if (response && response.deviceSn) {
    devSn.value = response.deviceSn;
    isShowId.value = true;
  } else {
    devSn.value = "N/A";
    isShowId.value = true;
  }
}

function retCancel() {

  isShowUpdateAlg.value = false;
  selectFileName.value = '';
  versionSelected.value = currentDevice.value ? currentDevice.value.AIVer : '';
}

function upDeviceVer(item) {
  if (!item) item = currentDevice.value;
  if (!item) return;
  openUpDevice(true, {
    item: item,
  });
}

</script>

<style lang="less" scoped>
.device-management {
  overflow: auto;
  height: 100%;
  background-color: var(--component-background);
}

.device-container {
  height: 100%;
  min-width: 900px;
  padding: 10px;
}

.device-profile {
  padding: 8px;
}

.device-profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .device-identity {
    display: flex;
    align-items: center;
    flex-grow: 1;
    min-width: 0;
  }

  .device-name-text {
    font-size: 20px;
    font-weight: 500;
    color: var(--text-color);
    margin-right: 12px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .device-actions {
    white-space: nowrap;

    :deep(.ant-btn + .ant-btn) {
      margin-left: 10px;
    }

    :deep(.ant-btn-link) {
      padding-left: 8px;
      padding-right: 8px;
      color: var(--primary-color);

      &[disabled] {
        color: var(--disabled-color);
      }
    }

    :deep(.ant-btn-link.ant-btn-dangerous) {
      color: var(--error-color);
    }

    :deep(.ant-divider-vertical) {
      margin: 0 4px;
    }
  }
}

.device-content-col {
  font-size: 13px;
  line-height: 1.8;

  .detail-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 12px 20px;
  }

  .detail-item {
    display: flex;
    flex-direction: column;
    padding: 4px 0;

    .detail-label {
      font-weight: normal;
      color: var(--text-color-secondary);
      margin-bottom: 2px;
      font-size: 12px;

      &.section-title {
        font-weight: 500;
        color: var(--text-color);
        margin-bottom: 8px;
        font-size: 13px;
      }
    }

    .detail-value {
      font-weight: 500;
      color: var(--text-color);
      word-break: break-all;
    }
  }

  .detail-item-full-row {
    grid-column: 1 / -1;
  }

  .detail-storage {
    .storage-content {
      display: flex;
      align-items: center;
      margin-top: 4px;
    }

    .storage-progress {
      flex-grow: 1;
      max-width: 250px;
      margin-right: 12px;
    }

    .storage-text {
      font-size: 12px;
      color: var(--text-color-secondary);
      line-height: 1.4;

      .total-space {
        margin-left: 8px;
      }
    }
  }

  .detail-license {
    .license-info-valid {
      margin-top: 4px;

      .license-item {
        display: flex;
        align-items: center;
        padding: 6px 0;
        font-size: 13px;
        color: var(--text-color);

        .license-icon {
          width: 14px;
          height: 14px;
          margin-right: 8px;
        }

        .license-type-label {
          color: var(--text-color-secondary);
          margin-right: 6px;
          min-width: 110px;
          display: inline-block;
        }

        .license-type-value {
          font-weight: 600;
        }
      }
    }

    .license-info-invalid {
      margin-top: 4px;
      color: var(--error-color);
      font-size: 13px;
      display: flex;
      align-items: center;
    }
  }
}

.no-device-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100% - 50px);
  min-height: 200px;
}

.up-modal {
  font-family: Source Han Sans CN;
  font-size: 13px;
}

:deep(.ant-progress-inner) {
  background-color: #e6f7ff;
}

:deep(.ant-card-body) {
  padding: 12px 16px;
}

.device-image-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding-top: 10px;
}

/* Styles for Custom AI Box based on user's image */
.css-drawn-custom-aibox-container {
  width: 90px; /* Adjusted width */
  height: 110px; /* Adjusted height */
  display: flex;
  align-items: center;
  justify-content: center;
}

.aibox-main-body-custom {
  width: 100%;
  height: 100%;
  background-color: #262626; /* Dark base for fins */
  background-image: repeating-linear-gradient(to bottom,
  #3a3a3a, /* Fin highlight */ #3a3a3a 5px, /* Fin thickness */ #262626 5px, /* Gap color (base) */ #262626 10px /* Fin + Gap total height */);
  border-radius: 3px;
  position: relative;
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.3);
  overflow: hidden; /* Hide parts of side panel that might extend if not perfectly sized */
}

.aibox-side-panel-custom {
  position: absolute;
  right: 0;
  top: 0;
  width: 28px; /* Width of the side panel */
  height: 100%;
  background-color: #1c1c1c; /* Slightly different dark shade for panel */
  border-left: 1px solid #111;
  padding: 5px 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-around; /* Distribute ports */
}

.aibox-screw-custom {
  width: 3px;
  height: 3px;
  background-color: #0a0a0a;
  border-radius: 50%;
  position: absolute;
  left: 3px; /* Position from left edge of side panel */
}

.aibox-screw-custom.screw-tr {
  top: 3px;
}

.aibox-screw-custom.screw-br {
  bottom: 3px;
}

/* Add screw-tl, screw-bl if needed and adjust positioning if they are on the front face of the panel */

.aibox-port-custom {
  width: 16px; /* Width of port representation */
  background-color: #101010; /* Very dark for port itself */
  border: 1px solid #050505;
  border-radius: 1px;
  margin: 3px 0; /* Spacing between ports */
  position: relative; /* For LEDs */
}

.aibox-port-custom.debug-port {
  height: 7px;
}

.aibox-port-custom.ethernet-port {
  height: 12px;
  display: flex; /* For internal LEDs */
  justify-content: space-around;
  align-items: center;
  padding: 0 2px; /* Padding for LEDs inside */
}

.aibox-led-custom {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  animation: led-flicker 1.5s infinite alternate;
}

.aibox-led-custom.led-left {
  background-color: #ffab00; /* Orange/Amber */
}

.aibox-led-custom.led-right {
  background-color: #00c853; /* Green */
}

.aibox-led-custom.animation-delay {
  animation-delay: 0.75s;
}

@keyframes led-flicker {
  0%, 100% {
    opacity: 0.4;
    box-shadow: none;
  }
  50% {
    opacity: 1;
  }
}

/* Dynamic Styling based on device status */
.css-drawn-custom-aibox-container.status-online .aibox-led-custom.led-left {
  background-color: #ffab00; /* Orange/Amber for online */
  animation: led-flicker 1.5s infinite alternate;
}

.css-drawn-custom-aibox-container.status-online .aibox-led-custom.led-right {
  background-color: #00c853; /* Green for online */
  animation: led-flicker 1.5s infinite alternate .75s; /* Staggered animation */
}

.css-drawn-custom-aibox-container.status-offline .aibox-main-body-custom {
  background-image: repeating-linear-gradient(to bottom,
  #2d2d2d, /* Duller Fin highlight for offline */ #2d2d2d 5px,
  #202020 5px, /* Duller Gap color for offline */ #202020 10px);
  box-shadow: 1px 1px 3px rgba(0, 0, 0, 0.2); /* Reduced shadow for offline */
}

.css-drawn-custom-aibox-container.status-offline .aibox-led-custom {
  background-color: #500000; /* Dim Red for offline */
  animation: led-offline-pulse 2.5s infinite ease-in-out;
  opacity: 0.7;
}

@keyframes led-offline-pulse {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.7;
  }
}

/* Ensure default LED colors are set if not explicitly online/offline for some reason, or rely on online as default */
.aibox-led-custom.led-left {
  background-color: #ffab00;
}

/* Default to online colors */
.aibox-led-custom.led-right {
  background-color: #00c853;
}

.aibox-led-custom.animation-delay {
  animation-delay: 0.75s;
}

/* This class may no longer be needed if online state redefines animations */

/* Moved .device-status-tag styles here to apply them globally within the component scope */
.device-status-tag {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px; /* Increased padding */
  border-radius: 14px; /* Adjusted border-radius */
  font-size: 13px; /* Increased font-size */
  line-height: 1.5;
  font-weight: 500; /* Added font-weight */
  white-space: nowrap; /* Ensure no wrapping of internal content */

  .status-icon {
    width: 16px; /* Increased icon size */
    height: 16px; /* Increased icon size */
    margin-right: 7px; /* Adjusted margin */
  }

  &.online {
    background-color: #e6fffb;
    color: #00a870;
  }

  &.offline {
    background-color: #fff1f0;
    color: #cf1322;
  }
}

/* Styles for Custom AI Box based on user's image */
.css-drawn-custom-aibox-container {
  width: 90px; /* Adjusted width */
  height: 110px; /* Adjusted height */
  display: flex;
  align-items: center;
  justify-content: center;
}

.aibox-main-body-custom {
  width: 100%;
  height: 100%;
  background-color: #262626; /* Dark base for fins */
  background-image: repeating-linear-gradient(to bottom,
  #3a3a3a, /* Fin highlight */ #3a3a3a 5px, /* Fin thickness */ #262626 5px, /* Gap color (base) */ #262626 10px /* Fin + Gap total height */);
  border-radius: 3px;
  position: relative;
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.3);
  overflow: hidden; /* Hide parts of side panel that might extend if not perfectly sized */
}

.aibox-side-panel-custom {
  position: absolute;
  right: 0;
  top: 0;
  width: 28px; /* Width of the side panel */
  height: 100%;
  background-color: #1c1c1c; /* Slightly different dark shade for panel */
  border-left: 1px solid #111;
  padding: 5px 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-around; /* Distribute ports */
}

.aibox-screw-custom {
  width: 3px;
  height: 3px;
  background-color: #0a0a0a;
  border-radius: 50%;
  position: absolute;
  left: 3px; /* Position from left edge of side panel */
}

.aibox-screw-custom.screw-tr {
  top: 3px;
}

.aibox-screw-custom.screw-br {
  bottom: 3px;
}

/* Add screw-tl, screw-bl if needed and adjust positioning if they are on the front face of the panel */

.aibox-port-custom {
  width: 16px; /* Width of port representation */
  background-color: #101010; /* Very dark for port itself */
  border: 1px solid #050505;
  border-radius: 1px;
  margin: 3px 0; /* Spacing between ports */
  position: relative; /* For LEDs */
}

.aibox-port-custom.debug-port {
  height: 7px;
}

.aibox-port-custom.ethernet-port {
  height: 12px;
  display: flex; /* For internal LEDs */
  justify-content: space-around;
  align-items: center;
  padding: 0 2px; /* Padding for LEDs inside */
}

.aibox-led-custom {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  animation: led-flicker 1.5s infinite alternate;
}

.aibox-led-custom.led-left {
  background-color: #ffab00; /* Orange/Amber */
}

.aibox-led-custom.led-right {
  background-color: #00c853; /* Green */
}

.aibox-led-custom.animation-delay {
  animation-delay: 0.75s;
}

@keyframes led-flicker {
  0%, 100% {
    opacity: 0.4;
    box-shadow: none;
  }
  50% {
    opacity: 1;
  }
}

/* Dynamic Styling based on device status */
.css-drawn-custom-aibox-container.status-online .aibox-led-custom.led-left {
  background-color: #ffab00; /* Orange/Amber for online */
  animation: led-flicker 1.5s infinite alternate;
}

.css-drawn-custom-aibox-container.status-online .aibox-led-custom.led-right {
  background-color: #00c853; /* Green for online */
  animation: led-flicker 1.5s infinite alternate .75s; /* Staggered animation */
}

.css-drawn-custom-aibox-container.status-offline .aibox-main-body-custom {
  background-image: repeating-linear-gradient(to bottom,
  #2d2d2d, /* Duller Fin highlight for offline */ #2d2d2d 5px,
  #202020 5px, /* Duller Gap color for offline */ #202020 10px);
  box-shadow: 1px 1px 3px rgba(0, 0, 0, 0.2); /* Reduced shadow for offline */
}

.css-drawn-custom-aibox-container.status-offline .aibox-led-custom {
  background-color: #500000; /* Dim Red for offline */
  animation: led-offline-pulse 2.5s infinite ease-in-out;
  opacity: 0.7;
}

@keyframes led-offline-pulse {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.7;
  }
}

/* Ensure default LED colors are set if not explicitly online/offline for some reason, or rely on online as default */
.aibox-led-custom.led-left {
  background-color: #ffab00;
}

/* Default to online colors */
.aibox-led-custom.led-right {
  background-color: #00c853;
}

.aibox-led-custom.animation-delay {
  animation-delay: 0.75s;
}

/* This class may no longer be needed if online state redefines animations */

/* Make sure this is the end of your <style> block or the styles are correctly placed */
</style>
