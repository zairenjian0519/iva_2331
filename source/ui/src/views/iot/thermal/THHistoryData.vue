<template>
  <BasicDrawer @register="registerDrawer">
    <BasicTable @register="registerTable" >
      <template #action="{record}">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #ipAddress>
        <div>{{ deviceInfo.ipAddress }}</div>
      </template>
    </BasicTable>
  </BasicDrawer>
</template>

<script setup lang="ts">
import {BasicColumn, BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {BasicDrawer, useDrawerInner} from "/@/components/Drawer";
import {deleteTHHistoryData, getTHHistoryData} from "./thsensor.api";
import {ref} from "vue";

const columns: BasicColumn[] = [
  {
    title: 'IP地址',
    dataIndex: 'ipAddress',
    key: 'ipAddress',
    align: 'center',
    slots: {customRender: 'ipAddress'}
  }, {
    title: '最高温度（℃）',
    dataIndex: 'temperatureMax',
    key: 'temperatureMax',
    align: 'center',
  }, {
    title: '最低温度（℃）',
    dataIndex: 'temperatureMin',
    key: 'temperatureMin',
    align: 'center',
  }, {
    title: '中间值温度（℃）',
    dataIndex: 'temperatureMid',
    key: 'temperatureMid',
    align: 'center',
  }, {
    title: '温度平均值（℃）',
    dataIndex: 'temperatureAver',
    key: 'temperatureAver',
    align: 'center',
  }, {
    title: '温度标准差（℃）',
    dataIndex: 'temperatureStd',
    key: 'temperatureStd',
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

const deviceInfo = ref({})
const [registerDrawer, { setDrawerProps }] = useDrawerInner( (data:any) => {
  if (data.record) deviceInfo.value = data.record
  setDrawerProps({
    title: data.title,
    width: 1250,
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
