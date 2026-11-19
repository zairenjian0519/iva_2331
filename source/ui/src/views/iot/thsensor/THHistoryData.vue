<template>
  <BasicDrawer @register="registerDrawer">
    <BasicTable @register="registerTable" >
      <template #action="{record}">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>
  </BasicDrawer>
</template>

<script setup lang="ts">
import {BasicColumn, BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {BasicDrawer, useDrawerInner} from "/@/components/Drawer";
import {deleteTHHistoryData, getTHHistoryData} from "/@/views/iot/thsensor/thsensor.api";

const columns: BasicColumn[] = [
  {
    title: '从机地址',
    dataIndex: 'slaveAddress',
    key: 'slaveAddress',
    align: 'center',
  }, {
    title: '温度（℃）',
    dataIndex: 'temperature',
    key: 'temperature',
    align: 'center',
  }, {
    title: '湿度（%RH）',
    dataIndex: 'humidity',
    key: 'humidity',
    align: 'center',
  }, {
    title: '时间',
    dataIndex: 'createTime',
    key: 'createTime',
    align: 'center',
  }
]

const {tableContext} = useListPage( {
  tableProps: {
    columns,
    rowKey: 'id',
    useSearchForm: false,
    bordered: true,
    pagination: {
      pageSize: 30,
    },
    striped: true,
    tableSetting: {
      fullScreen: false,
      setting: true,
    },
    actionColumn: {
      width: 120,
    },
  },
})
const [registerTable, {reload, setProps}] = tableContext

const getTableAction = (record: any) => {
  return [
    {
      label: '删除',
      confirm: true,
      color: 'error',
      popConfirm: {
        title: '是否确认删除',
        confirm: async () => {
          await deleteTHHistoryData({id: record.id});
          await reload();
        },
      },
    },
  ]
}

const [registerDrawer, { setDrawerProps }] = useDrawerInner( (data:any) => {
  setDrawerProps({
    title: data.title,
    width: 800,
    destroyOnClose: true,
  })
  setProps({
    api: getTHHistoryData,
    searchInfo: {
      id: data.record.id,
    }
  })
})
</script>

<style scoped lang="less">

</style>
