<template>
  <BasicTable @register="registerTable" >
    <template #headerTop>
      <a-button type="primary" @click="addData">新增音频采集点</a-button>
      <a-button type="error" @click="stopAllAudio" style="margin-left: 10px">全部停止采集</a-button>
    </template>
    <template #action="{ record }">
      <TableAction :actions="getTableAction(record)" />
    </template>
    <template #collectStatus="{ record }">
      <a-tag :color="record.collectStatus == 1? 'green': 'red'">{{ record.collectStatus == 1? '录制中' : '停止' }}</a-tag>
    </template>
    <template #status="{ record }">
      <a-tag :color="record.status == 1? 'green': 'red'">{{ record.status == 1? '在线' : '离线' }}</a-tag>
    </template>
  </BasicTable>
  <dialog-modal @register="register"/>
  <file-list-drawer @register="registerDrawer"/>
</template>

<script setup lang="ts">
import {BasicTable, TableAction} from "/@/components/Table";
import {deleteData, listPage, start, stop, stopAll} from "./AudioPickup.api";
import {columns} from './AudioPickup.data'
import {useListPage} from "/@/hooks/system/useListPage";
import DialogModal from './components/DialogForm.vue'
import {useModal} from "/@/components/Modal";
import FileListDrawer from "./components/FileListDrawer.vue";
import {useDrawer} from "/@/components/Drawer";


const {tableContext} = useListPage({
  tableProps: {
    api: listPage,
    columns: columns as any[],
    striped: true,
    useSearchForm: false,
    tableSetting: {
      // size: false,
      fullScreen: false,
      setting: false,
    },
    actionColumn: {
      width: 400,
    },
  },
})

const [registerTable, {reload}] = tableContext;

const getTableAction = (record) => {
  return [
    {
      label: '编辑',
      onClick: () => {
        openModal(true, {
          record,
          title: '编辑音频采集点',
          isUpdate: true,
          reload,
        })
      },
    },
    {
      label: '启动采集',
      popConfirm: {
        title: '是否确认启动',
        confirm: async () => {
          await start({id: record.id})
          await reload()
        }
      },
    },
    {
      label: '停止采集',
      popConfirm: {
        title: '是否确认停止',
        confirm: async () => {
          await stop({id: record.id, stop: true})
          await reload()
        }
      }
    },
    {
      label: '查看文件',
      onClick: () => {
        openDrawer(true, {record})
      }
    },
    {
      label: '删除',
      color: 'error',
      popConfirm: {
        title: '是否确认删除',
        confirm: async () => {
          await deleteData({id: record.id})
          await reload()
        }
      }
    },
  ]
}

const [register, { openModal}] = useModal();

const addData = () => {
  openModal(true, {title: '新增音频采集点', isUpdate: false, reload})
}

const [registerDrawer, { openDrawer }] = useDrawer();

const stopAllAudio = async () => {
  await stopAll()
  await reload()
}
</script>

<style scoped lang="less">

</style>
