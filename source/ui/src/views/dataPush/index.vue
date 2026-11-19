<template>
  <div style="margin: 10px">
    <a-card>
      <template #title>
        <div class="title">
          <div>数据推送</div>
          <div>
            <a-button type="link" @click="openDrawer">实时推送日志</a-button>
            <a-button type="link" @click="openDrawerHistory">历史推送日志</a-button>
            <a-button type="link" @click="downloadDocument">接口文档下载</a-button>
          </div>
        </div>
      </template>
      <a-form
        :model="formState"
        :label-col="{ span: 9 }"
        :wrapper-col="{ span: 6 }"
        autocomplete="off"
        @finish="onFinish">

        <a-form-item label="告警推送开关" name="enable">
          <div style="display: flex;justify-content: space-between;">
            <a-switch v-model:checked="formState.enable"/>
            <a-tag :color="formState.enable?'success':'default'">
              {{ formState.enable ? '开启' : '关闭' }}
            </a-tag>
          </div>
        </a-form-item>

        <a-form-item label="告警推送方式" name="pushType" :rules="rules.pushType">
          <a-select v-model:value="formState.pushType" placeholder="请选择推送方式">
            <a-select-option value="direct">Direct 裸推</a-select-option>
            <a-select-option value="token">Token 验证</a-select-option>
          </a-select>
        </a-form-item>

        <template v-if="formState.pushType==='token'">
          <a-form-item label="Token请求地址" name="tokenUrl" :rules="rules.tokenUrl">
            <a-input v-model:value="formState.tokenUrl" placeholder="请输入请求token的地址"/>
          </a-form-item>

          <a-form-item label="用户名" name="username" :rules="rules.username">
            <a-input v-model:value="formState.username" placeholder="请输入请求token的用户名"/>
          </a-form-item>

          <a-form-item label="密码" name="password" :rules="rules.password">
            <a-input v-model:value="formState.password" placeholder="请输入请求token的密码"/>
          </a-form-item>

          <a-form-item label="Token携带方式" name="tokenType" :rules="rules.tokenType">
            <a-select v-model:value="formState.tokenType" placeholder="请选择token携带方式">
              <a-select-option value="param">Param 传参</a-select-option>
              <a-select-option value="header">Header 请求头</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item v-if="formState.tokenType==='param'" label="参数名" name="paramName"
                       :rules="rules.paramName">
            <a-input v-model:value="formState.paramName" placeholder="请填写接收token参数的参数名"/>
          </a-form-item>

          <a-form-item v-if="formState.tokenType==='header'" label="Header Key" name="headerKey"
                       :rules="rules.headerKey">
            <a-input v-model:value="formState.headerKey"
                     placeholder="请填写携带token的Header的Key"/>
          </a-form-item>
        </template>


        <a-form-item label="告警推送地址" name="pushUrl" :rules="rules.pushUrl">
          <a-input v-model:value="formState.pushUrl" placeholder="请填写告警推送地址"/>
        </a-form-item>

        <a-form-item label="" :wrapper-col="{ offset: 9, span: 6}">
          <a-button type="primary" style="float: right" html-type="submit" :loading="btnloading">
            保存立即生效
          </a-button>
        </a-form-item>

        <a-form-item label="" :wrapper-col="{ offset: 7}">
          <p style="font-size-adjust: inherit;font-weight: bolder">说明：</p>
          <p> 1. 推送方式分为:
            <code>Direct</code> 直接裸推送，要求服务端接口禁用接口验证；
            <code>Token</code> 接口推送需要要进行Token验证。
          </p>
          <p> 2. Token验证分为两种：
            <code>Param</code> 传参和 <code>Header</code> 请求头， 接口通过这两种方式去接收 <code>token</code>
            参数。
          </p>
          <p> 3. 用户名和密码为获取 <code>token</code> 所需要的用户名密码。</p>
          <p> 4. 获取 <code>token</code> 的接口返回格式严格按照以下格式返回：
            <code>{ code: 200, success: true, data: { token: xxxx, expire: 1800 }, msg: xxxx
              }</code>
          </p>
          <p> 其中 <code>expire</code> 为 <code>token</code> 过期时间，单位秒。</p>
          <p> 5. 数据推送反馈严格按照此格式返回：<code>{code: 200, success: true, data: null, msg:
            xxxx }</code></p>
        </a-form-item>
      </a-form>
    </a-card>

    <a-drawer
      placement="right"
      :closable="false"
      v-model:visible="drawerVisible"
      @afterVisibleChange="initDrawer"
      @close="drawerClose"
      :body-style="{ backgroundColor: '#000000', color: '#ffffff'}"
      width="50%">
      <div ref="logContainer">
        <p v-if="showPlaceholder">暂无日志，开启推送后可查看实时推送日志</p>
        <p class="logItem" v-for="(log, index) in logItems" :key="index">{{ log }}</p>
      </div>
    </a-drawer>

    <a-drawer
      placement="right"
      :closable="false"
      v-model:visible="historyDrawerVisible"
      @afterVisibleChange="initDrawerHistory"
      @close="drawerHistoryClose"
      width="50%">
      <BasicTable @register="registerTable">
        <template #headerTop>
          <div>
            查询时间：
            <a-range-picker allowClear
                            @change="handleTimeChange"
                            format="YYYY-MM-DD"
                            valueFormat="YYYY-MM-DD"/>
            <a-button class="ml-3" type="primary" @click="getHistory">查询</a-button>
          </div>
        </template>
      </BasicTable>
    </a-drawer>

  </div>
</template>

<script lang="ts" setup>

import {onMounted, onUnmounted, reactive, ref, unref} from 'vue';
import {message} from 'ant-design-vue';
import {get, save} from './dataPush.api';
import {connectWebSocket, offWebSocket, onWebSocket} from "/@/hooks/web/useWebSocketnew";
import {useUserStore} from '/@/store/modules/user';
import {BasicColumn, BasicTable} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {getPageData} from "/@/views/dataPush/dataPushHistory.api";

const formState = reactive({
  enable: false,
  pushType: 'direct',
  tokenType: 'header',
  paramName: 'Token',
  headerKey: 'Token',
  pushUrl: null,
  username: null,
  password: null,
  tokenUrl: null
})

const rules = {
  pushType: [{required: true, message: '推送类型必选'}],
  pushUrl: [
    {required: true, message: '告警推送地址必填'},
    {max: 255, message: '长度必须小于255'}],
  tokenType: [{required: true, message: 'token携带方式必选'}],
  tokenUrl: [
    {required: true, message: 'token请求地址必填'},
    {max: 255, message: '长度必须小于255'}],
  username: [
    {required: true, message: 'token请求用户名必填'},
    {max: 64, message: '长度必须小于64'}],
  password: [{required: true, message: 'token请求密码必填'},
    {max: 64, message: '长度必须小于64'}],
  paramName: [
    {required: true, message: '参数必填'},
    {max: 64, message: '长度必须小于64'}],
  headerKey: [
    {required: true, message: 'header key必填'},
    {max: 64, message: '长度必须小于64'}]
}

const btnloading = ref(false);

const onFinish = async (values) => {
  btnloading.value = true
  try {
    const response = await save(values)
    if (response) {
      message.success('保存成功')
      await fetchData()
    }
  } finally {
    btnloading.value = false
  }
}

const getCurrentHostname = () => {
  const hostname = window.location.hostname;
  const port = window.location.port;
  const protocol = window.location.protocol;

  // 拼接IP地址和端口
  return protocol + '//' + hostname + (port ? ':' + port : '');
};

const downloadDocument = () => {
  const currentHost = getCurrentHostname();
  const fileUrl = currentHost + '/AI超脑集成接口说明.pdf';
  window.open(fileUrl)
};

async function fetchData() {
  const response = await get();
  if (response) Object.assign(formState, response)
}

// 推送日志
const drawerVisible = ref(false);
const drawerClose = () => {
  drawerVisible.value = false
}

const logContainer = ref();
const showPlaceholder = ref(false);
const logItems = ref([]);

const openDrawer = () => {
  drawerVisible.value = true
}

const initDrawer = (visible) => {
  if (!visible) {
    showPlaceholder.value = false
    return
  }
  showPlaceholder.value = logItems.value.length === 0;
}

// 历史推送日志
const historyDrawerVisible = ref(false);
const openDrawerHistory = () => {
  historyDrawerVisible.value = true
}

const initDrawerHistory = (visible) => {
  if (!visible) {
    return
  }
  reload()
}

const drawerHistoryClose = () => {
  historyDrawerVisible.value = false
}

// table

const state = reactive({
  startTime: '',
  endTime: '',
})

const columns: BasicColumn[] = [
  {
    title: '时间',
    align: 'center',
    dataIndex: 'createTime',
    width: 200,
  }, {
    title: '信息',
    align: 'center',
    dataIndex: 'message',
  }
]

const {tableContext} = useListPage({
  designScope: 'data-push-table',
  tableProps: {
    api: getPageData,
    columns: columns,
    striped: true,
    searchInfo: {
      startTime: state.startTime,
      endTime: state.endTime,
    },
    useSearchForm: false,
    tableSetting: {
      fullScreen: false,
      size: false,
      redo: false,
      setting: false,
    },
    showActionColumn: false,
    pagination: {
      pageSize: 30,
    },
  },
})

const [registerTable, {reload, setProps}] = tableContext;

const handleTimeChange = (value) => {
  console.log('value', value)
  setProps({
    searchInfo: {
      startTime: '',
      endTime: '',
    },
  })
  if (value) {
    setProps({
      searchInfo: {
        startTime: value[0],
        endTime: value[1],
      },
    })
  }

}

const getHistory = () => {
  reload()
}

const userStore = useUserStore();
const initWebsocket = () => {
  let userId = unref(userStore.getUserInfo).id;
  const index = window.location.host.indexOf(':');
  const hostIp = window.location.host.slice(0, index);
  const urlViWebSocket = `ws://${hostIp}:8088/kyscene` + '/dataPushWebSocket/' + userId
  connectWebSocket(urlViWebSocket);
}

const onWebSocketMessage = (data) => {
  console.log('dataPushWebsocket', data)
  logItems.value.push(data.msg)
  showPlaceholder.value = logItems.value.length === 0;
  if (logItems.value.length > 100) {
    logItems.value.shift()
  }
}

onUnmounted(() => {
  offWebSocket(onWebSocketMessage)
})

onMounted(() => {
  fetchData()

  // 初始化websocket连接
  initWebsocket()
  onWebSocket(onWebSocketMessage);
})

</script>

<style lang="less" scoped>
.title {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .rightContent {
    display: flex;
    align-items: center;
  }
}

.logItem {
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-all;
}
</style>
