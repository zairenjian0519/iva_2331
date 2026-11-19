<template>
  <div>
    <BasicDrawer @register="registerBaseDrawer">
      <BasicTable @register="registerTable">
        <template #action="{ record }">
          <TableAction :actions="getTableAction(record)" />
        </template>
      </BasicTable>
    </BasicDrawer>
  </div>
</template>

<script setup lang="ts">
import {BasicColumn, BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {deleteHistory, getHistoryData} from "/@/views/iot/electricLock/lock.api";
import {BasicDrawer, useDrawerInner} from "/@/components/Drawer";

import {useMessage} from "/@/hooks/web/useMessage";
const { createMessage } = useMessage();

const [registerBaseDrawer, { setDrawerProps }] = useDrawerInner( (data) => {
  console.log("pinNumber", data.record.pinNumber)
  setDrawerProps({
    width: 800,
    destroyOnClose: true,
    showFooter: false,
    closable: true,
  });

  reload({
    searchInfo: {
      pinNumber: data.record.pinNumber,
    }
  })
})

const columns: BasicColumn[] = [
  {
    title: '时间',
    dataIndex: 'createTime',
    key: 'createTime',
  },
  {
    title: 'GPIO编号',
    dataIndex: 'pinNumber',
    key: 'pinNumber',
  },
  {
    title: '值',
    dataIndex: 'realtimeData',
    key: 'realtimeData',
  }
]

const {tableContext} = useListPage({
  tableProps: {
    title: '电磁锁历史变化数据',
    bordered: true,
    api: getHistoryData,
    columns: columns,
    striped: true,
    useSearchForm: false,
    tableSetting: {
      fullScreen: false,
      setting: true,
    },
    actionColumn: {
      width: 150,
    },
  },
})

const [registerTable, {reload}] = tableContext;

const getTableAction = (record) => [
  {
    icon: 'ant-design:delete-outlined',
    label: '删除',
    color: 'error',
    popConfirm: {
      title: '是否确认删除',
      confirm: handleDelete.bind(null, record),
    },
  }
]

async function handleDelete(record) {
  await deleteHistory({ id: record.id })
  await reload()
  createMessage.success('删除成功')
}
</script>

<style scoped lang="less">

</style>
