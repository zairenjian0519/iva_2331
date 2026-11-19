<template>
  <div class="sound-container">
    <a-card title="串口配置" :bordered="false" style="width: 100%">
      <a-form
        ref="formRef"
        :model="formState"
        :label-col="{span: 8}"
        :wrapper-col="{span: 16}"
        autocomplete="off">
        <a-row>
          <a-col :span="6">
            <a-form-item label="串口标签" name="soundName" :rules="rulesState.soundName">
              <a-input v-model:value="formState.soundName" placeholder="请确定一个串口标签"/>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="串口号" name="serialName" :rules="rulesState.serialName">
              <a-select v-model:value="formState.serialName" placeholder="请选择串口号"
                        :options="serialArr"/>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="波特率" name="baudRate" :rules="rulesState.baudRate">
              <a-select v-model:value="formState.baudRate" :options=baudRateArr
                        placeholder="请选择波特率"/>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="数据位" name="dataBits" :rules="rulesState.dataBits">
              <a-select v-model:value="formState.dataBits" :options="dataBitsArr"
                        placeholder="请选择数据位"/>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row>
          <a-col :span="6">
            <a-form-item label="停止位" name="stopBits" :rules="rulesState.stopBits">
              <a-select v-model:value="formState.stopBits" :options="stopBitsArr"
                        placeholder="请选择停止位"/>
            </a-form-item>
          </a-col>

          <a-col :span="6">
            <a-form-item label="校验位" name="parityBit" :rules="rulesState.parityBit">
              <a-select v-model:value="formState.parityBit" :options="parityBitArr"
                        placeholder="请选择校验位"/>
            </a-form-item>
          </a-col>

          <a-col :span="6">
            <a-form-item label="指令选择" name="voiceFile" :rules="rulesState.voiceFile">
              <a-select v-model:value="formState.voiceFile" :options="voiceFileArr"
                        placeholder="请选择串口指令" @change="voiceChange"/>
            </a-form-item>
          </a-col>

          <a-col :span="6">
            <a-form-item label="目标算法" name="algorithm" :rules="rulesState.algorithm">
              <a-select v-model:value="formState.algorithm" :options="algorithmArr"
                        placeholder="请选择需要报警的算法"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="24">
            <div style="float: right">
              <a-button type="primary" @click="refreshSelectData">刷新串口列表</a-button>
              <a-button type="primary" style="margin-left: 10px" @click="testSoundPillar"
                        :loading="voiceTestBtnLoad">执行开启指令
              </a-button>
              <a-button type="primary" style="margin-left: 10px" @click="closeSoundPillar"
                        :loading="voiceCloseBtnLoad">
                执行关闭指令
              </a-button>
              <a-button type="primary" style="margin-left: 10px" @click="saveData">保存配置
              </a-button>
            </div>
          </a-col>
        </a-row>
      </a-form>
    </a-card>
    <a-card title="配置列表" :bordered="false" style="margin-top: 10px"
            :body-style="{margin: 0, padding: 0}">
      <BasicTable @register="registerTable">
        <template #action="{ record }">
          <TableAction :actions="getTableAction(record)"/>
        </template>
        <template #voiceFile="{record}">
          {{ formatVoice(record['voiceFileId']) }}
        </template>
        <template #algorithm="{record}">
          {{ formatAlgorithm(record['algorithm']) }}
        </template>
        <template #serialPortStatus="{record}">
          <a-tag color="success" v-if="record['serialPortStatus']==1">在线</a-tag>
          <a-tag color="default" v-if="record['serialPortStatus']==0">离线</a-tag>
        </template>
      </BasicTable>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {
  checkSoundPillar,
  closeVoice,
  deleteRecord,
  getAlgorithmList,
  getSoundPillarVoices,
  getSystemSerialArr,
  listPage,
  saveSettings
} from "/@/views/serial485/index";
import {computed, onMounted, onUnmounted, reactive, ref, unref} from "vue";
import {ActionItem, BasicColumn, BasicTable, TableAction} from '/@/components/Table';
import {useListPage} from '/@/hooks/system/useListPage';
import {useMessage} from "/@/hooks/web/useMessage";
import {connectWebSocket, offWebSocket, onWebSocket} from "/@/hooks/web/useWebSocketnew";
import {useUserStore} from "/@/store/modules/user";
import {lowerCase} from "lodash-es";

const {createConfirm, createMessage} = useMessage()

//波特率常见值
const baudRateArr = [
  {label: 9600, value: 9600},
  {label: 19200, value: 19200},
  {label: 38400, value: 38400},
  {label: 57600, value: 57600},
  {label: 115200, value: 115200},
  {label: 230400, value: 230400},
  {label: 460800, value: 460800},
  {label: 921600, value: 921600}
]

//数据位常见值
const dataBitsArr = [
  {label: '5位', value: 5},
  {label: '6位', value: 6},
  {label: '7位', value: 7},
  {label: '8位', value: 8}
]

//停止位常见值
const stopBitsArr = [
  {label: '1位', value: 1},
  {label: '1.5位', value: 1.5},
  {label: '2位', value: 2}
]

//校验位常见值
const parityBitArr = [
  {label: 'NONE', value: 0},
  {label: 'ODD', value: 1},
  {label: 'EVEN', value: 2},
  {label: 'MARK', value: 3},
  {label: 'SPACE', value: 4}
]

const rulesState = {
  soundName: [{required: true, message: '请输入一个音柱标签'}, {
    max: 15,
    message: '长度不能超过15'
  }],
  serialName: [{required: true, message: '串口号'}],
  baudRate: [{required: true, message: '请选择波特率'}],
  dataBits: [{required: true, message: '请选择数据位'}],
  stopBits: [{required: true, message: '请选择停止位'}],
  parityBit: [{required: true, message: '请选择校验位'}],
  voiceFile: [{required: true, message: '请选择音频文件'}],
  algorithm: [{required: true, message: '请选择需要报警的算法'}],
}

const formState = reactive({
  soundName: undefined,
  serialName: undefined,
  baudRate: 9600,
  dataBits: 8,
  stopBits: 1,
  parityBit: 0,
  voiceFile: undefined,
  algorithm: undefined
})

// 串口状态
let serialPortStatus = 0

// 串口表id
let serialPortId = null

// 音频id
let voiceFileId = null

// 串口音频绑定id
let serialPortVoiceId = null

//获取串口方法
//串口数组
const serialArr = reactive([])

async function getSerialArr() {
  const serials = await getSystemSerialArr()
  if (serials && serials.length > 0) {
    serialArr.length = 0;
    for (const serialPort of serials) {
      serialArr.push({label: serialPort, value: serialPort})
    }
  }
}

//获取算法列表
const algorithmArr = reactive([]);

async function getAlgorithmArr() {
  const algorithmList = await getAlgorithmList()
  if (algorithmList && algorithmList.length > 0) {
    algorithmArr.length = 0
    for (const item of algorithmList) {
      algorithmArr.push({label: item['name'], value: item['code']})
    }
  }
}

// 刷新选择框数据
function refreshSelectData() {
  getSerialArr();
  getAlgorithmArr();
  getVoices();
}

//音柱音频配置列表获取
const voiceFileArr = reactive([])
let soundPillarVoices = []

async function getVoices() {
  const resultArr = await getSoundPillarVoices()
  if (resultArr && resultArr.length > 0) {
    voiceFileArr.length = 0
    soundPillarVoices.length = 0
    soundPillarVoices = resultArr
    soundPillarVoices.forEach(item => {
      voiceFileArr.push({
        label: item['voiceName'],
        value: item['openCode'],
        voiceFileId: item['id']
      })
    })
  }
}

const formRef = ref()

//测试音柱声音
let playResponseCode = null
const voiceTestBtnLoad = ref(false)

async function testSoundPillar() {
  try {
    await formRef.value.validateFields();
    voiceTestBtnLoad.value = true
    await checkSoundPillar(formState)
    createMessage.success("串口" + formState.serialName + "通讯成功，请确认音柱是否有声音")
    const target = soundPillarVoices.find(item => item['id'] == voiceFileId)
    if (target) playResponseCode = target['openCodeResponse'];
    console.log('指令发送成功，确认码: ', playResponseCode)
    // 5秒后如果指令还没有成功返回，证明串口通讯失败
    setTimeout(() => {
      if (voiceTestBtnLoad.value) {
        createMessage.error("未接收到音柱响应码，音频播放失败，串口号：" + formState.serialName)
        voiceTestBtnLoad.value = false
      }
    }, 5000)
  } catch (errorInfo) {
    console.log('Failed:', errorInfo);
  }
}

// 关闭音频
const voiceCloseBtnLoad = ref(false)
let closeResponseCode = null;

async function closeSoundPillar() {
  const targetItem = soundPillarVoices.find(item => item['id'] == voiceFileId)
  if (!targetItem) return
  voiceCloseBtnLoad.value = true
  const closeCode = targetItem['closeCode']
  console.log('音频关闭指令码：', closeCode)
  const params = {serialName: formState.serialName, closeCode: closeCode}
  await closeVoice(params)
  createMessage.success("串口" + formState.serialName + "通讯成功，请确认音柱是否关闭。如果未关闭，请插拔电源重试")
  const target = soundPillarVoices.find(item => item['id'] == voiceFileId)
  if (target) closeResponseCode = target['closeCodeResponse'];
  console.log('关闭指令发送成功，确认码: ', closeResponseCode)
  // 5秒后如果指令还没有成功返回，证明串口通讯失败
  setTimeout(() => {
    if (voiceCloseBtnLoad.value) {
      createMessage.error("未接收到音柱响应码，音频关闭失败，串口号：" + formState.serialName)
      voiceCloseBtnLoad.value = false
    }
  }, 5000)
}

// 音频选择框change
const voiceChange = (val, item) => {
  voiceFileId = item.voiceFileId
}

//表格
const columns: BasicColumn[] = [
  {
    title: '设备标签名',
    align: 'center',
    dataIndex: 'soundName'
  }, {
    title: '对应串口号',
    align: 'center',
    dataIndex: 'serialName',
  }, {
    title: '指令标识',
    align: 'center',
    dataIndex: 'voiceFile',
    slots: {customRender: 'voiceFile'},
  }, {
    title: '目标算法',
    align: 'center',
    dataIndex: 'algorithm',
    slots: {customRender: 'algorithm'},
  }, {
    title: '串口状态',
    align: 'center',
    dataIndex: 'serialPortStatus',
    slots: {customRender: 'serialPortStatus'},
  }
]

// 列表页面公共参数、方法
const {tableContext} = useListPage({
  designScope: 'serial-voice-table',
  tableProps: {
    api: listPage,
    columns: columns,
    striped: true,
    useSearchForm: false,
    actionColumn: {
      width: 120,
    },
  },
});

//BasicTable绑定注册
const [registerTable, {reload}] = tableContext;

/**
 * 操作栏
 */
function getTableAction(record): ActionItem[] {
  return [
    {
      label: '编辑',
      onClick: handleEdit.bind(null, record),
    },
    {
      label: '删除',
      onClick: handleDelete.bind(null, record)
    }
  ];
}

function handleEdit(record) {
  Object.assign(formState, record)
  formState.algorithm = '' + formState.algorithm
  voiceFileId = record['voiceFileId']
  serialPortId = record['serialPortId']
  serialPortVoiceId = record['serialPortVoiceId']
  serialPortStatus = record['serialPortStatus']
  createMessage.info("配置信息已填充到上面配置表单")
}

function handleDelete(record) {
  serialPortVoiceId = record['serialPortVoiceId']
  createConfirm({
    iconType: 'warning',
    content: "确定删除此配置？",
    onOk() {
      deleteRecord({id: serialPortVoiceId}).then(res => {
        reload()
        createMessage.success("删除成功")
      })
    }
  });

}

// 格式化音频显示
const formatVoice = computed(() => {
  return (tempVoiceFileId) => {
    const targetItem = soundPillarVoices.find(item => item['id'] == tempVoiceFileId)
    return targetItem['voiceName'];
  }
})

// 格式化算法显示
const formatAlgorithm = computed(() => {
  return (param) => {
    const targetItem = algorithmArr.find(item => item['value'] == param)
    return targetItem['label']
  }
})

// 保存配置
async function saveData() {
  console.log("保存数据：", formState)
  const params = Object.assign(formState, {serialPortId}, {voiceFileId}, {serialPortVoiceId}, {serialPortStatus})
  console.log(params)
  const response = await saveSettings(params)
  console.log(response)
  await reload()
}

// mqtt 初始化
function handleMsg(msg) {
  console.log('接收到websocket消息：', msg)
  const responseHex = msg['data']
  const serialName = formState.serialName
  const b1 = serialName == msg['serialName']

  const newResponseHex = responseHex.replaceAll(" ", "")
  let newPlayResponseCode = null
  if (playResponseCode) {
    newPlayResponseCode = playResponseCode.replaceAll(" ", "")
    const playResponse = lowerCase(newResponseHex) == lowerCase(newPlayResponseCode)
    if (b1 && playResponse) voiceTestBtnLoad.value = false
  }
  let newCloseResponseCode = null
  if (closeResponseCode) {
    newCloseResponseCode = closeResponseCode.replaceAll(" ", "")
    const closeResponse = lowerCase(newResponseHex) == lowerCase(newCloseResponseCode)
    if (b1 && closeResponse) voiceCloseBtnLoad.value = false
  }
}

function initMqtt() {
  const userStore = useUserStore();
  let userId = unref(userStore.getUserInfo).id;
  const index = window.location.host.indexOf(':');
  const hostIp = window.location.host.slice(0, index);
  let url = `ws://${hostIp}:8088/kyscene` + '/serialWebSocket/' + userId
  connectWebSocket(url)
  onWebSocket((msg) => handleMsg(msg))
}

onMounted(() => {
  getSerialArr();
  getAlgorithmArr();
  getVoices();
  initMqtt();
})

onUnmounted(() => {
  offWebSocket((msg) => {
    console.log(msg)
  })
})

</script>

<style scoped lang="less">
.sound-container {
  height: calc(100vh - 48px);
  width: 100%;
  min-width: 1366px;
  padding: 15px;
}

[data-doc-theme='light'] .ant-table-striped :deep(.table-striped) td {
  background-color: #fafafa;
}

[data-doc-theme='dark'] .ant-table-striped :deep(.table-striped) td {
  background-color: rgb(29, 29, 29);
}
</style>
