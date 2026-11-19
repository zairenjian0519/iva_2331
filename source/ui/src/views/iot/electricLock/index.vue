<template>
  <div class="container-electric-lock">
    <div class="buttons">
      <a-button type="primary" @click="addDevice">新增设备</a-button>
    </div>
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>
  </div>

  <DeviceModal @register="registerModal" @success="handleSuccess"></DeviceModal>
  <RealtimeData @register="registerRealModal"></RealtimeData>
  <HistoryData @register="registerHistoryDrawer"></HistoryData>
</template>

<script setup lang="ts">
import {BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {deleteDevice, getPage} from "./lock.api";
import {columns} from "./lock.data";
import DeviceModal from "./DeviceModal.vue";
import {useModal} from "/@/components/Modal";
import RealtimeData from "./RealtimeData.vue";
import HistoryData from "./HistoryData.vue";
import {useDrawer} from "/@/components/Drawer";

const {tableContext} = useListPage({
  designScope: 'model-file-table',
  tableProps: {
    title: '电磁锁列表',
    bordered: true,
    api: getPage,
    columns: columns,
    striped: true,
    useSearchForm: false,
    tableSetting: {
      fullScreen: false,
      setting: true,
    },
    actionColumn: {
      width: 350,
    },
  },
})
const [registerTable, {reload}] = tableContext;

const getTableAction = (record) => [
  {
    icon: 'ant-design:edit-outlined',
    label: '编辑',
    onClick: handleEdit.bind(null, record),
  },
  {
    icon: 'ant-design:delete-outlined',
    label: '删除',
    color: 'error',
    popConfirm: {
      title: '是否确认删除',
      confirm: handleDelete.bind(null, record),
    },
  }, {
    icon: 'ant-design:eye-outlined',
    label: '实时数据',
    onClick: () => {
      openRealModal(true, {
        title: '实时数据',
        record
      })
    }
  }, {
    icon: 'ant-design:history-outlined',
    label: '历史数据',
    onClick: () => {
      showHistory(record)
    }
  }
];

const [registerModal, { openModal }] = useModal()

function addDevice() {
  openModal(true, {
    title: '新增设备',
    isUpdate: false
  })
}

function handleEdit(record) {
  openModal(true, {
    title: '编辑设备',
    isUpdate: true,
    record
  })
}

async function handleDelete(record) {
  await deleteDevice({ id: record.id })
  await reload()
}

function handleSuccess() {
  reload()
}

const [registerRealModal, { openModal: openRealModal }] = useModal()

const [registerHistoryDrawer, { openDrawer: openHistoryDrawer }] =useDrawer()

const showHistory = (record) => {
  openHistoryDrawer(true, {
    record
  })
}
</script>

<style scoped lang="less">
.container-electric-lock {
  margin: 0 10px;

  .buttons {
    margin: 10px 0;
    background: #fff;
    padding: 10px
  }
}
</style>
