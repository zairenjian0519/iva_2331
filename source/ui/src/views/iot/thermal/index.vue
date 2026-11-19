<template>
  <div class="container-table">
    <BasicTable @register="registerTable">
      <template #headerTop>
        <a-button type="primary" @click="addDevice">
          <a-icon type="plus" /> 新增设备
        </a-button>
      </template>
      <template #location="{record}">
       {{ findPathById(record.location, kyCategoryTreeData) }}
      </template>
      <template #meterType="{record}">
        {{ getMeterTypeName(record.meterType) }}
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
import {columns} from "./thsensor.data"
import {onMounted, ref} from "vue";
import {deleteDevice, getMeterTypeList, listPage} from "./thsensor.api";
import {getKyCategoryTreeData} from "/@/views/base/kycategory/KyCategory.api";

import THHistoryData from "./THHistoryData.vue";
import THRealTimeData from "./THRealTimeData.vue"
import SensorDeviceModal from "./SensorDeviceModal.vue";
import {useMessage} from "/@/hooks/web/useMessage";
import {BasicTable, TableAction} from "/@/components/Table";
import {useListPage} from "/@/hooks/system/useListPage";
import {useModal} from "/@/components/Modal";
import {useDrawer} from "/@/components/Drawer";

const {createMessage} = useMessage();
const kyCategoryTreeData = ref([])
const meterTypeList = ref([])

const [registerModal, { openModal }] =useModal()

const { tableContext } = useListPage({
  tableProps: {
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
    isUpdate: false,
    kyCategoryTreeData: kyCategoryTreeData.value,
    meterTypeList: meterTypeList.value
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
        record,
        kyCategoryTreeData: kyCategoryTreeData.value,
        meterTypeList: meterTypeList.value
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

const [registerTHRealDataModal, { openModal: openTHRealDataModal }] = useModal()
const [registerTHHistoryDataModal, { openDrawer: openTHHistoryDataDrawer }] =useDrawer();

// 递归查找节点路径
function findPathById(targetVal, treeData, path = []) {
  for (const node of treeData) {
    const currentPath = [...path, node.title];

    if (node.value == targetVal) {
      return currentPath;
    }

    if (node.children && node.children.length) {
      const foundPath = findPathById(targetVal, node.children, currentPath);
      if (foundPath) {
        return foundPath;
      }
    }
  }
  return null;
}

function getMeterTypeName(val) {
  const res = meterTypeList.value.find(item => item.value == val)
  return res ? res.label : ''
}

onMounted( () => {
  getKyCategoryTreeData({category: 1}).then((response:any)=>{
    if (!response) return
    kyCategoryTreeData.value = response
  })

  getMeterTypeList().then((response:any)=>{
    if (!response) return
    const options = []
    for (let i = 0; i < response.length; i++) {
      options.push({
        label: response[i],
        value: i + 1,
      })
    }
    meterTypeList.value = options
  })
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
