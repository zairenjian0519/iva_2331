<template>
  <div class="device-management">
    <div class="device-container">
      <a-card :bordered="false" style="height: 100%">
        <a-tabs defaultActiveKey="dev-info">
          <a-tab-pane key="dev-info">
            <template #tab>
              <label>超脑管理</label>
              <div class="tab-title">
                <label class="online-num">{{ onNum }} </label>
                <label style="color: #8a8d99">/{{ devList.dev.length }}</label>
              </div>
            </template>
            <div class="alg-operation">
              <primary-button label="新建超脑" v-auth="'dev:add'" @click="handleAdd"
                              :disabled="false">
                <i class="add-dev-icon"></i>
              </primary-button>
              <div class="input-search">
                <i class="search-icon"></i>
                <a-input v-model:value="searchValue" placeholder="超脑名称"/>
              </div>
            </div>

            <div v-if="showIf">
              <a-row :gutter="[16, 16]" :wrap="true" class="row-top">
                <a-col v-for="(item, index) of devList.dev" :key="index"
                       :span="item.showRun ? 24 : 12">
                  <a-row>
                    <a-col :span="item.showRun ? 12 : 24">
                      <a-card @click="cardClick(item)"
                              :class="item.status === '0' ? 'cardOffline' : ''">
                        <div class="div-top">
                          <div style="width: 35%; float: left">
                            <span
                              style="float: left; padding-top: 3px; font-weight: 500; font-size: 15px">{{
                                item.deviceName
                              }}</span>
                            <div class="div-status">
                              <img style="float: left; margin-left: 6px" alt=""
                                   :src="item.status === '1' ? onLine : offLine"/>
                              <span class="span-status">{{ item.status_dictText }}</span>
                            </div>
                          </div>
                          <div class="operation">
<!--                            <a-button type="link" class="off" @click="syncConfig(item)"
                                      v-show="item.status === '1'"><span>云端获取</span></a-button>
                            <a-button type="link" class="off" @click="syncDevice(item)"
                                      v-show="item.status === '1'"><span>向下同步</span></a-button>-->
                            <a-button type="link" class="off" @click="saveConfig(item)"
                                      v-show="item.status === '1'" v-auth="'dev:conf'">
                              <span>配置下发</span></a-button>
                            <a-button type="link" class="reboot" @click="devReboot(item)"
                                      v-show="item.status === '1'" v-auth="'dev:reboot'">
                              <span>重启算法</span></a-button>
<!--                            <a-button type="link" class="report" @click="showRunReport(item)"
                                      v-show="item.status === '1'"><span>运行报告</span></a-button>-->
<!--                            <a-button type="link" class="reboot" @click="rebootDm()"
                                      v-auth="'dev:reboot'">
                              <span>刷新超脑</span></a-button>-->
                            <div class="line" v-show="item.status === '1'"></div>
                            <div style="display: flex; align-items: center;height: 24px ">
<!--                            <div class="setting">-->
<!--                              <a-button type="link" @click="handleEdit(item)" v-auth="'dev:edit'" class="setting-text">设置-->
                              <a-button type="link" @click="handleEdit(item)" v-auth="'dev:edit'" >设置
                              </a-button>
                              <!--                            <div class="divider"></div>
                                                            <a-dropdown v-model:open="visible">
                                                                <template #overlay>
                                                                  <a-menu>
                                                                    <a-menu-item v-if="item.status === '1'" @click="devMachine(item)">获取机器码</a-menu-item>
                                                                    <a-menu-item v-if="item.status === '1'" @click="devAuthInfo(item)">获取认证信息</a-menu-item>
                                                                    <a-menu-item v-if="item.status === '1'" @click="uploadLicense(item)">上传license</a-menu-item>
                                                                    <a-menu-item v-if="item.status === '1'" @click="upDeviceVer(item)">版本升级</a-menu-item>
                                                                  </a-menu>
                                                                </template>
                                                                <a-button style="padding:0px;margin-left: 3px;" type="text">
                                                                  <DownOutlined />
                                                                </a-button>
                                                              </a-dropdown>-->
                              <a-button type="link" class="delete" @click="handleDelete(item)"
                                        v-auth="'dev:del'"><span>删除</span></a-button>
                            </div>
                          </div>
                        </div>
                        <div class="div-bottom">
                          <img src="../../../assets/svg/icon/img_aibox.svg" class="dev-img"/>
                          <div>
                            <div class="div-info">
                              <div class="div-row">
                                <div>产品型号</div>
                                <span class="info-span">{{ item.category_dictText }}</span>
                              </div>
                              <div class="div-row">
<!--                                <span>机器码</span>
                                <span v-if="isShowId" class="code">{{ item.machineId }}</span>
                                <span v-else class="code">请获取机器码</span>-->
                                <div>设备SN码</div>
                                <span v-if="isShowId" class="info-span">{{ devSn }}</span>
                              </div>
                              <div class="div-row">
                                <div>算法包版本</div>
                                <span class="info-span" >{{item.AIVer}}</span>
                              </div>
                              <div style="height: 20px; padding-left: 12px"></div>
                              <div class="div-row" style="height: 21px">
                                <div>存储空间</div>
                                <a-progress
                                  :percent="item.disk_percent ? item.disk_percent : 0"
                                  style="width: calc(100% - 126px); margin-left: 16px"
                                  stroke-color="#7bbdc7"
                                  :status="item.disk_percent ? (item.disk_percent > 85 ? 'exception' : 'active') : 'active'"
                                  :show-info="false"
                                />
                              </div>
                              <div class="div-row">
                              <span class="info-span" style="margin-left: 94px">
                                占量 ({{
                                  item.disk_used ? (item.disk_used / 1024).toFixed(0) : '-'
                                }}G, {{ item.disk_percent ? item.disk_percent : 0 }}%)</span>
                                <span style="margin-right: 32px; float: right; margin-top: 0">总量 {{
                                    item.disk_total ? (item.disk_total / 1024).toFixed(0) : '--'
                                  }}G</span><br/>
                              </div>
                              <div
                                v-if="item.licenseStatus !== null && item.licenseStatus !== '0' && item.licenseStatus !== ''">
                                <div class="div-num" style="width: 140px">
                                  <img alt="" :src="treeCameraIcon" class="div-num-img"/>
                                  <label>路数授权</label> <label style="margin-left: 2px">
                                  {{ item.useCameraNum }}/{{ item.cameraNum }} </label>
                                </div>
                                <div class="div-num" style="margin-left: 8px; width: 190px">
                                  <img alt="" :src="treeAlgIcon" class="div-num-img"/>
                                  <label>算法个数授权</label> <label> {{
                                    item.useAlgorithmNum
                                  }}/{{ item.algorithmNum }}</label>
                                </div>
                                <div class="div-num" style="margin-left: 8px; width: 190px">
                                  <img alt="" :src="treeAlgIcon" class="div-num-img"/>
                                  <label>可配算法相机授权</label> <label>
                                  {{ item.useAlgorithmCameraNum }}/{{
                                    item.algorithmCameraNum
                                  }}</label>
                                </div>
                              </div>
                              <div v-else style="margin-top: 16px">
<!--                                <img style="padding-top: 8px; float: left" :src="infoIconWarning" alt=""/>
                                <label>未进行授权, 去
                                  <a-button style="padding-left: 0" type="link" danger
                                            @click="uploadLicense(item)">授权
                                  </a-button>
                                </label>-->
                                <span style="color:red;"><a-icon type="info-circle"/> 未授权请联系管理员</span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </a-card>
                    </a-col>
                    <a-col :span="12" v-if="item.showRun">
                      <RunReport ref="runReportHtml" :dev-run-info="item.devRun"/>
                    </a-col>
                  </a-row>
                </a-col>
              </a-row>
            </div>

          </a-tab-pane>
          <a-tab-pane tab="升级记录" key="dev-log" v-if="false">
            <a-row class="log-row">
              <a-col :span="2">
                <a-anchor :affix="false">
                  <div style="margin-bottom: 10px">
                    <label class="dev-label">设备列表</label>
                  </div>
                  <a-anchor-link v-for="item of devList.dev" :key="item.id" :href="item.id"
                                 :title="item.deviceName" @click.prevent="devChange(item)"
                                 class="anchor"/>
                </a-anchor>
              </a-col>
              <!--          <a-col :span="1">-->
              <!--            <a-divider type="vertical" style="height: 200px; background-color: #d9d9d9;"></a-divider>-->
              <!--          </a-col>-->
              <a-col :span="22">
                <DeviceLog ref="devTree"/>
              </a-col>
            </a-row>
          </a-tab-pane>
        </a-tabs>
      </a-card
      >
    </div>
  </div>

  <DeviceManagementModal @register="registerModal" @success="handleSuccess"/>
  <UpDevice @register="upDeviceModal" @success="handleSuccess"/>
  <UpLicense @register="upLicenseModal" @success="handleSuccess"
             @handle-license-ok="handleLicenseOk"/>
  <a-modal class="up-modal" v-model:visible="isShowUpdateAlg" destroyOnClose title="升级算法包版本"
           ok-text="升级" @ok="handleOK" @cancel="cancelUpdate" width="558px" style="height: 320px">
    <a-row :gutter="[16, 16]">
      <a-col :span="24">
        <a-row style="margin-top: 20px">
          <a-col :offset="1" :span="20">
            <a-tooltip placement="topLeft">
              <template #title>
                设备正在使用的算法包版本，可通过下拉框切换至已安装在本机的历史算法包版本
              </template>
              <div><label style="float: left">当前算法包版本</label><img style="padding-top: 3px"
                                                                         :src="infoIconWarning"
                                                                         alt=""/></div>
            </a-tooltip>
          </a-col>
        </a-row>
        <a-row style="margin-top: 8px">
          <a-col :offset="1" :span="20">
            <a-select style="width: 240px" v-model:value="versionSelected">
              <option v-for="(item, index) of versionList" :key="index" :value="item">{{
                  item
                }}
              </option>
            </a-select>
            <!--              <label>{{devAlgVer}}</label>-->
          </a-col>
        </a-row>
        <a-row style="margin-top: 8px">
          <a-col :offset="1" :span="20">
            <label>选择算法包</label>
          </a-col>
        </a-row>
        <a-row style="margin-top: 8px">
          <a-col :offset="1" :span="23">
            <a-upload :action="uploadUrl" :headers="getheader()" :max-count="maxCount"
                      @change="uploadChange" :show-upload-list="false">
              <a-input style="width: 500px" v-model:value="selectFileName">
                <template #suffix>
                  <img :src="infoIconWarning" alt=""/>
                </template>
              </a-input>
            </a-upload>
          </a-col>
        </a-row>
        <a-row style="margin-top: 1px; margin-bottom: 60px">
          <a-col :offset="1" :span="20">
            <div><img style="float: left; margin-top: 3px" :src="infoIconWarning" alt=""/><label>算法包升级后需重启设备生效</label>
            </div>
          </a-col>
        </a-row>
      </a-col>
    </a-row>
  </a-modal>
  <a-modal v-model:visible="isShowUpdateLoading" destroy-on-close title="正在升级" :footer="null"
           width="399px" :closable="false">
    <div style="height: 160px; padding-top: 60px; padding-left: 60px">
      <img :src="navIcon" alt="" style="float: left"/>
      <label style="float: left">正在升级算法包，请稍后再执行其他操作</label>
    </div>
  </a-modal>
  <a-modal v-model:visible="isShowUpdateRet" destroy-on-close title="升级成功" :footer="null"
           width="399px" :closable="false">
    <div style="height: 160px; padding-top: 50px; padding-left: 80px">
      <div style="height: 30px">
        <img :src="navIcon" alt="" style="float: left"/>
        <label style="float: left">算法包已升级，重启服务以生效</label>
      </div>
      <div style="margin-left: 50px">
        <a-button style="color: #adb4cc" type="link" @click="retCancel">稍后</a-button>
        <a-button style="margin-left: 20px" type="link" @click="devReboot">重启</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref, watch, watchEffect} from 'vue';
import {Anchor, AnchorLink, Col, Dropdown, Menu, MenuItem, Progress, Row} from 'ant-design-vue';
import {
  changeStatus,
  deleteOne,
  exportImg,
  getAuthInfo,
  getDeviceSn,
  getDevInfo,
  getVerInfo,
  list,
  rebootDev,
  rebootDM,
  saveByDev,
  setAIVer,
  updateAlg,
  uploadUrl
} from './DeviceManagement.api';
import RunReport from './components/RunReport.vue';
import {useModal} from '/@/components/Modal';
import DeviceManagementModal from './components/DeviceManagementModal.vue';
import UpDevice from './components/UpDevice.vue';
import DeviceLog from './components/DeviceLog.vue';
import {useGlobSetting} from '/@/hooks/setting';
import {useWebSocket} from '@vueuse/core';
import {useUserStore} from '/@/store/modules/user';
import {getToken} from '/@/utils/auth';
import {useMessage} from '/@/hooks/web/useMessage';
import treeCameraIcon from '/@/assets/svg/icon/tree_icon_camera.svg';
import treeAlgIcon from '/@/assets/svg/icon/tree_icon_algorithm.svg';
import infoIconWarning from '/@/assets/svg/icon/info_icon_warning.svg';
import navIcon from '/@/assets/svg/icon/nav_icon_detection_default.svg';
import offLine from '/@/assets/svg/icon/icon_offline.svg';
import onLine from '/@/assets/svg/icon/icon_online.svg';
import PrimaryButton from '/@/components/BaseButton/PrimaryButton.vue';
import UpLicense from '/@/views/base/deviceManagement/components/UpLicense.vue';
import axios from 'axios';
import {saveGroupAll} from '/@/views/base/cameraGroup/CameraGroup.api';

const visible = ref(true)
//注册model
const [registerModal, {openModal}] = useModal();
const [upDeviceModal, {openModal: openUpDevice}] = useModal();
const [upLicenseModal, {openModal: openUpLicense}] = useModal();
const ACol = Col;
const ARow = Row;
const AProgress = Progress;
const AAnchor = Anchor;
const AAnchorLink = AnchorLink;
const ADropdown = Dropdown;
const AMenu = Menu;
const AMenuItem = MenuItem;
const glob = useGlobSetting();
const userId = useUserStore().getUserInfo?.id;
const hostUrl = window.location.host;
const index = window.location.host.indexOf(':');
const hostIp = window.location.host.slice(0, index);
let url = glob.domainUrl.replace('https://', 'wss://').replace('http://', 'ws://') + '/devWebsocket/' + userId;
if (hostUrl != 'localhost:3100') {
  url = url.replace(/\/\/([^\s/:]+)(:\d+)?/, '//' + hostIp + ':8088');
}
const {status, data, send, close, open, ws} = useWebSocket(url);
const {createMessage, createConfirm} = useMessage();
const devId = ref();
const devIp = ref<string>('');
const devAlgVer = ref<string>('');
const searchValue = ref<string>('');
const maxCount = ref(1);
const isShowUpdateAlg = ref<boolean>(false);
const isShowUpdateLoading = ref<boolean>(false);
const isShowUpdateRet = ref<boolean>(false);
const selectFileName = ref<string>('');
const versionSelected = ref<string>('');
const versionList = ref<string[]>([]);
const runReportHtml = ref();
const isShowId = ref<boolean>(false);
let map = new Map();
// const currentIndex = ref<number>();
const onNum = computed(() => {
  let onList = devList.dev.filter((el) => {
    return el.status === '1';
  });
  return onList.length;
});

function getheader() {
  return {'X-Access-Token': getToken()};
}

watch(searchValue, (value) => {
  if (value) {
    devList.dev = devList.dev.filter((item) => {
      return item.deviceName.match(value);
    });
  } else {
    devList.dev = mainState.devListTemp;
  }
});

watchEffect(() => {
  if (data.value) {
    try {
      // console.log(data.value);
      onWebsocketMessage(JSON.parse(data.value));
    } catch (error) {
    }
  }
});

const devTree = ref();

const devList = reactive({
  dev: [
    {
      id: '',
      algorithmNum: 0,
      useAlgorithmNum: 0,
      cameraNum: 0,
      useCameraNum: 0,
      algorithmCameraNum: 0,
      useAlgorithmCameraNum: 0,
      category: 'KYAIBox',
      category_dictText: 'KYAIBox',
      deviceIp: null,
      deviceName: 'KYAIBox',
      factoryName: 'ky',
      factoryName_dictText: 'KYLAND',
      licenseStatus: '111',
      status: '0',
      status_dictText: '离线',
      disk_total: 0,
      disk_used: 0,
      disk_percent: 0,
      AIVer: '1.0.0',
      showRun: false,
      devRun: {},
      machineId: '',
    },
  ],
});
const mainState = reactive({
  hostIp: '' as string,
  hostUrl: window.location.host,
  devListTemp: [],
});

function cardClick(item) {
  // console.log(item);
  devId.value = item.id;
  devIp.value = item.deviceIp;
  devAlgVer.value = item.AIVer;
}

function handleAdd() {
  openModal(true, {
    isUpdate: false,
    showFooter: true,
  });
  showIf.value = true
}

function handleEdit(record: Recordable) {
  openModal(true, {
    record,
    isUpdate: true,
    showFooter: true,
  });
}

async function showUpdate(item) {
  cardClick(item);
  selectFileName.value = '';
  isShowUpdateAlg.value = true;
  versionSelected.value = devAlgVer.value;
  versionList.value = await getVerInfo({id: item.id});
}

function cancelUpdate() {
  // console.log(versionSelected.value);
  isShowUpdateAlg.value = false;
  versionSelected.value = '';
}

function handleLicenseOk() {
  reload();
}

function handleSuccess() {
  reload();
}

const showIf = ref(false)

function handleDelete(record) {
  createConfirm({
    iconType: 'error',
    title: '删除',
    content: '确定要删除当前设备吗?',
    onOk: () => {
      showIf.value = false
      deleteOne({id: record.id}, handleSuccess);
    },
  });
}

onMounted(async () => {
  const index = mainState.hostUrl.indexOf(':');
  mainState.hostIp = mainState.hostUrl.slice(0, index);
  const devs = await list('');
  devList.dev = devs.records || [];
  if (devList.dev.length != 0) {
    showIf.value = true
  }
  mainState.devListTemp = devs.records;
  devList.dev.forEach((item) => {
    item.showRun = false;
  });
  await getDeviceInfo();

  await devMachine({})
});

async function reload() {
  const devs = await list('');
  devList.dev = devs.records;
  await getDeviceInfo();
}

async function onWebsocketMessage(data) {
  // console.log(data.devIp, 'websocket');
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
      // await changeStatus({ip: dev.deviceIp, status: dev.status});
      // break;
    }
    // await compareTime(dev.deviceIp);
  }

  // devList.dev.sort((a, b)=>{
  //   return a.status === '1' ? -1 : 1;
  // });
}

//新增方法：websocket当前时间对比上次推送时间，超过一定时间未推送状态则改为离线
async function compareTime(deviceIp) {
  const lastTime = map.get(deviceIp);
  if (lastTime) {
    // console.log(deviceIp, 'deviceIp');
    const diff = new Date().getTime() - lastTime;
    // console.log(new Date().getTime(), '当前时间');
    // console.log(lastTime, '上次时间');
    // console.log(diff, '时间差');
    if (diff > 30000) {
      for (let dev of devList.dev) {
        if (dev.deviceIp == deviceIp) {
          dev.status = '0';
          dev.status_dictText = '离线';
          await changeStatus({ip: dev.deviceIp, status: dev.status});
        }
      }
    }
  }
}

async function getDeviceInfo() {
  const devInfo = await getDevInfo();
  // console.log(devInfo, 'devinfo');
  for (let data of devInfo.devList) {
    // console.log(data.status, 'data');
    for (let dev of devList.dev) {
      if (dev.deviceIp == data.devIp) {
        dev.disk_total = data.diskTotal;
        dev.disk_used = data.diskUse;
        dev.disk_percent = data.diskRate ? Number.parseInt(data.diskRate) : 0;
        dev.status = data.status;
        dev.useCameraNum = data.useCameraNum;
        dev.useAlgorithmCameraNum = data.useAlgorithmCameraNum;
        dev.useAlgorithmNum = data.useAlgorithmNum;
        dev.status_dictText = data.status === '1' ? '在线' : '离线';
        dev.AIVer = data.aiVer;
        // break;
      }
    }
  }
  devList.dev.sort((a, b) => {
    return a.status === '1' ? -1 : 1;
  });
}

function devNum() {
}

function uploadChange(info) {
  if (info.file.status === 'done') {
    if (info.file.response.success) {
      // reload();
      createMessage.success(`${info.file.name} 上传成功!`);
      selectFileName.value = info.file.name;
    } else {
      createMessage.error(`${info.file.response.message}`);
    }
  } else if (info.file.status === 'error') {
    createMessage.error(`${info.file.response.message}`);
  }
}

function handleOK() {
  if (selectFileName.value == '' && versionSelected.value === devAlgVer.value) {
    createMessage.error('请选择文件');
    return;
  } else if (selectFileName.value !== '') {
    isShowUpdateLoading.value = true;
    isShowUpdateAlg.value = false;
    const res = updateAlg(
      {
        id: devId.value,
        path: selectFileName.value,
        type: '3',
      },
      updateCallbackSuccess,
      updateCallbackError
    );
  } else if (versionSelected.value !== devAlgVer.value) {
    setAIVer(
      {
        id: devId.value,
        ver: versionSelected.value,
      },
      updateCallbackSuccess,
      updateCallbackError
    );
  }
}

function updateCallbackSuccess() {
  isShowUpdateLoading.value = false;
  isShowUpdateRet.value = true;
}

function updateCallbackError() {
  isShowUpdateLoading.value = false;
}

function showRunReport(item) {
  // isShowUpdateRet.value = true;
  item.showRun = !item.showRun;
  devList.dev.forEach((item) => (item.devRun = {}));
  // if(runReportHtml.value){
  //   console.log(runReportHtml);
  //   runReportHtml.value.clearData();
  // }
}

async function syncConfig(item) {
  // const url = 'http://10.10.10.68:8080/kyscene/deviceManagement/deviceManagement/devGetRequest';
  // const url = 'http://' + item.deviceIp + ':8088' + '/kyscene/deviceManagement/deviceManagement/getConfigInfo';
  const url = '/kyscene/deviceManagement/deviceManagement/devGetRequest';
  const res: any = await axios.get(url, {
    params: {
      devIp: item.deviceIp,
    },
  });
  // console.log(res, 333);
  if (res.data.message === '同步成功') {
    createMessage.success(res.data.message);
  } else {
    createMessage.error(res.data.message);
  }
}

async function syncDevice(item) {
  // const url = 'http://10.10.10.68:8080/kyscene/deviceManagement/deviceManagement/devGetRequest';
  // const url = 'http://' + item.deviceIp + ':8088' + '/kyscene/deviceManagement/deviceManagement/getConfigInfo';
  const url = '/kyscene/deviceManagement/deviceManagement/devPostRequest';
  const res: any = await axios.get(url, {
    params: {
      devIp: item.deviceIp,
    },
  });
  if (res.data.message === '同步成功') {
    createMessage.success(res.data.message);
  } else {
    createMessage.error(res.data.message);
  }
}

async function saveConfig(item) {
  createConfirm({
    iconType: 'info',
    title: '配置下发',
    content: '配置下发将重新同步相机和算法配置',
    onOk: async () => {
      await saveGroupAll({});
      await saveByDev({devId: item.id});
      //下发轮询组配置
    }
  })
}

async function devReboot(item) {
  await rebootDev({ip: item.deviceIp ? item.deviceIp : devIp.value}, retCancel);
}

async function rebootDm() {
  createConfirm({
    iconType: 'warning',
    title: '刷新超脑',
    content: '刷新超脑将重启DM进程，不要频繁进行操作， 一般设备不在线执行此操作',
    onOk: async () => {
      await rebootDM({});
    },
  });
}

const devSn = ref("")
async function devMachine(item) {
  // await getMachineId({devId: item.id});
  const response = await getDeviceSn()
  if (response && response.deviceSn) {
    devSn.value = response.deviceSn
  }
  isShowId.value = true;
  // currentIndex.value = index;
}

async function devAuthInfo(item) {
  await getAuthInfo({devId: item.id});
  reload();
}

function retCancel() {
  isShowUpdateRet.value = false;
  isShowUpdateAlg.value = false;
}

function devChange(item) {
  devTree.value.updateData(item.id);
}

async function exportDevImg(item) {
  const data = await exportImg({id: item.id});

  let link = document.createElement('a');
  link.style.display = 'none';
  link.href = 'http://' + item.deviceIp + ':8038/download' + data.result;
  link.setAttribute('download', item.deviceName + '-样本数据');
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link); //下载完成移除元素
}

function upDeviceVer(item) {
  openUpDevice(true, {
    item: item,
  });
}

function uploadLicense(item) {
  openUpLicense(true, {
    item: item,
  });
}
</script>

<style lang="less" scoped>
.device-management {
  overflow: auto;

  height: 100%;
}

.device-container {
  height: 100%;
  min-width: 1200px;
}

.row-top {
  margin-top: 8px;
}

.online-num {
  color: var(--secondary-text-color);
}

.info-span {
  margin-left: 10%;
}

.add-dev-icon {
  background: url('/@/assets/svg/icon/editor_btn_adddevice_default.svg');
  width: 14px;
  height: 14px;
  display: inline-block;
}

.dev-img {
  padding: 2px;
  float: left;
  margin-top: 5%;
}

.div-top {
  padding: 10px;
}

:deep .ant-input {
  height: 24px;
  width: 220px;
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

.alg-operation {
  display: flex;

  .delete-btn {
    margin-left: 8px;

    &:hover {
      .delete-icon {
        background: url('/@/assets/svg/icon/editor_btn_delete_hover.svg');
        width: 14px;
        height: 14px;
      }
    }
  }

  .input-search {
    // display: flex;
    position: relative;
    margin-left: 8px;
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

  .delete-icon {
    background: url('/@/assets/svg/icon/editor_btn_delete_default.svg');
    width: 20px;
    height: 20px;
    display: inline-block;
  }
}

.operation {
  display: flex;
  justify-content: flex-end;
  min-width: 265px;
}

.dropdown-icon {
  display: inline-block;
  cursor: pointer;
  background: url('/@/assets/svg/icon/top_icon_arrowdown_default.svg');
  width: 8px;
  height: 4px;

  &:hover {
    background: url('/@/assets/svg/icon/top_icon_arrowdown_hover.svg');
    width: 8px;
    height: 4px;
  }
}

.delete,
.reboot,
.off,
.report {
  display: flex;
  justify-content: center;
  align-items: center;
}

.delete {
  width: 42px;
  height: 24px;
  text-align: center;
  background-color: transparent;
  font-family: Source Han Sans CN;
  color: var(--serious-text-color);
  font-size: 13px;

  &:hover {
    background-color: rgba(204, 102, 102, 0.2);
    border-radius: 4px;
  }
}

//.reboot {
//  width: 50px;
//  height: 24px;
//  font-family: Source Han Sans CN;
//  color: #adb3cc;
//  font-size: 13px;
//  text-align: center;
//  background-color: transparent;
//
//  &:hover {
//    background-color: var(--fill-three-color);
//    border-radius: 4px;
//  }
//}

.off,
.reboot,
.report {
  font-family: Source Han Sans CN;
  color: #8c94b3;
  font-size: 13px;
  background-color: transparent;
  width: 76px;
  height: 24px;

  &:hover {
    background-color: var(--fill-three-color);
    border-radius: 4px;

    background-color: var(--fill-three-color);
    border-radius: 4px;
  }
}

.line {
  width: 1px;
  height: 16px;
  margin: 4px 4px 0 4px;
  background-color: #d3d8e699;
}

.setting {
  display: flex;
  height: 24px;
  align-items: center;
  width: 80px;
  background: transparent;

  .divider {
    // width: 1px;
    // height: 12px;
    // background-color: rgba(211, 216, 230, 0.6);
    display: none;
  }

  &:hover {
    border: 1px solid #ccc;

    .divider {
      width: 1px;
      height: 20px;
      background-color: #ccc;
      display: block;
    }
  }
}

.code {
  margin-left: 52px;
}

.div-bottom {
  width: 100%;
  padding: 5px;
  float: left;
}

.div-run {
  float: right;
  margin-top: 5%;
  width: 15%;
}

.div-info {
  width: 75%;
  float: left;
  margin: 1%;
  font-size: 13px;
  font-family: Source Han Sans CN;
}

.div-info span {
  margin-top: 2%;
}

.div-num {
  float: left;
  /*margin-left: 8px;*/
  margin-top: 16px;
  text-align: center;
  /*width: 179px;*/
  height: 28px;
  background-color: #edeff6;
  border-radius: 4px;
  padding: 3px;
  font-family: Source Han Sans CN;
  font-size: 13px;
  display: flex;
}

.div-num label {
  margin-left: 7px;
}

.div-num-img {
  float: left;
  margin-top: 3px;
  margin-left: 4px;
  width: 14px;
  height: 14px;
}

.div-row div {
  float: left;
  width: 78px;
}

.div-row .info-span {
  margin-left: 16px;
}

.div-row {
  margin-top: 2%;
}

.div-status {
  background-color: #edeff6;
  float: left;
  margin-left: 3%;
  border-radius: 20px;
  width: 70px;
  text-align: center;
  height: 28px;
  padding-top: 3px;
}

.span-status {
  font-size: 10px;
  margin-left: 1px;
  font-family: fantasy;
  float: left;
  margin-top: 3px;
}

.cardOffline {
  background: #f0f3f8;
  font-family: Source Han Sans CN;
  color: var(--cancel-text-color);
  font-size: 12px;
}

:deep .ant-anchor-wrapper {
  // overflow: hidden;
  border-right: 2px solid var(--weak-split-line-color);
  // padding-right: 70px;
  margin-top: 12px;
}

:deep.ant-anchor-link-title {
  font-family: Source Han Sans CN;
  color: var(--cancel-text-color);
  font-size: 13px;
}

:deep .ant-anchor-link-active > .ant-anchor-link-title {
  color: #486ce5;
}

:deep .ant-anchor-ink {
  display: none;
}

:deep .ant-anchor .ant-anchor-link {
  padding: 4px 0 4px 16px;
  display: flex;
}

:deep .ant-anchor .ant-anchor-link-active {
  border-right: 2px solid #7089ed;
  margin-right: -70px;
}

:deep .ant-anchor-ink-ball {
  position: absolute;
  left: 50%;
  display: none;
  background-color: #fff;
  transform: translateX(-50%);
  transition: top 0.3s ease-in-out;
}

:deep .ant-anchor .ant-anchor-ink-ball {
  display: none;
}

.dev-label {
  width: 52px;
  height: 13px;
  font-family: Source Han Sans CN;
  color: #575b66;
  font-size: 13px;
}

.add-dev-btn {
  width: 100px;
  height: 24px;
  background-color: #738ce6;
  border-radius: 4px;
  padding: 1px;
}

.add-dev-span {
  width: 52px;
  height: 13px;
  font-family: Source Han Sans CN;
  color: rgba(255, 255, 255, 0.95);
  font-size: 13px;
  text-align: center;
  float: right;
  margin-right: 14px;
  margin-bottom: 5px;
}

.del-dev-btn {
  width: 76px;
  height: 23.5px;
  background-color: #e0e4ef;
  border: 1px solid;
  border-color: #e0e4ef;
  border-radius: 4px;
  padding: 1px;
}

.del-dev-span {
  width: 26px;
  height: 13px;
  font-family: Source Han Sans CN;
  color: #474e66;
  font-size: 13px;
  text-align: center;
}

.up-modal {
  font-family: Source Han Sans CN;
  font-size: 13px;
}

.tab-title {
  width: 36px;
  height: 20px;
  background-color: #e8ecf3;
  border-radius: 10px;
  float: right;
  font-size: 12px;
  text-align: center;
  margin-left: 4px;
  margin-top: 1px;
}
</style>
