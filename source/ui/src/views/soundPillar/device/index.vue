<template>
  <div style="margin: 15px">
    <a-card title="获取音柱" :bordered="false" style="width: 100%">
      <a-form
        ref="formRef"
        :model="formState"
        :label-col="{span: 5}"
        :wrapper-col="{span: 18}"
        autocomplete="off">
        <a-row>
          <!--          <a-col :span="6">
                      <a-form-item label="账 号" name="account" :rules="rulesState.account">
                        <a-input v-model:value="formState.account" placeholder="请输入管理员给的账号"/>
                      </a-form-item>
                    </a-col>
                    <a-col :span="6">
                      <a-form-item label="密 码" name="password" :rules="rulesState.password">
                        <a-input type="password" v-model:value="formState.password"
                                 placeholder="请输入管理员给的密码"/>
                      </a-form-item>
                    </a-col>-->
          <a-col :span="6">
            <a-form-item label="项目名称" name="soundPillarDir" :rules="rulesState.soundPillarDir">
              <a-input v-model:value="formState.soundPillarDir" placeholder="请输入项目名称"/>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-button :loading="syncBtnLoad" type="primary" @click="getDeviceList"
                      style="margin-right: 10px">
              同步音柱信息
            </a-button>
            <a-button type="danger" @click="resetForm">重置输入信息</a-button>
          </a-col>
        </a-row>
      </a-form>
    </a-card>
    <a-card title="音柱列表" :bordered="false" style="width: 100%; margin-top: 10px">
      <BasicTable @register="registerTable">
        <!--操作栏-->
        <template #action="{ record }">
          <TableAction :actions="getTableAction(record)"/>
        </template>
        <template #enable="{record}">
          <a-tag color="success" v-if="record.enable" @click="changeDeviceStatus(0, record.id)"
                 style="cursor: pointer">启用
          </a-tag>
          <a-tag color="default" v-else @click="changeDeviceStatus(1, record.id)"
                 style="cursor: pointer">
            禁用
          </a-tag>
        </template>
        <template #appState="{record}">
          <a-tag v-if="record.appState === 'offline'">离线</a-tag>
          <a-tag color="success" v-if="record.appState === 'online'">在线</a-tag>
        </template>
      </BasicTable>
    </a-card>
    <basic-modal
      v-bind="$attrs"
      @register="register"
      title="修改设备信息"
      @ok="saveDeviceInfo">
      <a-form
        :model="deviceInfo"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 15 }"
        autocomplete="off">
        <a-form-item label="音柱名称" name="Name">
          <a-input v-model:value="deviceInfo.Name" placeholder="请输入音柱名称"/>
        </a-form-item>
        <a-form-item label="联系人" name="Linkman">
          <a-input v-model:value="deviceInfo.Linkman" placeholder="请输入联系人"/>
        </a-form-item>
        <a-form-item label="联系人手机号码" name="PhoneNum">
          <a-input v-model:value="deviceInfo.PhoneNum" placeholder="联系人手机号码"/>
        </a-form-item>
        <a-form-item label="地址" name="SetupLocation">
          <a-input v-model:value="deviceInfo.SetupLocation" placeholder="所在地址"/>
        </a-form-item>
        <a-form-item label="备注" name="Remark">
          <a-input v-model:value="deviceInfo.Remark" placeholder="备注信息"/>
        </a-form-item>
      </a-form>
    </basic-modal>
    <basic-modal
      v-bind="$attrs"
      @register="register1"
      okText="播放音频"
      cancelText="关闭弹窗"
      title="测试音柱"
      :showCancelBtn="false"
      @ok="testAudioFile">
      <a-form
        :model="currentRecord"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 15 }"
        autocomplete="off">
        <a-form-item label="设备名称">
          <strong>{{ currentRecord.name }}</strong>
        </a-form-item>
        <a-form-item label="设备id">
          <strong>{{ currentRecord.deviceId }}</strong>
        </a-form-item>
        <a-form-item label="音频文件">
          <a-select placeholder="请选择一个音频文件" v-model:value="soundFileId">
            <a-select-option v-for="item in soundFiles" :key="item.id" :value="item.audioId">
              {{ item.displayName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="设置音量">
          <a-slider v-model:value="volume" :max="100" :min="1" :step="1"></a-slider>
        </a-form-item>
      </a-form>
      <template #appendFooter>
        <a-button type="primary" @click="closeAudio" :loading="closeAudioBtnLoading">关闭音频
        </a-button>
        <a-button type="primary" @click="getVolume" :loading="getVolumeBtnLoading">获取音量
        </a-button>
        <a-button type="primary" @click="setVolume" :loading="setVolumeBtnLoading">设置音量
        </a-button>
      </template>
    </basic-modal>

    <basic-modal
      v-bind="$attrs"
      @register="registerCamera"
      okText="确定"
      cancelText="关闭弹窗"
      title="绑定相机"
      :showCancelBtn="false"
      @ok="bindCamera">
      <BasicTable @register="registerCameraTable" :row-selection="{
        onSelect: tableSelect,
        onSelectAll: onSelectAll,
        type: 'checkbox',
        selectedRowKeys: selectedRowKeys
      }"></BasicTable>
    </basic-modal>
  </div>

</template>

<script setup>
import {onMounted, onUnmounted, reactive, ref} from 'vue';
import {useScript} from "/@/hooks/web/useScript";
import {BasicModal, useModalInner} from '/@/components/Modal';
import {BasicTable, TableAction} from '/@/components/Table';
import {useListPage} from '/@/hooks/system/useListPage';
import {message} from "ant-design-vue";
import {useMessage} from '/@/hooks/web/useMessage';

import {
  closePlayTask,
  getAccountInfo, getProjectInfo,
  listPage,
  sycDeviceInfo,
  updateSoundPillar,
  updateStatus,
  updateVolume
} from './SoundDeviceInfo.api'
import {audioList, deleteRecord, playAudioFile} from "/@/views/soundPillar/voice/AudioFile.api";
import {list} from "/@/views/base/cameraManager/CameraManager.api";

// 首先加载 sdk
const {success: sosLoaded} = useScript({src: "/sos/sos-v1.0.15.js"})
const {success: encryptLoaded} = useScript({src: "/sos/jsencrypt.min.js"})
let sdk = null;

// ==== 表格 ====
const columns = [
  {
    title: '音柱名称',
    align: 'center',
    dataIndex: 'name'
  },
  {
    title: '设备id',
    align: 'center',
    dataIndex: 'deviceId'
  },
  {
    title: '音量',
    align: 'center',
    dataIndex: 'volume'
  },
  // {
  //   title: '设备序列号',
  //   align: 'center',
  //   dataIndex: 'serialNum'
  // },
  // {
  //   title: '证书ID',
  //   align: 'center',
  //   dataIndex: 'licenseId'
  // },
  {
    title: '在线时间',
    align: 'center',
    dataIndex: 'onlineTime'
  },
  {
    title: '在线状态',
    align: 'center',
    dataIndex: 'appState',
    slots: {customRender: 'appState'}
  },
  {
    title: '信号强度',
    align: 'center',
    dataIndex: 'signalStrength'
  },
  {
    title: '状态',
    align: 'center',
    dataIndex: 'enable',
    slots: {customRender: 'enable'}
  }
];

const {tableContext} = useListPage({
  tableProps: {
    title: '输入管理员给的项目名称，同步该项目下的音柱',
    api: listPage,
    columns: columns,
    striped: true,
    useSearchForm: false,
    actionColumn: {
      width: 250,
    }
  },
});

// BasicTable绑定注册
const [registerTable, {reload, setLoading}] = tableContext;

function getTableAction(record) {
  return [
    {
      label: '编辑',
      onClick: handleEdit.bind(null, record),
    },
    {
      label: '调试',
      onClick: handleTest.bind(null, record),
    },
    {
      label: '绑定相机',
      onClick: bindCameraModal.bind(null, record),
    },
    {
      label: '删除',
      color: 'error',
      onClick: handleDelete.bind(null, record),
    }
  ];
}

//自定义表单字段
const deviceInfo = reactive({})
const [register, {closeModal, setModalProps}] = useModalInner();

// 编辑
function handleEdit(record) {
  if (!checkInitSdk()) return
  setLoading(true)
  // 获取配置信息
  setTimeout(async () => {
    try {
      const rsp = await sdk.deviceBaseInfoGet({
        DeviceId: record.deviceId
      });
      Object.assign(deviceInfo, rsp)
      message.success('获取设备信息成功')
      setModalProps({visible: true})
    } catch (error) {
      message.error('获取设备信息失败: ' + error)
      console.log('获取设备信息失败', error)
    } finally {
      setLoading(false)
    }
  }, 2000)
}

// 测试弹窗
let soundFiles = ref([])
const currentRecord = ref({})
const [register1, {closeModal: closeModal1, setModalProps: setModalProps1}] = useModalInner();

async function handleTest(record) {
  currentRecord.value = record
  const audioFiles = await audioList()
  if (!audioFiles || audioFiles.length === 0)
    message.warn("没有可用的音频文件")
  soundFiles.length = 0
  soundFiles.value = audioFiles
  console.log(soundFiles)
  setModalProps1({visible: true})
}

const {createConfirm} = useMessage()

const handleDelete = async (record)=> {
  createConfirm({
    iconType: 'warning',
    title: '提示信息',
    content: '确定删除该音柱？',
    onOk: async () => {
      await deleteRecord({id: record.id})
      await reload()
      message.success("删除成功")
    },
    onCancel: () => console.log('取消'),
  });
}

// 绑定相机
const [registerCamera, {closeModal: closeCameraModal, setModalProps: setCameraModalProps}] = useModalInner();
const cameraColumns = [
  {
    title: '相机名称',
    align: 'center',
    dataIndex: 'cameraName'
  },
  {
    title: '相机IP',
    align: 'center',
    dataIndex: 'ip'
  },
  {
    title: '相机品牌',
    align: 'center',
    dataIndex: 'factory_dictText'
  }
];
const {tableContext: cameraTableContext} = useListPage({
  tableProps: {
    size: "small",
    api: list,
    columns: cameraColumns,
    striped: true,
    showActionColumn: false,
    showTableSetting: false,
    useSearchForm: false,
    rowKey: 'id'
  },
})
const [registerCameraTable, {reload: cameraTableReload}, {rowSelection, selectedRowKeys}] = cameraTableContext;
const bindCameraModal = async (record) => {
  selectedRowKeys.value= []
  currentRecord.value = record
  if (record.cameraList) {
    console.log(record.cameraList)
    selectedRowKeys.value = JSON.parse(record.cameraList)
  }
  setCameraModalProps({
    visible: true,
    width: 800
  })
}

const tableSelect = (record, selected) => {
  if (selected) {
    selectedRowKeys.value.push(record.id)
  } else {
    selectedRowKeys.value.map((currentItem, index) => {
      if (currentItem === record.id) {
        selectedRowKeys.value.splice(index, 1);
      }
    })
  }
}

const onSelectAll = (selected, selectedRows, changeRows) => {
  if (selected) {
    // 如果全选，将 changeRows 的 id 添加到 selectedRowKeys，并确保唯一性
    const currentKeys = new Set(selectedRowKeys.value);
    changeRows.forEach((x) => currentKeys.add(x.id));
    selectedRowKeys.value = Array.from(currentKeys);
  } else {
    // 如果取消全选，移除 changeRows 中对应的 id
    const changeRowIds = new Set(changeRows.map((x) => x.id));
    selectedRowKeys.value = selectedRowKeys.value.filter(
      (key) => !changeRowIds.has(key)
    );
  }
};

const bindCamera = async () => {
  const selectedRow = selectedRowKeys.value
  // if (!selectedRow || selectedRow.length === 0) {
  //   message.error("请选择一个相机")
  //   return
  // }
  const params = {
    id: currentRecord.value.id,
    cameraList: JSON.stringify(selectedRow)
  }
  await updateSoundPillar(params)
  await reload()
  closeCameraModal()
}

// 测试音频
const soundFileId = ref()
const taskCodeRef = ref(null)

async function testAudioFile() {
  if (!soundFileId.value) {
    message.error("请选择一个音频文件")
    return
  }
  // console.log('音频id:', soundFileId.value, '音柱id:', currentRecord.value.deviceId)
  const params = {
    audioId: soundFileId.value,
    soundDeviceId: currentRecord.value.deviceId
  }
  const resp = await playAudioFile(params)
  if (resp) taskCodeRef.value = resp.taskCode
  message.success("播放成功")
}

// 关闭音频播放任务
const closeAudioBtnLoading = ref(false)
async function closeAudio() {
  closeAudioBtnLoading.value = true
  if (taskCodeRef.value)
    await closePlayTask({taskCode: taskCodeRef.value});
  message.success("关闭成功")
  closeAudioBtnLoading.value = false
}

// 获取音柱音量
const getVolumeBtnLoading = ref(false)

async function getVolume() {
  const deviceId = currentRecord.value.deviceId;
  if (!deviceId) return
  if (!checkInitSdk()) {
    closeModal1()
    return
  }
  getVolumeBtnLoading.value = true
  setTimeout(async () => {
    try {
      const rsp = await sdk.deviceVolumeGet({
        DeviceId: deviceId,
      });
      console.log('获取设备音量成功', '扬声器音量：', rsp.SpeakerVolume, '麦克风灵敏度：', rsp.MicrophoneVolume)
      message.success("当前扬声器音量：" + rsp.SpeakerVolume)
      volume.value = rsp.SpeakerVolume
      await updateVolume({deviceId: deviceId, volume: volume.value})
      await reload()
    } catch (error) {
      console.log('获取设备音量失败', error)
      message.error("获取设备音量失败，请稍后重试")
    }
    getVolumeBtnLoading.value = false
  }, 2000)
}

// 设置音柱音量
const volume = ref(1)
const setVolumeBtnLoading = ref(false)

async function setVolume() {
  const deviceId = currentRecord.value.deviceId;
  const checked = checkInitSdk();
  if (!checked) {
    closeModal1()
    return
  }
  setVolumeBtnLoading.value = true
  console.log('设备id', deviceId, '音量：', volume.value)
  try {
    const rsp = await sdk.deviceVolumeSet({
      DeviceId: deviceId,
      SpeakerVolume: volume.value,
    });
    console.log('设置设备音量成功')
    message.success('设置设备音量成功')
    await updateVolume({deviceId: deviceId, volume: volume.value})
    await reload()
  } catch (error) {
    console.log('设置设备音量失败', error)
    message.error('设置设备音量失败: ' + error)
  }
  setVolumeBtnLoading.value = false
}

// 检查 sdk 初始化
function checkInitSdk() {
  if (!sosLoaded.value || !encryptLoaded.value) {
    message.error("SDK JS 加载失败")
    return false;
  }
  if (!formState.account || !formState.password) {
    message.error("请输入设备管理账号密码")
    return false;
  }
  if (!sdk) initSdk(formState.account, formState.password, false)
  return true
}

async function saveDeviceInfo() {
  try {
    const rsp = await sdk.deviceBaseInfoMod({
      DeviceId: deviceInfo.DeviceId,
      Name: deviceInfo.Name, // 需要修改此字段就填此字段，不修改，就不填
      Linkman: deviceInfo.Linkman, // 需要修改此字段就填此字段，不修改，就不填
      PhoneNum: deviceInfo.PhoneNum, // 需要修改此字段就填此字段，不修改，就不填
      SetupLocation: deviceInfo.SetupLocation, // 需要修改此字段就填此字段，不修改，就不填
      Remark: deviceInfo.Remark, // 需要修改此字段就填此字段，不修改，就不填
    });
    if (rsp.HttpStatus === 200 && rsp.Status === 0) {
      message.success('修改设备信息成功')
      setModalProps({visible: false})
      await sycDeviceInfo()
    } else {
      message.error('修改设备信息失败')
      console.log('修改设备信息失败', rsp)
    }
  } catch (error) {
    message.error('修改设备信息失败')
    console.log('修改设备信息失败', error)
  }

}

function changeDeviceStatus(status, id) {
  const contentStr = status === 0 ? '确定禁用该音柱？' : '确定启用该音柱？'
  createConfirm({
    iconType: 'warning',
    title: '提示信息',
    content: contentStr,
    onOk: () => updateDeviceStatus(status, id),
    onCancel: () => console.log('取消'),
  });
}

async function updateDeviceStatus(status, id) {
  await updateStatus({status, id})
  await reload();
}

// ==== 表格结束 ====

// ==== 表单 ====
const syncBtnLoad = ref(false)
const formState = reactive({
  account: 'b9c3b79c467c4983a3705dc702dab385',
  password: '530c08a037524b1b9ac724c3c12e8074',
  soundPillarDir: null
})

const rulesState = {
  account: [{required: true, message: '请输入管理员给的账号'}, {
    max: 15,
    message: '长度不能超过15'
  }],
  password: [{required: true, message: '请输入管理员给的密码'}, {
    max: 50,
    message: '长度不能超过50'
  }],
  soundPillarDir: [{required: false, message: '请输入项目名称'}, {
    max: 50,
    message: '长度不能超过50'
  }],
}

function resetForm() {
  formState.soundPillarDir  = null
  sdk && sdk.destroy();
  sdk = null;
}

// 获取所有设备列表
async function getAllDevices() {
  syncBtnLoad.value = true
  if (sdk) {
    try {
      const response = await sdk.deviceList({
        PageSize: 50,
        PageNum: 1,
        Filters: [
          [{Key: "DeviceType", Type: "str", Op: "=", Value: "alarm"}],
        ],
        OrderKey: "OnlineTime",
        Desc: true,
      });
      if (response.Status === 0) {
        const records = response.RecordList;
        if (records && records.length > 0) {
          message.success("获取音柱信息成功，正在同步到本地...")
          await sycPillarList(records)
        }
      } else {
        message.error(response.StatusMsg)
      }
    } catch (error) {
      message.error(error)
    }
  } else {
    message.error("获取设备列表失败，请先启动SDK")
  }
  syncBtnLoad.value = false
}

async function sycPillarList(list) {
  const listParams = list.map(item => {
    return {
      deviceId: item.Id,
      name: item.Name,
      serialNum: item.SerialNum,
      licenseId: item.LicenseId,
      onlineTime: item.OnlineTime,
      appState: item.AppState,
      signalStrength: item.SignalStrength,
    }
  })
  await sycDeviceInfo(listParams)
  message.success("同步音柱到本地成功")
  await reload()
}

const formRef = ref()

async function getDeviceList() {
  syncBtnLoad.value = true
  await sycDeviceInfo( { soundPillarDir: formState.soundPillarDir })
  await reload()
  syncBtnLoad.value = false
}

// ==== 表单结束 ====

// 初始化 sos sdk
function initSdk(account, password, isSync) {
  sdk = new SOS.SDK({
    Account: account,
    Password: password,
  });
  // SDK运行状态事件通知
  sdk.addEventListener(SOS.EVENT.SDK_STATE, (event, code, description) => {
    switch (event) {
      case SOS.EVENT.SDK_STATE_CODE.WARN:
        // 一般是网络原因与服务器连接断开，会warn
        console.warn("sdk state: warn", code, description);
        message.warn("网络原因与服务器连接断开")
        break;

      case SOS.EVENT.SDK_STATE_CODE.ERROR:
        console.error("sdk state: error", code, description);
        if (code === SOS.ERROR_CODE.LOGIN_CONFLICT) {
          // 账号在其他地方登录，这个需要处理下
          console.log("你的账号已在其他地方登录，你可以再初始化一次，把对方挤下来")
          message.warn("你的账号已在其他地方登录，你可以再初始化一次，把对方挤下来")
          // 这里一定要记得把sdk destroy掉，不然会触发sdk的重连机制
          sdk && sdk.destroy();
          sdk = null;
        }
        break;

      case SOS.EVENT.SDK_STATE_CODE.SUCCESS:
        console.info("sdk state: success");
        message.info("sdk 初始化成功")
        // if (isSync) getAllDevices()
        break;

      default:
        break;
    }
  })

  sdk.init().then(() => {
    // 初始化成功（这里成功之后，也会通过sdk.addEventListener(SOS.EVENT.SDK_STATE)的消息通知到）
    console.log('SDK启动成功，版本号：', sdk.version());
  }).catch(error => {
    console.error(error);
  })
}

// 销毁sos sdk
function closeSdk() {
  if (sdk) sdk.destroy();
}

// 网页卸载
onUnmounted(() => {
  closeSdk();
})


onMounted(async () => {
  const accountInfo = await getAccountInfo()
  console.log('accountInfo', accountInfo)
  if (accountInfo.AudioPassword && accountInfo.AudioPassword) {
    formState.account = accountInfo.AudioAccount
    formState.password = accountInfo.AudioPassword
    // initSdk(accountInfo.account, accountInfo.password, true)
  }

  const projectInfo = await getProjectInfo()
  if (projectInfo.SoundPillarDir) {
    formState.soundPillarDir = projectInfo.SoundPillarDir
  }
})

</script>

<style scoped lang="less">

</style>
