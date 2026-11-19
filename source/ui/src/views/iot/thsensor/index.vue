<template>
  <div class="container-thsensor">
    <BasicForm @register="registerForm">
      <template #formFooter>
        <div class="formButton">
          <a-button type="primary" @click="listSystemSerials" class="mr-2" :loading="serialBtnLoading">刷新串口列表</a-button>
          <a-button type="primary" @click="connect" class="mr-2">连接</a-button>
          <a-button type="error" @click="disConnect" class="mr-2">断开</a-button>
          <a-button type="default" @click="handleReset" >重置</a-button>
        </div>
      </template>
      <template #status="{model, field}">
        <a-tag :color="model[field] == 1 ? 'green' : 'error'">{{ model[field] == 1 ? '已连接' : '未连接' }}</a-tag>
      </template>
    </BasicForm>
  </div>
  <div class="container-table">
    <BasicTable @register="registerTable">
      <template #headerTop>
        <a-button type="primary" @click="addDevice">
          <a-icon type="plus" /> 新增设备
        </a-button>
      </template>
      <template #action="{record}">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>
  </div>
  <sensor-device-modal @register="registerModal" @success="handleSuccess"></sensor-device-modal>
  <THRealTimeData @register = "registerTHRealDataModal"></THRealTimeData>
  <THHistoryData @register="registerTHHistoryDataModal"></THHistoryData>
</template>

<script setup lang="ts">
import {BasicForm, useForm} from "/@/components/Form";
import {columns, formSchemas} from "./thsensor.data"
import {onMounted, ref} from "vue";
import {
  connectCommPort, deleteDevice,
  disconnectCommPort, getCommSetting,
  getSystemCommPorts, listPage
} from "/@/views/iot/thsensor/thsensor.api";

import THHistoryData from "./THHistoryData.vue";
import THRealTimeData from "./THRealTimeData.vue"
import SensorDeviceModal from "./SensorDeviceModal.vue";
import { useMessage } from "/@/hooks/web/useMessage";
import {BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {useModal} from "/@/components/Modal";
import {useDrawer} from "/@/components/Drawer";
const {createMessage} = useMessage();

const [registerModal, { openModal }] =useModal()

const { tableContext } = useListPage({
  tableProps: {
    title: '设备管理',
    columns,
    rowKey: 'id',
    api: listPage,
    striped: true,
    useSearchForm: false,
    tableSetting: {
      fullScreen: false,
      setting: true,
    },
    bordered: true,
    actionColumn: {
      width: 350,
    },
  },
})

const [registerTable, { reload }] = tableContext

const addDevice = () => {
  openModal(true, {
    title: '新增设备',
    isUpdate: false
  })
}

const handleSuccess = () => {
  reload()
}

const getTableAction = (record) => [
  {
    label: '编辑',
    icon: 'ant-design:edit-outlined',
    onClick: () => {
      openModal(true, {
        title: '编辑设备',
        isUpdate: true,
        record
      })
    }
  }, {
    label: '实时数据',
    icon: 'ant-design:eye-outlined',
    onClick: () => {
      openTHRealDataModal(true, {
        title: '实时数据',
        record
      })
    }
  }, {
    label: '历史数据',
    icon: 'ant-design:history-outlined',
    onClick: () => {
      openTHHistoryDataDrawer(true, {
        title: '历史数据',
        record
      })
    }
  }, {
    label: '删除',
    color: 'error',
    icon: 'ant-design:delete-outlined',
    popConfirm: {
      title: '是否确认删除',
      confirm: async () => {
        await deleteDevice({id: record.id})
        await reload()
        createMessage.success("删除成功")
      }
    }
  }
]

const [registerForm, {validate, setFieldsValue, updateSchema}] = useForm({
  schemas: formSchemas,
  showActionButtonGroup: false,
})

async function connect() {
  const values = await validate()
  await connectCommPort(values)
  await initCommSetting()
  createMessage.success("连接成功")
}

async function disConnect() {
  await disconnectCommPort()
  await initCommSetting()
  createMessage.success("断开成功")
}

async function handleReset() {
  await setFieldsValue({
    baudRate: 9600,
    dataBits: 8,
    stopBits: 1,
    parity: 0
  })
}

const serialBtnLoading = ref(false)
async function listSystemSerials() {
  serialBtnLoading.value = true;
  try {
    const serials = await getSystemCommPorts();
    let serialArr = []
    if (serials && serials.length > 0) {
      serialArr.length = 0;
      for (const serialPort of serials) {
        serialArr.push({label: serialPort, value: serialPort})
      }
    }
    await updateSchema({
      field: 'commPortName',
      componentProps: {
        options: serialArr
      }
    })
  }finally {
    serialBtnLoading.value = false;
  }

}

async function initCommSetting() {
  const setting  = await getCommSetting();
  if( setting) {
    await setFieldsValue(setting)
  }
}

const [registerTHRealDataModal, { openModal: openTHRealDataModal }] = useModal()


const [registerTHHistoryDataModal, { openDrawer: openTHHistoryDataDrawer }] =useDrawer();

onMounted(() => {
  listSystemSerials()
  initCommSetting()
})
</script>

<style scoped lang="less">
.container-thsensor {
  margin: 10px;
  padding-top: 20px;
  padding-left: 10px;
  background: #ffffff;

  .formButton {
    width: 100%;
    text-align: right;
    margin-right: 50px;
    margin-bottom: 20px;
  }
}
.container-table {
  margin: 10px;
  background: #ffffff;
}
</style>
