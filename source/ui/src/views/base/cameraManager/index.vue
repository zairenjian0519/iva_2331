<template>
  <div class="camera-main">
    <div class="camera-main-container">
      <a-layout>
        <a-layout-sider class="cameraLeft">
          <div class="camera-title">
            <span class="title" style="margin-left: 12px">相机组</span>
          </div>
          <CameraLeftTree ref="cameraTreeRef" @select="selectChange"
                          @handle-click-tab="handleClickTab"/>
        </a-layout-sider>
        <a-layout-content class="cameraRight">
          <div id="cameraRightFist">
            <div class="titleStyle">
              <span class="title" style="font-weight: bold">{{ cameraInfo.deviceName }}</span>
              <span>
                <img :src="cameraIcon" alt="" class="camera-icon"/>
              </span>
              <span>{{ cameraInfo.onLine }}/{{ cameraInfo.total }}</span>
              <template
                v-if="cameraInfo.currentIndex === 0 && cameraInfo.deviceName !== '全部相机'">
                <a-divider type="vertical" class="divider"/>
                <div>
                  <span class="title">剩余license:</span>
                  <span class="title-num">{{ cameraInfo.cameraAlgLicense.canUseNum }}</span>
                </div>
              </template>
            </div>
            <div class="buttonStyle">
              <primary-button class="search-btn" @click="addCamernOnClick()" v-auth="'camera:add'"
                              :disabled="false" label="新建相机">
                <img :src="cameraAdd" alt=""/>
              </primary-button>
              <secondary-button class="delete-btn" @click="deleteCamera()"
                                v-auth="'camera:deleteBatch'" label="批量删除">
                <i class="delete-icon"></i>
              </secondary-button>
              <a-divider type="vertical" style="margin-left: 12px"/>
              <secondary-button class="import-btn" @click="importCamera()" v-auth="'camera:import'"
                                label="导入">
                <i class="import-icon"></i>
              </secondary-button>
              <secondary-button class="export-btn" @click="exportCamera()" v-auth="'camera:export'"
                                label="导出">
                <i class="export-icon"></i>
              </secondary-button>
            </div>
            <div class="camera-search">
              <camera-search ref="cameraSearchRef" :back-style="false"
                             @handle-search="searchChange"/>
            </div>
            <div class="tableTop">
              <div class="tableTopLeft">
                <img :src="tableIcon" alt="" class="camera-icon"/>
                <span style="font-size: 13px">{{
                    cameraState.selectedRowKeys.length ? `已选择${cameraState.selectedRowKeys.length} 条数据` : '未选择任何数据'
                  }}</span>
              </div>
              <div class="tableTopRight">
                <div class="switch-item">人脸识别开关：<a-switch v-model:checked="cameraState.enableFaceAlgorithm" @change="handFaceAlgSwitch"/></div>
                <a-button type="danger" size="small" :disabled="false" @click="viewCameraConfig">
                  相机推荐配置，点击查看
                </a-button>
              </div>
            </div>
          </div>
          <div style="margin-top: 0; margin-left: 20px; margin-right: 15px">
            <a-table
              style="font-size: 13px"
              ref="cameraTableRef"
              :row-class-name="rowClass"
              :custom-row="onClickRow"
              :columns="columns"
              :scroll="{ y: tableHight }"
              rowKey="id"
              :data-source="cameraInfo.tableData"
              :pagination="pagination"
              :loading="cameraInfo.loading"
              :row-selection="{ onSelectAll: onSelectAll, onSelect: onSelect, selectedRowKeys: cameraState.selectedRowKeys, onChange: onSelectChange }"
              @change="handChange"
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'operation'">
                  <button class="setting" @click.stop="setFaceGroup(record)">人脸库</button>
                  <button v-if="record.cameraStatus == '4'" @click.stop="reconnect(record)"
                          style="font-size: 13px; " class="setting">立即重连
                  </button>
                  <a-popover>
                    <template #content
                              v-if="record.alarmType === null && cameraInfo.cameraAlgLicense.canUseNum === 0">
                      <div class="tips">
                        <i class="tips-icon"></i>
                        <span>可与相机绑定的算法授权数量已达上限<br/> </span>
                      </div>
                      <p class="paragph">1.请联系管理员申请更多授权；<br/>2.删除已有相机的算法，以在本相机上配置算法；
                      </p>
                    </template>
                    <button @click.stop="configAlg(record)" style="font-size: 13px" class="setting"
                            :disabled="record.alarmType === null && cameraInfo.cameraAlgLicense.canUseNum === 0"
                    >配置算法
                    </button
                    >
                  </a-popover>

<!--                  <button @click.stop="goAlg" style="font-size: 13px; margin-left: 4px"
                          class="management">管理算法
                  </button>-->
                  <button @click.stop="updateCamera(record)"
                          style="font-size: 13px;" v-auth="'camera:edit'"
                          class="edit">编辑
                  </button>
                  <!-- <div ref="deletePopover">  :get-popup-container="getContainer"-->
                  <a-popover placement="topLeft" class="popover" trigger="click">
                    <template #content v-if="clickIndex === index">
                      <div class="tips">
                        <i class="tips-icon"></i>
                        <span>删除相机后将删除其绑定的算法配置，<br/> </span>
                      </div>
                      <p class="text-tips">且不可恢复，请问是否删除？</p>

                      <div class="action">
                        <buttton class="cancel" @click.stop="handleCancel">取消</buttton>
                        <button class="delete" @click.stop="deleteOneCamera(record)">删除</button>
                      </div>
                    </template>

                    <button style="font-size: 13px; margin-left: 4px" v-auth="'camera:delete'"
                            class="delete" @click.stop="handleDelete(index)">删除
                    </button>
                  </a-popover>
                  <!-- </div> -->
                </template>
                <template v-if="column.key === 'cameraStatus'">
                  <span>
                    <a-tag style="background-color: #edeff6; border-radius: 8px">
                      <span
                        :class="record.cameraStatus === '1' ? 'redPointOnline' : 'redPointOffline'"></span>
                      {{ record.cameraStatus === '1' ? '在线' : '离线' }}
                    </a-tag>
                    <base-tooltip :tooltip-title="record.offlineReason"
                                  v-show="record.cameraStatus !== '1'" class="tooltip">
                      <i class="offline-icon"></i>
                    </base-tooltip>
                  </span>
                </template>
                <template v-if="column.key === 'ip'"> {{ record.ip ? record.ip : '-' }}</template>
                <template v-if="column.key === 'alarmType'">
                  {{ record.alarmType ? record.alarmType : '-' }}
                </template>
              </template>
              <template #customFilterIcon="{}">
                <img :src="downIcon" alt=""/>
              </template>
            </a-table>
          </div>
        </a-layout-content>
      </a-layout>
      <!--表单区域-->
      <div>
        <camera-modal ref="addCamera" @handleOk="handleOk"/>
      </div>
      <!--文件导入区域-->
      <div>
        <camera-import v-model:visible="cameraState.importView"
                       @handleImportCancel="handleImportCancel" @finish="seachList"/>
        <AlgorithmModal @register="registerModal" @success="handleSuccess"/>
      </div>
    </div>

    <set-face-group-modal ref="setFaceGroupModal"/>

    <face-confirm-modal ref="faceConfirmModal" @refresh-table="refreshTable"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, onUnmounted, reactive, ref, unref} from 'vue';
import {Popover} from 'ant-design-vue';
import {cameraData, columns, license, tableItem} from './CameraManager.data';
import CameraModal from './components/CameraModal.vue';
import CameraImport from './components/CameraImport.vue';
import CameraSearch from '/@/components/BaseSearch/CameraSearch.vue';
import PrimaryButton from '/@/components/BaseButton/PrimaryButton.vue';
import SecondaryButton from '/@/components/BaseButton/SecondaryButton.vue';
import cameraIcon from '/@/assets/svg/icon/filter_icon_camera.svg';
import tableIcon from '/@/assets/svg/icon/info_icon_tips.svg';
import cameraAdd from '/@/assets/svg/icon/editor_btn_addcamera_default.svg';
import downIcon from '/@/assets/svg/icon/icon_arrowdown_default.svg';
import CameraLeftTree from './components/CameraLeftTree.vue';
import {
  batchDelete,
  deleteOne,
  exportXlsx,
  faceAlgSwitchStatus,
  getAlgLicenseNum,
  list,
  onLineCount,
  reconnectCamera,
  restartAi,
  savaAllByDev
} from './CameraManager.api';
import {useMessage} from '/@/hooks/web/useMessage';
import {XLSX_FILE_SUFFIX, XLSX_MIME_TYPE} from '/@/hooks/system/useMethods';
import {useGo} from '/@/hooks/web/usePage';
import AlgorithmModal from '../algorithmmanager/components/AlgorithmModal.vue';
import {useModal} from '/@/components/Modal';
import {list as algList} from '../algorithmmanager/AlgorithmManager.api';
import {useStore} from '/@/store/modules/alarmTable';
import BaseTooltip from '/@/components/BaseTooltip/BaseTooltip.vue';
import {useUserStore} from "/@/store/modules/user";
import {getToken} from '/@/utils/auth';
import SetFaceGroupModal from "./components/SetFaceGroupModal.vue";
import FaceConfirmModal from "./components/FaceConfirmModal.vue";

const tableHight = computed(() => {
  const y = document.getElementById('cameraRightFist')?.clientHeight;
  return document.body.clientHeight - (y ? y : 230) - 55;
});
const APopover = Popover;
const clickIndex = ref();

const store = useStore();
const {createMessage} = useMessage();
const cameraTreeRef = ref();
const addCamera = ref();
const cameraSearchRef = ref();
const go = useGo();
const [registerModal, {openModal}] = useModal();
let upInterval: NodeJS.Timer | null = null;
let cameraStatusSocket: WebSocket | null = null;
let cameraStatusReconnectTimer: ReturnType<typeof setTimeout> | null = null;
let shouldReconnectCameraStatus = false;
let refreshInProgress = false;

const cameraInfo = reactive({
  tableData: [] as tableItem[],
  currentPage: 1 as number,
  pageSize: 10 as number,
  total: 0 as number,
  showQuickJumper: true,
  factoryList: [],
  option: {
    //查询条件
    cameraName: '',
    location: '',
    cameraStatus: '',
    cameraOwnership: '',
    workType: '',
    categorys: [],
    factory: '',
    ip: '',
    pageNo: 1,
    pageSize: 10 as number,
  },
  isUpdate: false,
  loading: true,
  deviceName: '全部相机',
  onLine: 0 as number,
  datas: {} as cameraData,
  cameraAlgLicense: {} as license,
  currentIndex: 0 as number,
});

//相机状态码字典
// const cameraCode = {
//   '1': '正常在线',
//   '2': '认证失败（用户名/密码错误）',
//   '3': '端口错误',
//   '4': '连接超时（相机不存在/连接被拒绝）',
//   '5': 'RTSP地址错误',
//   '6': '网络不可达/ip地址错误',
//   '7': '其他异常',
// };

const userStore = useUserStore();
const initWebSocket = () => {
  shouldReconnectCameraStatus = true;
  connectCameraStatusWebSocket();
}

const connectCameraStatusWebSocket = () => {
  if (!shouldReconnectCameraStatus || cameraStatusSocket?.readyState === WebSocket.OPEN || cameraStatusSocket?.readyState === WebSocket.CONNECTING) {
    return;
  }
  let userId = unref(userStore.getUserInfo).id;
  const hostname = window.location.hostname.includes(':') ? `[${window.location.hostname}]` : window.location.hostname;
  const url = `ws://${hostname}:8088/kyscene/cameraStatusWebSocket/` + userId;
  const token = (getToken() || '') as string;
  cameraStatusSocket = token ? new WebSocket(url, [token]) : new WebSocket(url);
  cameraStatusSocket.onopen = () => console.log('[CameraStatusWebSocket] 连接成功:', url);
  cameraStatusSocket.onmessage = (event) => websocketCallback(event.data);
  cameraStatusSocket.onerror = (event) => {
    console.error('[CameraStatusWebSocket] 连接错误:', event);
    cameraStatusSocket?.close();
  };
  cameraStatusSocket.onclose = () => {
    cameraStatusSocket = null;
    if (shouldReconnectCameraStatus) {
      cameraStatusReconnectTimer = setTimeout(connectCameraStatusWebSocket, 3000);
    }
  };
}

const websocketCallback = (msg: object | string) => {
  try {
    console.log('接收到相机列表更新事件：', msg);
    const eventMsg: any = typeof msg === 'string' ? JSON.parse(msg) : msg;
    if (eventMsg.event === 'refresh') {
      void refreshTable();
    }
  } catch (error) {
    console.error('处理相机列表更新事件时出错：', error);
  }
}

onMounted(() => {
  refreshTable()
  // 建立websocket
  initWebSocket()
  upInterval = setInterval(() => {
    void refreshTable();
  }, 30000);

  getFaceAlgSwitch()
});

const refreshTable = async () => {
  if (refreshInProgress) return;
  refreshInProgress = true;
  try {
    await seachList();
    cameraTreeRef.value?.reload();
  } finally {
    refreshInProgress = false;
  }
}

onUnmounted(() => {
  if (upInterval) clearInterval(upInterval);
  shouldReconnectCameraStatus = false;
  if (cameraStatusReconnectTimer) clearTimeout(cameraStatusReconnectTimer);
  cameraStatusSocket?.close();
  cameraStatusSocket = null;
})

//表格keys选择事件
const onSelectChange = (selectedRowKeys: [], selectedRows: []) => {
  //如果被删除，选中的数量不对
  cameraState.selectedRowKeys = selectedRowKeys;
  cameraState.selectedRowKeys.forEach((d) => {
    keySet.add(d);
  });

  cameraState.selectedRowKeys.length = 0;
  keySet.forEach((d) => {
    cameraState.selectedRowKeys.push(d);
  });

  cameraState.cameraOwnerships = [];
  selectedRows.forEach((sel: cameraData) => {
    if (cameraState.cameraOwnerships.indexOf(sel.cameraOwnership) < 0) {
      cameraState.cameraOwnerships.push(sel.cameraOwnership);
    }
  });
};

const onSelect = (record, selected, selectedRows, nativeEvent) => {
  if (!selected) {
    keySet.delete(record.id);
  }
};

const onSelectAll = (selected, selectedRows, changeRows) => {
  if (!selected) {
    changeRows.forEach((d) => {
      keySet.delete(d.id);
    });
  }
};
//切换tab
const handleClickTab = (index: number) => {
  cameraInfo.currentIndex = index;
};

function handleSuccess() {
  seachList();
}

//查询相机list
const seachList = async () => {
  cameraInfo.loading = true;
  cameraInfo.option.pageNo = cameraInfo.currentPage;
  cameraInfo.option.pageSize = cameraInfo.pageSize;
  const tableData = await list(cameraInfo.option);
  const online = await onLineCount(cameraInfo.option);
  cameraInfo.total = tableData.total;
  cameraInfo.tableData = tableData.records;
  //计算在线数量
  cameraInfo.onLine = online;
  cameraInfo.loading = false;
  //选择设备时显示可配算法的相机剩余数量
  if (cameraInfo.deviceName != '全部相机') {
    const cameraAlgLicense = await getAlgLicenseNum(cameraInfo.option);
    cameraInfo.cameraAlgLicense = cameraAlgLicense;
  }
};

const cameraState = reactive({
  isShowModel: false,
  selectedRowKeys: [],
  cameraOwnerships: [] as string[],
  importView: false,
  enableFaceAlgorithm: false,
});

const addCamernOnClick = () => {
  let parem;
  if (cameraInfo.option.cameraOwnership) {
    parem = {cameraOwnership: cameraInfo.option.cameraOwnership};
  } else if (cameraInfo.option.location) {
    parem = {location: cameraInfo.option.location};
  }
  addCamera.value.initCameraModel({isUpdate: false, datas: parem});
};

const handleOk = (isUpdate) => {
  if (!isUpdate) {
    const currentPage = Math.ceil((cameraInfo.total + 1) / cameraInfo.pageSize);
    cameraInfo.currentPage = cameraInfo.currentPage < currentPage ? currentPage : cameraInfo.currentPage;
  }
  seachList();
  cameraTreeRef.value.updateTree();
};

const handleImportCancel = () => {
  //导入窗关闭
  cameraState.importView = false;
};

const deleteCamera = async () => {
  if (cameraState.selectedRowKeys.length === 0) {
    createMessage.info('未选择任何数据...');
    return;
  }
  batchDelete({ids: cameraState.selectedRowKeys}, handleDeleteSuccess);
  await cameraTreeRef.value.updateTree();
};

const deleteOneCamera = async (record) => {
  cameraState.cameraOwnerships = [];
  cameraState.cameraOwnerships.push(record.cameraOwnership);
  await deleteOne({id: record.id}, handleDeleteSuccess);
  await cameraTreeRef.value.updateTree();
};

function handleDeleteSuccess() {
  cameraState.selectedRowKeys.length = 0;
  pageCount();

  seachList();
  //调用配置下发请求
  if (cameraState.cameraOwnerships.length > 0) {
    cameraState.cameraOwnerships.forEach((devId) => {
      savaAllByDev({devId: devId});
    });
  }
}

const updateCamera = (record) => {
  addCamera.value.initCameraModel({isUpdate: true, datas: record});
};

const handleDelete = (index) => {
  clickIndex.value = index;
};

const handleCancel = (index) => {
  return !(clickIndex.value = index);
};
//分页
const pagination = computed(() => ({
  total: cameraInfo.total,
  current: cameraInfo.currentPage,
  pageSize: cameraInfo.pageSize,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: () => `共 ${cameraInfo.total} 台`,
  showQuickJumper: true,
  onShowSizeChange: pageSizeChange,
}));

//多选删除时根据当前数据进行分页
function pageCount() {
  let currentPage;
  if (cameraState.selectedRowKeys.length > 0) {
    currentPage = Math.ceil((cameraInfo.total - cameraState.selectedRowKeys.length) / cameraInfo.pageSize);
  } else {
    currentPage = Math.ceil((cameraInfo.total - 1) / cameraInfo.pageSize);
  }
  cameraInfo.currentPage = currentPage < 1 ? 1 : currentPage < cameraInfo.currentPage ? currentPage : cameraInfo.currentPage;
}

//跳转至多少页
const pageSizeChange = (current, size) => {
  cameraInfo.pageSize = size;
};
const keySet = new Set();

//分页点击事件
const handChange = (pagination) => {
  for (const data of cameraState.selectedRowKeys) {
    keySet.add(data);
  }
  cameraInfo.currentPage = pagination.current;
  cameraInfo.pageSize = pagination.pageSize;

  seachList();
};

//选择左侧树查询相机列表
const selectChange = async (parm) => {
  //判断是否点击设备和区域树
  cameraSearchRef.value.clearSearch(); //清空右上方查询条件
  cameraInfo.deviceName = parm.title;
  cameraInfo.option = {...parm};
  cameraInfo.currentPage = 1;
  cameraInfo.pageSize = pagination.value.pageSize;
  seachList();
};
//点击查询按钮的时候
const searchChange = (parm) => {
  cameraInfo.option.cameraName = parm.cameraName;
  cameraInfo.option.categorys = parm.categorys;
  cameraInfo.option.factory = parm.factory;
  cameraInfo.currentPage = 1;
  cameraInfo.pageSize = pagination.value.pageSize;
  seachList();
};

const importCamera = () => {
  cameraState.importView = true;
};
// 点击行
const onClickRow = (record, index) => {
  return {
    onClick: (event) => {
      event.currentTarget.getElementsByClassName('ant-checkbox-wrapper')[0].click();
      store.setState(record.uid);
    },
  };
};
// 行颜色
const rowClass = (record, index) => {
  return index === store.$state.index ? 'clickStyle' : '';
};

const exportCamera = async () => {
  let name = '相机管理';
  console.log(cameraInfo.option, 'option', cameraState.selectedRowKeys);
  if (!cameraInfo.option.cameraOwnership || cameraState.selectedRowKeys.length <= 0) {
    createMessage.warning('先在左侧选择设备，再选择表格数据进行导出');
    return;
  } else {
    const data = await exportXlsx(cameraInfo.option);
    if (!data) {
      createMessage.warning('文件下载失败');
      return;
    }
    let blobOptions = {type: 'application/vnd.ms-excel'};
    let fileSuffix = XLSX_FILE_SUFFIX;
    blobOptions['type'] = XLSX_MIME_TYPE;
    if (typeof window.navigator['msSaveBlob'] !== 'undefined') {
      window.navigator['msSaveBlob'](new Blob([data], blobOptions), name + fileSuffix);
    } else {
      let url = window.URL.createObjectURL(new Blob([data], blobOptions));
      let link = document.createElement('a');
      link.style.display = 'none';
      link.href = url;
      link.setAttribute('download', name + fileSuffix);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link); //下载完成移除元素
      window.URL.revokeObjectURL(url); //释放掉blob对象
    }
  }
};

const configAlg = async (record) => {
  //跳转编辑算法页面
  const algValus = await algList({belongTo: record.id});
  console.log(record, 'record');
  openModal(true, {
    algList: algValus.records,
    cameraName: record.cameraName,
    cameraId: record.id,
    isUpdate: false,
    showFooter: true,
    enableFaceAlgorithm: cameraState.enableFaceAlgorithm,
  })
};

const goAlg = () => {
  //跳转算法配置功能界面
  go('/base/algorithmmanager');
};

const reconnect = async (record) => {
  cameraInfo.loading = true;
  try {
    await reconnectCamera(record)
    refreshTable()
  } finally {
    cameraInfo.loading = false;
  }
}

const restartAiServer = async () => {
  await restartAi()
}

// 查看相机配置
const viewCameraConfig = async () => {
  const hostname = window.location.hostname;
  const port = window.location.port;
  const protocol = window.location.protocol;

  // 拼接IP地址和端口
  const urlPrefix = protocol + '//' + hostname + (port ? ':' + port : '');

  const fileUrl = urlPrefix + '/相机配置模板.pdf';
  window.open(fileUrl)
}

const faceConfirmModal = ref();
const handFaceAlgSwitch = (val: boolean)=> {
  faceConfirmModal.value.openConfirm(val)
}

const getFaceAlgSwitch = async () => {
  cameraState.enableFaceAlgorithm = await faceAlgSwitchStatus();
}

const setFaceGroupModal = ref()
const setFaceGroup = (record) => {
  console.log(record, 'record')
  setFaceGroupModal.value.open(record)
}

</script>

<style>
.popover {
  font-family: Source Han Sans CN;
  color: var(--secondary-text-color);
  font-size: 13px;
}

.tips {
  display: flex;
  height: 24px;
  align-items: center;
}

.tips-icon {
  width: 16px;
  height: 16px;
  background: url('/@/assets/svg/icon/info_icon_tips_red.svg');
  display: inline-block;
  margin: 0 4px 0 12px;
}

.text-tips {
  margin: 0 0 0 32px;
}

.action {
  display: flex;
  justify-content: flex-end;
}

.cancel {
  color: var(--icon-Color);
  font-size: 13px;
  width: 42px;
  height: 20px;
  font-family: Source Han Sans CN;
  background-color: transparent;
  display: flex;
  justify-content: center;
  cursor: pointer;
}

.cancel:hover {
  background-color: var(--fill-three-color);
  border-radius: 4px;
}

.delete {
  width: 42px;
  height: 20px;
  background-color: transparent;
  font-family: Source Han Sans CN;
  color: var(--serious-text-color);
  font-size: 13px;
}

.delete:hover {
  background-color: rgba(204, 102, 102, 0.2);
  border-radius: 4px;
}

.paragph {
  padding: 12px 0 0 32px;
  color: #7f8599;
  font-size: 13px;
}
</style>
<style lang="less" scoped>
.camera-main {
  height: calc(100vh - 72px);
  overflow-x: auto;
}

.camera-main-container {
  min-width: 1366px;

  .cameraLeft {
    min-width: 260px !important;
    width: 260px;
    max-width: 268px;
    background: var(--background-Color);
    height: calc(100vh - 70px);
    margin: 10px 0 10px 10px;
  }

  .cameraRight {
    margin: 10px 10px 10px 10px;
    background: var(--background-Color);
    height: calc(100vh - 70px);
    width: 100%;
    overflow-x: scroll;
  }

  .divider {
    margin: 4px 8px 0 8px;
  }

  .title {
    color: var(--tips-text-color);
    font-size: 13px;

    &-num {
      color: var(--tips-text-color);
      font-size: 13px;
      margin: 0 0 0 8px;
    }
  }

  .titleStyle {
    margin: 9px 0 0 20px;
    display: flex;
    align-items: center;
  }

  .seach-input {
    display: flex;
    justify-content: space-between;

    .seach-item {
      margin-left: 15px;
      width: 180px;
      border-radius: 4px;
    }
  }

  /deep/ .camera-icon {
    width: 16px;
    height: 18px;
    margin: 10px 8px;
  }

  .buttonStyle {
    display: flex;
    align-items: center;
    margin-top: 3px;

    .search-btn {
      width: 104px;
      height: 24px;
      background-color: var(--primary-Color);
      font-family: Source Han Sans CN;
      color: rgba(255, 255, 255, 0.95);
      font-size: 13px;
      border-radius: 4px;
      margin-left: 20px;
      cursor: pointer;
      display: flex;
      justify-content: center;
      align-items: center;
      font-size: 13px;

      &:hover {
        background-color: var(--primary-Color-hover);
      }

      &:active {
        background-color: var(--primary-Color-actived);
      }

      .search-icon {
        margin-right: 6px;
      }
    }
  }

  .delete-btn {
    margin-left: 8px;

    &:hover {
      .delete-icon {
        background-image: url('/@/assets/svg/icon/editor_btn_delete_hover.svg');
        background-repeat: no-repeat;
        background-position: center;
      }
    }
  }

  .import-btn {
    margin-left: 4px;
    margin-right: 12px;

    &:hover {
      .import-icon {
        background: url('/@/assets/svg/icon/editor_btn_import_hover.svg');
      }
    }
  }

  .export-btn {
    margin-right: 12px;

    &:hover {
      .export-icon {
        background: url('/@/assets/svg/icon/editor_btn_export_hover.svg');
      }
    }
  }

  .export-icon,
  .import-icon {
    width: 14px;
    height: 14px;
    display: inline-block;
  }

  .delete-icon {
    background: url('/@/assets/svg/icon/editor_btn_delete_default.svg');
    width: 20px;
    height: 20px;
    display: inline-block;
  }

  .import-icon {
    background: url('/@/assets/svg/icon/editor_btn_import_default.svg');
  }

  .export-icon {
    background: url('/@/assets/svg/icon/editor_btn_export_default.svg');
  }

  .camera-title {
    background: var(--background-Color);
    margin: 15px 12px 16px 2px;
    display: flex;
    font-weight: bold;
    align-items: center;
  }

  .camera-tab {
    background: #ffffff;
    margin: 12px 12px 16px 0;
    display: flex;
    width: 90px;
    align-items: center;
  }

  .camera-search {
    margin-top: 12px;

    .search .reset-btn {
      width: 64px;
      height: 24px;
      font-size: 13px;
      background-color: #e0e4ef;
      border-color: #e0e4ef;
      border-radius: 4px;
      margin-right: 0 !important;
    }
  }

  .tableTop {

    margin-top: 0;
    margin-left: 20px;
    padding-right: 20px;
    width: 100%;
    display: inline-flex;
    justify-content: space-between;
    align-items: center;

    .tableTopLeft {
      display: flex;
      align-items: center;
    }

    .tableTopRight {
      margin-right: 20px;
      display: inline-flex;
      align-items: center;

      .switch-item {
        display: inline-flex;
        align-items: center;
        margin-right: 15px;
      }
    }
  }

  .exam {
    margin-right: 10px;
    color: #4d66d8;
    text-decoration: underline;
  }

  :deep .ant-table-wrapper {
    min-width: 868px;
  }

  :deep .ant-table-header {
    height: 45px;
    display: flex;
    border-bottom: 1px solid var(--ash-text-color);
    border-top: 1px solid transparent;
    border-left: 1px solid transparent;
    border-right: 1px solid transparent;
    border-top-left-radius: 4px;
    border-top-right-radius: 4px;
  }

  :deep .ant-table-thead > tr > th {
    font-weight: 600;
    color: #606780;
    font-size: 13px;
    padding: 12px;
    background-color: var(--fill-four-color);
  }

  :deep .ant-table-tbody > tr:first-child > td {
    border-bottom: 1px solid var(--weak-split-line-color);
    font-size: 13px;
    cursor: pointer;
    padding: 0px !important;
  }

  :deep .ant-table-tbody > tr > td {
    border-bottom: 1px solid var(--weak-split-line-color);
    font-size: 13px;
    cursor: pointer;
    padding: 11px !important;
  }

  :deep .ant-table-tbody > tr.ant-table-row-selected > td {
    background-color: var(--stripe-color);
  }

  :deep .ant-table-header {
    height: 44px;
    background-color: var(--fill-three-color);
    border: 1px solid var(--ash-text-color);
    border-radius: 4pxs;
    text-align: center;
  }

  :deep .ant-table-thead > tr > th {
    font-weight: 600;
    color: #606780;
    font-size: 13px;
  }

  :deep .ant-table-tbody > tr > td {
    border-bottom: 1px solid var(--weak-split-line-color);
    font-size: 13px;
    cursor: pointer;
  }

  :deep .ant-table-tbody > tr:hover:not(.ant-table-expanded-row):not(.ant-table-row-selected) > td {
    background-color: var(--fill-four-color) !important;
  }

  :deep .ant-table-thead > tr > th:not(:last-child):not(.ant-table-selection-column):not(.ant-table-row-expand-icon-cell):not([colspan])::before {
    position: absolute;
    top: 50%;
    right: 0;
    width: 1px;
    height: 0;
    background-color: rgba(0, 0, 0, 0.06);
    transform: translateY(-50%);
    transition: background-color 0.3s;
    content: '';
  }

  :deep .ant-table-column-sorter-up.active,
  :deep .ant-table-column-sorter-down.active {
    color: #7089ed;
  }

  :deep .ant-checkbox-indeterminate .ant-checkbox-inner {
    background-color: #7089ed;
    border-color: #7089ed;

    &::after {
      width: 6px;
      height: 1.5px;
      background-color: #ffffff;
      border-radius: 0.4px;
    }
  }

  // :deep .ant-pagination {
  //   height: 28px;
  //   position: fixed;
  //   bottom: 44px;
  //   width: 50%;
  //   z-index: 333;
  //   background-color: #fff;
  //   text-align: right;
  //   height: 28px;
  //   right: 42px;
  // }
  :deep .ant-pagination-item-active {
    border: none;
  }

  :deep .ant-pagination-item {
    width: 36px;
    height: 28px;
    border-radius: 4px;
    line-height: 28px;
    min-width: 28px;

    &:hover {
      border-color: #738ce6;
    }
  }

  :deep .ant-pagination-item-active a {
    width: 36px;
    height: 28px;
    background-color: #738ce6;
    border-color: #738ce6;
    border-radius: 4px;
    color: #ffffff;
  }

  :deep .ant-pagination-prev .ant-pagination-item-link,
  :deep .ant-pagination-next .ant-pagination-item-link {
    width: 28px;
    height: 28px;
    padding: 0;
    font-size: 12px;
    min-width: 28px;
  }

  :deep .ant-pagination-prev,
  :deep .ant-pagination-next,
  :deep .ant-pagination-jump-prev,
  :deep .ant-pagination-jump-next,
  :deep .ant-pagination-jump-prev {
    display: inline-block;
    min-width: 28px;
    height: 28px;
  }

  :deep .ant-pagination-prev:hover .ant-pagination-item-link,
  :deep .ant-pagination-next:hover .ant-pagination-item-link {
    &:hover {
      color: #738ce6;
      border-color: #738ce6;
    }
  }

  :deep .ant-pagination-options {
    height: 28px;
  }

  :deep .ant-pagination-options-quick-jumper {
    height: 28px;
    line-height: 28px;
  }

  :deep .ant-select-focused:not(.ant-select-disabled).ant-select:not(.ant-select-customize-input) .ant-select-selector {
    background-color: #ffffff;
    border: 1px solid;
    border-color: #738ce6;
    box-shadow: none;
    border-right-width: 1px !important;
  }

  :deep .ant-select:not(.ant-select-disabled):hover .ant-select-selector {
    background-color: #ffffff;
    border: 1px solid;
    border-color: #adb4cc;
    border-radius: 4px;
  }

  :deep .ant-select-item-option-selected:not(.ant-select-item-option-disabled) {
    background-color: rgba(115, 140, 230, 0.2);
    width: 86px;
    height: 28px;
    height: 28px;
    justify-content: center;
    border-radius: 4px;
  }

  :deep .rc-virtual-list-holder-inner {
    align-items: center;
  }

  :deep .ant-select-item-option-active:not(.ant-select-item-option-disabled) {
    background-color: #f5f5f5;
    width: 86px;
    height: 28px;
    background-color: var(--netural-color);
    border-radius: 4px;
  }

  :deep .ant-checkbox-checked .ant-checkbox-inner {
    background-color: #738ce6;
    border-color: #738ce6;
  }

  :deep .ant-checkbox-wrapper:hover .ant-checkbox-inner,
  :deep .ant-checkbox:hover .ant-checkbox-inner,
  :deep .ant-checkbox-input:focus + .ant-checkbox-inner {
    border-color: #738ce6;
  }

  .setting {
    font-family: Source Han Sans CN;
    color: var(--primary-Color);
    font-size: 13px;
    width: 68px;
    height: 20px;
    background-color: transparent;

    &:hover {
      background-color: rgba(133, 159, 242, 0.2);
      border-radius: 4px;
    }
  }

  .management,
  .edit {
    font-family: Source Han Sans CN;
    color: var(--icon-Color);
    font-size: 13px;

    height: 20px;
    background-color: transparent;

    &:hover {
      background-color: var(--fill-three-color);
      border-radius: 4px;
    }
  }

  .management {
    width: 68px;
  }

  .edit {
    width: 42px;
  }

  .delete {
    width: 42px;
    height: 20px;
    background-color: transparent;
    font-family: Source Han Sans CN;
    color: var(--serious-text-color);
    font-size: 13px;

    &:hover {
      background-color: rgba(204, 102, 102, 0.2);
      border-radius: 4px;
    }
  }

  /deep/ .ant-pagination span li {
    font-size: 13px;
  }

  /deep/ .ant-pagination-options-quick-jumper {
    font-size: 13px;
  }

  .redPointOffline {
    display: inline-block;
    text-align: center;
    font-size: 0.1em;
    color: #fff;
    background: var(--serious-text-color);
    width: 8px;
    height: 8px;
    border-radius: 8px;
  }

  .redPointOnline {
    display: inline-block;
    text-align: center;
    font-size: 0.1em;
    color: #fff;
    background: #7fc1cc;
    width: 8px;
    height: 8px;
    border-radius: 8px;
  }

  .offline-icon {
    background: url('/@/assets/svg/icon/icon_offline.svg');
    display: inline-block;
    width: 12px;
    height: 12px;
  }

  .tooltip {
    display: inline-block;
  }
}
</style>
